package org.cache.core;

import org.cache.core.metrics.Snapshot;

import java.util.List;
import java.util.Optional;

public interface CacheOperations<K> {

    void putString(K key, String value, long ttlMillis);

    Optional<String> getString(K key);

    void push(K key, String value);

    Optional<List<String>> lrange(K key, int from, int to);

    void delete(K key);

    int size();

    void clear();

    Snapshot metrics();
}
