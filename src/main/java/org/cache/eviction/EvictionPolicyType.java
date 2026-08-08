package org.cache.eviction;

import java.util.Locale;

public enum EvictionPolicyType {
    LRU,
    MRU;

    public static EvictionPolicyType from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported eviction policy: " + value, exception);
        }
    }
}
