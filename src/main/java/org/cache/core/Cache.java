package org.cache.core;

import org.cache.core.metrics.Snapshot;

import java.util.Optional;

public interface Cache<K> {

    void put(K key, byte[] value, ValueType type, long ttlMillis);

    Optional<CacheEntry> get(K key);

    void delete(K key);

    int size();

    void clear();

    Snapshot metrics();
}
