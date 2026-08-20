package org.cache.network.http.dto;

public record ClusterNodeRequestDto(
        String id,
        String host,
        int httpPort,
        int tcpPort,
        int clusterPort
) {
}
