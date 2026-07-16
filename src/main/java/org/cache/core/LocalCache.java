package org.cache.core;

import org.cache.eviction.EvictionPolicy;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalCache<K, V> implements Cache<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final int capacity;
    private final EvictionPolicy<K> evictionPolicy;

    public LocalCache(int capacity, EvictionPolicy<K> evictionPolicy) {
        this.cache = new ConcurrentHashMap<>();
        this.capacity = capacity;
        this.evictionPolicy = evictionPolicy;

    }

    @Override
    public void put(K key, V value, long ttlMillis) {

        var newEntry = new CacheEntry<>(value, ttlMillis);

        cache.put(key, newEntry);

        evictionPolicy.onKeyAdded(key);

        if (cache.size() > capacity) {
            evictionPolicy.selectVictim().ifPresent(victim -> {
                cache.remove(victim);
                evictionPolicy.onKeyRemoved(victim);
            });
        }
    }

    @Override
    public Optional<V> get(K key) {
        var entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }
        evictionPolicy.onKeyAccessed(key);

        return Optional.ofNullable(entry.getValue());
    }

    @Override
    public void delete(K key) {
        cache.remove(key);
        evictionPolicy.onKeyRemoved(key);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
