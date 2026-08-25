package org.cache.cluster;

import org.cache.cluster.hashing.ConsistentHashRing;
import org.cache.cluster.hashing.ClusterHash;

import java.util.Comparator;
import java.util.List;

public record ClusterTopology(
        long version,
        List<CacheNode> nodes,
        int replicationFactor,
        int virtualNodeCount,
        ConsistentHashRing hashRing
) {

    public ClusterTopology(long version, List<CacheNode> nodes, int replicationFactor, int virtualNodeCount) {
        this(
                version,
                List.copyOf(nodes),
                replicationFactor,
                virtualNodeCount,
                new ConsistentHashRing(new ClusterInfo(replicationFactor, List.copyOf(nodes)), virtualNodeCount)
        );
    }

    public String fingerprint() {
        String canonicalTopology = replicationFactor
                + "|"
                + virtualNodeCount
                + "|"
                + nodes.stream()
                .sorted(Comparator.comparing(CacheNode::getId))
                .map(this::fingerprintNode)
                .toList();

        return ClusterHash.hashHex(canonicalTopology);
    }

    private String fingerprintNode(CacheNode node) {
        return node.getId()
                + "|"
                + node.getHost()
                + "|"
                + node.getHttpPort()
                + "|"
                + node.getTcpPort()
                + "|"
                + node.getClusterPort();
    }
}
