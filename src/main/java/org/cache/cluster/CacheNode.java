package org.cache.cluster;

public record CacheNode(
        String id,
        String host,
        int httpPort,
        int tcpPort,
        int clusterPort
) {
}
