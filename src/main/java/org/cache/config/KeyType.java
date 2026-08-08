package org.cache.config;

import java.util.Arrays;
import java.util.Locale;

public enum KeyType {
    STRING("string", "String"),
    INTEGER("integer", "int", "Integer", "Int");

    private final String[] values;

    KeyType(String... values) {
        this.values = values;
    }

    public static KeyType from(String value) {
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(type -> type.matches(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported key type: " + value));
    }

    private boolean matches(String target) {
        for (String value : values) {
            if (value.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
