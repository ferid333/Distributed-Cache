package org.cache.cluster.hashing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;
import org.cache.cluster.NodeStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class ConsistentHashRing {

    private static final int DEFAULT_VIRTUAL_NODE_COUNT = 128;
    private final NavigableMap<Long, CacheNode> ring = new TreeMap<>();
    private final int replicationFactor;

    public ConsistentHashRing(ClusterInfo clusterInfo) {
        this(clusterInfo, DEFAULT_VIRTUAL_NODE_COUNT);
    }

    public ConsistentHashRing(ClusterInfo clusterInfo, int virtualNodeCount) {
        Objects.requireNonNull(clusterInfo, "ClusterInfo must not be null");

        if (virtualNodeCount < 1) {
            throw new IllegalArgumentException("Virtual node count must be at least 1");
        }

        this.replicationFactor = clusterInfo.replicationFactor();

        buildRing(clusterInfo.nodes(), virtualNodeCount);
    }

    public CacheNode nodeFor(String key) {
        List<CacheNode> nodes = nodesFor(key);
        if (nodes.isEmpty()) {
            throw new IllegalStateException("No available nodes in hash ring");
        }

        return nodes.getFirst();
    }

    public List<CacheNode> nodesFor(String key) {
        Objects.requireNonNull(key, "Key must not be null");

        long keyHash = ClusterHash.hashLong(key);
        List<CacheNode> nodes = new ArrayList<>(replicationFactor);
        Set<String> selectedNodeIds = new HashSet<>();

        addNodesFromRing(keyHash, nodes, selectedNodeIds);
        addNodesFromRing(ring.firstKey(), nodes, selectedNodeIds);

        return List.copyOf(nodes);
    }

    private void buildRing(List<CacheNode> nodes, int virtualNodeCount) {
        for (CacheNode node : nodes) {
            addNode(node, virtualNodeCount);
        }
    }

    private void addNode(CacheNode node, int virtualNodeCount) {
        for (int index = 0; index < virtualNodeCount; index++) {
            ring.put(ClusterHash.hashLong(node.getId() + "#" + index), node);
        }
    }

    private void addNodesFromRing(long startHash, List<CacheNode> nodes, Set<String> selectedNodeIds) {
        for (CacheNode node : ring.tailMap(startHash, true).values()) {
            if (node.getStatus() != NodeStatus.UNAVAILABLE && selectedNodeIds.add(node.getId())) {
                nodes.add(node);
            }

            if (nodes.size() == replicationFactor) {
                return;
            }
        }
    }

}
