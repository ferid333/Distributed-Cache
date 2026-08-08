package org.cache.config;

public enum ConfigKey {
    CAPACITY("capacity"),
    DEFAULT_TTL_MILLIS("defaultTtlMillis"),
    KEY_TYPE("key-type"),
    EVICTION_POLICY("eviction-policy"),
    NODE_ID("node.id"),
    NODE_HOST("node.host"),
    NODE_HTTP_PORT("node.http-port"),
    NODE_TCP_PORT("node.tcp-port"),
    NODE_CLUSTER_PORT("node.cluster-port");

    private final String propertyName;

    ConfigKey(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
