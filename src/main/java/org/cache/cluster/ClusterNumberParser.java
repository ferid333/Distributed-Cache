package org.cache.cluster;

public final class ClusterNumberParser {

    private ClusterNumberParser() {
    }

    public static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CacheInfoException("Invalid cluster topology " + name + ": " + value);
        }
    }

    public static long parseLong(String value, String name) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new CacheInfoException("Invalid cluster topology " + name + ": " + value);
        }
    }
}
