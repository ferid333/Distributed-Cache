package org.cache.client;

public class CacheClientException extends RuntimeException {

    public CacheClientException(String message) {
        super(message);
    }

    public CacheClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
