package org.cache.core;

import org.cache.core.metrics.CacheMetrics;
import org.cache.core.metrics.Snapshot;
import org.cache.eviction.EvictionPolicy;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalCache<K, V> implements Cache<K, V>, AutoCloseable {

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final int capacity;
    private final EvictionPolicy<K> evictionPolicy;
    private final CacheMetrics metrics;
    private final Object evictionLock;
    private final int cleanupBatchSize;
    private final ScheduledExecutorService cleanupScheduler;
    private Iterator<Map.Entry<K, CacheEntry<V>>> cleanupIterator;

    public LocalCache(int capacity, EvictionPolicy<K> evictionPolicy) {
        this(capacity, evictionPolicy, 1_000, 100);
    }

    public LocalCache(
            int capacity,
            EvictionPolicy<K> evictionPolicy,
            long cleanupIntervalMillis,
            int cleanupBatchSize
    ) {

        this.cache = new ConcurrentHashMap<>();
        this.capacity = capacity;
        this.evictionPolicy = evictionPolicy;
        this.metrics = new CacheMetrics();
        this.evictionLock = new Object();
        this.cleanupBatchSize = cleanupBatchSize;
        this.cleanupIterator = cache.entrySet().iterator();
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "cache-expiry-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        cleanupScheduler.scheduleAtFixedRate(
                this::removeExpiredEntries,
                cleanupIntervalMillis,
                cleanupIntervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void put(K key, V value, long ttlMillis) {

        var newEntry = new CacheEntry<>(value, ttlMillis);

        synchronized (evictionLock) {
            cache.put(key, newEntry);
            evictionPolicy.onKeyAdded(key);

            if (cache.size() > capacity) {
                evictionPolicy.selectVictim().ifPresent(victim -> {
                    if (cache.remove(victim) != null) {
                        evictionPolicy.onKeyRemoved(victim);
                        metrics.recordEviction();
                    }
                });
            }
        }
    }

    @Override
    public Optional<V> get(K key) {
        var entry = cache.get(key);

        if (entry == null) {
            metrics.recordMiss();
            return Optional.empty();
        }

        synchronized (evictionLock) {
            var currentEntry = cache.get(key);

            if (currentEntry == null) {
                metrics.recordMiss();
                return Optional.empty();
            }

            if (currentEntry.isExpired()) {
                cache.remove(key);
                evictionPolicy.onKeyRemoved(key);
                metrics.recordMiss();
                metrics.recordExpiration();

                return Optional.empty();
            }

            evictionPolicy.onKeyAccessed(key);
            metrics.recordHit();
            return Optional.ofNullable(currentEntry.getValue());
        }
    }

    @Override
    public void delete(K key) {
        synchronized (evictionLock) {
            cache.remove(key);
            evictionPolicy.onKeyRemoved(key);
        }
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void clear() {
        synchronized (evictionLock) {
            cache.keySet().forEach(evictionPolicy::onKeyRemoved);
            cache.clear();
        }
    }

    public synchronized void removeExpiredEntries() {
        int checkedEntries = 0;

        while (checkedEntries < cleanupBatchSize) {
            if (!cleanupIterator.hasNext()) {
                cleanupIterator = cache.entrySet().iterator();
            }

            if (!cleanupIterator.hasNext()) {
                return;
            }

            var entry = cleanupIterator.next();
            checkedEntries++;

            if (!entry.getValue().isExpired()) {
                continue;
            }

            synchronized (evictionLock) {
                var currentEntry = cache.get(entry.getKey());
                if (currentEntry != null && currentEntry.isExpired()) {
                    cache.remove(entry.getKey());
                    evictionPolicy.onKeyRemoved(entry.getKey());
                    metrics.recordExpiration();
                }
            }
        }
    }

    @Override
    public void close() {
        cleanupScheduler.shutdownNow();
    }

    @Override
    public Snapshot metrics() {
        return metrics.snapshot();
    }
}
