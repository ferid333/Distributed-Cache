package org.cache.client;

import org.cache.core.metrics.Snapshot;

import java.util.Optional;

public interface CacheClient<K, V> {

    void put(K key, V value, long ttl);

    void put(K key, V value);

    Optional<V> get(K key);

    void delete(K key);

    Snapshot metrics();

    int size();

    void clear();
}
