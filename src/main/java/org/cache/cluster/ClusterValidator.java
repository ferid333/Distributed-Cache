package org.cache.cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClusterValidator {

    private ClusterValidator() {
    }

    public static void validateClusterInfo(int replicationFactor, List<CacheNode> nodes) {
        if (replicationFactor < 1) {
            throw new CacheInfoException("Cluster replication factor must be at least 1");
        }

        if (replicationFactor > nodes.size()) {
            throw new CacheInfoException("Cluster replication factor must not exceed number of active nodes");
        }

        Set<String> nodeIds = new HashSet<>();
        Set<String> hostPorts = new HashSet<>();

        for (CacheNode node : nodes) {
            if (!nodeIds.add(node.getId())) {
                throw new CacheInfoException("Cluster node ids must be unique: " + node.getId());
            }

            addHostPort(hostPorts, node.getHost(), node.getHttpPort());
            addHostPort(hostPorts, node.getHost(), node.getTcpPort());
            addHostPort(hostPorts, node.getHost(), node.getClusterPort());
        }
    }

    private static void addHostPort(Set<String> hostPorts, String host, int port) {
        String hostPort = host + ":" + port;
        if (!hostPorts.add(hostPort)) {
            throw new CacheInfoException("Cluster node host-port combinations must be unique: " + hostPort);
        }
    }
}
