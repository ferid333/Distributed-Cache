package org.cache.cluster.hashing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class ConsistentHashRing {

    private static final int DEFAULT_VIRTUAL_NODE_COUNT = 128;
    private static final String HASH_ALGORITHM = "SHA-256";

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
        return nodesFor(key).getFirst();
    }

    public List<CacheNode> nodesFor(String key) {
        Objects.requireNonNull(key, "Key must not be null");

        long keyHash = hash(key);
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
            ring.put(hash(node.id() + "#" + index), node);
        }
    }

    private void addNodesFromRing(long startHash, List<CacheNode> nodes, Set<String> selectedNodeIds) {
        for (CacheNode node : ring.tailMap(startHash, true).values()) {
            if (selectedNodeIds.add(node.id())) {
                nodes.add(node);
            }

            if (nodes.size() == replicationFactor) {
                return;
            }
        }
    }

    private long hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " hash algorithm is not available", e);
        }
    }
}
