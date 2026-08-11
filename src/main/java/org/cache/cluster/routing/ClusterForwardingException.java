package org.cache.cluster.routing;

public class ClusterForwardingException extends RuntimeException {

    public ClusterForwardingException(String message) {
        super(message);
    }

    public ClusterForwardingException(String message, Throwable cause) {
        super(message, cause);
    }
}
