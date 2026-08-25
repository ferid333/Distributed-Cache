package org.cache.config;

public class CacheConfigException extends IllegalArgumentException {

    public CacheConfigException(String message) {
        super(message);
    }

    public CacheConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
