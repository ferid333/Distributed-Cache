package org.cache.cluster;

public enum TopologyField {
    VERSION("version"),
    REPLICATION_FACTOR("replicationFactor"),
    VIRTUAL_NODE_COUNT("virtualNodeCount"),
    HTTP_PORT("httpPort"),
    TCP_PORT("tcpPort"),
    CLUSTER_PORT("clusterPort");

    private final String value;

    TopologyField(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}
