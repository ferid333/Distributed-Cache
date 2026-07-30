package org.cache.core;

import java.util.Arrays;

public class CacheEntry {

    private final byte[] value;
    private final ValueType type;
    private final long expiresAt;

    CacheEntry(byte[] value, ValueType type, long ttlMillis) {
        this.value = Arrays.copyOf(value, value.length);
        this.type = type;
        this.expiresAt = ttlMillis > 0
                ? System.currentTimeMillis() + ttlMillis
                : Long.MAX_VALUE;
    }

    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    public ValueType getType() {
        return type;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
