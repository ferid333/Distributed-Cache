package org.cache.cluster;

import org.cache.cluster.hashing.ConsistentHashRing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ClusterMembership {

    private volatile ClusterTopology clusterTopology;

    public ClusterMembership(ClusterTopology clusterTopology) {
        this.clusterTopology = clusterTopology;
    }

    public ClusterTopology currentTopology() {
        return clusterTopology;
    }

    public ConsistentHashRing getHashRing() {
        if (clusterTopology == null) {
            return null;
        }

        return clusterTopology.hashRing();
    }

    public synchronized void addNode(CacheNode node) {
        if (clusterTopology == null) {
            throw new CacheInfoException("Cluster is not enabled");
        }

        List<CacheNode> nodes = new ArrayList<>(clusterTopology.nodes());
        nodes.add(node);
        ClusterValidator.validateClusterInfo(clusterTopology.replicationFactor(), nodes);

        clusterTopology = nextTopology(nodes);
    }

    public synchronized void removeNode(String nodeId) {
        if (clusterTopology == null) {
            throw new CacheInfoException("Cluster is not enabled");
        }

        if (findNode(nodeId).isEmpty()) {
            throw new CacheInfoException("Cluster node does not exist: " + nodeId);
        }

        List<CacheNode> nodes = clusterTopology.nodes()
                .stream()
                .filter(node -> !node.getId().equals(nodeId))
                .toList();
        ClusterValidator.validateClusterInfo(clusterTopology.replicationFactor(), nodes);

        clusterTopology = nextTopology(nodes);
    }

    public synchronized boolean applyTopology(ClusterTopology incoming) {
        if (clusterTopology != null) {
            if (incoming.version() < clusterTopology.version()) {
                return false;
            }

            if (incoming.version() == clusterTopology.version()
                    && incoming.fingerprint().equals(clusterTopology.fingerprint())) {
                return false;
            }

            if (incoming.version() == clusterTopology.version() && !hasSmallerTcpAddresses(incoming)) {
                return false;
            }
        }

        ClusterValidator.validateClusterInfo(
                incoming.replicationFactor(),
                incoming.nodes()
        );

        clusterTopology = new ClusterTopology(
                incoming.version(),
                incoming.nodes(),
                incoming.replicationFactor(),
                incoming.virtualNodeCount()
        );

        return true;
    }

    public void markStatus(String nodeId, NodeStatus status) {
        findNode(nodeId).ifPresent(node -> node.setStatus(status));
    }

    public Optional<CacheNode> findNode(String nodeId) {
        if (clusterTopology == null) {
            return Optional.empty();
        }

        return clusterTopology.nodes()
                .stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst();
    }

    private ClusterTopology nextTopology(List<CacheNode> nodes) {
        return new ClusterTopology(
                clusterTopology.version() + 1,
                nodes,
                clusterTopology.replicationFactor(),
                clusterTopology.virtualNodeCount()
        );
    }

    private boolean hasSmallerTcpAddresses(ClusterTopology incoming) {
        return tcpAddresses(incoming).compareTo(tcpAddresses(clusterTopology)) < 0;
    }

    private String tcpAddresses(ClusterTopology topology) {
        return topology.nodes()
                .stream()
                .map(node -> node.getHost() + ":" + node.getTcpPort())
                .sorted(Comparator.naturalOrder())
                .toList()
                .toString();
    }
}
