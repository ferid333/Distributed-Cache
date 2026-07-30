package org.cache.util;

import java.nio.charset.StandardCharsets;

public final class ConverterUtil {

    public static byte[] toByteArray(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
