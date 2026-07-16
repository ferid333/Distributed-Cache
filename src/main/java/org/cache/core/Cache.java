package org.cache.core;

import java.util.Optional;

public interface Cache<K, V> {

    void put(K key, V value, long ttlMillis);

    Optional<V> get(K key);

    void delete(K key);

    int size();

    void clear();
}
