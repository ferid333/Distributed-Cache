package org.cache.core;

public class CacheEntry<V> {

    private final V value;
    private final long expiresAt;

    CacheEntry(V value, long ttlMillis) {
        this.value = value;
        this.expiresAt = ttlMillis > 0
                ? System.currentTimeMillis() + ttlMillis
                : Long.MAX_VALUE;
    }

    public V getValue() {
        return value;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
