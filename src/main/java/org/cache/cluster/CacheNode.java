package org.cache.cluster;

public record CacheNode(
        String id,
        String host,
        int httpPort,
        int tcpPort,
        int clusterPort,
        NodeStatus status
) {

    public CacheNode(String id, String host, int httpPort, int tcpPort, int clusterPort) {
        this(id, host, httpPort, tcpPort, clusterPort, NodeStatus.HEALTHY);
    }
}
