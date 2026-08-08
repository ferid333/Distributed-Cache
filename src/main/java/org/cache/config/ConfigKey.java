package org.cache.config;

public enum ConfigKey {
    CAPACITY("capacity"),
    DEFAULT_TTL_MILLIS("defaultTtlMillis"),
    KEY_TYPE("key-type"),
    EVICTION_POLICY("eviction-policy"),
    NODE("node"),
    CLUSTER("cluster"),
    NODES("nodes"),
    ID("id"),
    HOST("host"),
    HTTP_PORT("http-port"),
    TCP_PORT("tcp-port"),
    CLUSTER_PORT("cluster-port"),
    REPLICATION_FACTOR("replication-factor");

    private final String propertyName;

    ConfigKey(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public static String merge(ConfigKey first, ConfigKey... others) {
        StringBuilder propertyName = new StringBuilder(first.getPropertyName());

        for (ConfigKey key : others) {
            propertyName.append('.').append(key.getPropertyName());
        }

        return propertyName.toString();
    }
}
