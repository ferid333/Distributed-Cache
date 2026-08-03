package org.cache.client;

import org.cache.core.metrics.Snapshot;

import java.util.List;
import java.util.Optional;

public interface CacheClient<K, V> {

    void put(K key, V value, long ttl);

    void put(K key, V value);

    Optional<V> get(K key);

    void delete(K key);

    Snapshot metrics();

    int size();

    void clear();

    void push(K key, V value);

    List<V> lrange(K key, int to);

    List<V> lrange(K key, int from, int to);
}
