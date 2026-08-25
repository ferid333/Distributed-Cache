package org.cache.cluster.hashing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;
import org.cache.cluster.NodeStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsistentHashRingTest {

    @Test
    void nodeForReturnsSameNodeForSameKey() {
        ConsistentHashRing ring = new ConsistentHashRing(clusterInfo(1), 32);

        CacheNode firstResult = ring.nodeFor("account:42");
        CacheNode secondResult = ring.nodeFor("account:42");

        assertEquals(firstResult, secondResult);
    }

    @Test
    void nodesForReturnsDistinctReplicas() {
        ConsistentHashRing ring = new ConsistentHashRing(clusterInfo(2), 32);

        List<CacheNode> nodes = ring.nodesFor("account:42");

        assertEquals(2, nodes.size());
        assertNotEquals(nodes.get(0).getId(), nodes.get(1).getId());
    }

    @Test
    void nodesForWrapsAroundRing() {
        ConsistentHashRing ring = new ConsistentHashRing(clusterInfo(3), 1);

        List<CacheNode> nodes = ring.nodesFor("account:42");

        assertEquals(3, nodes.size());
    }

    @Test
    void nodesForIgnoresUnavailableNodes() {
        ClusterInfo clusterInfo = clusterInfo(3);
        CacheNode unavailableNode = clusterInfo.nodes().get(1);
        unavailableNode.setStatus(NodeStatus.UNAVAILABLE);
        ConsistentHashRing ring = new ConsistentHashRing(clusterInfo, 32);

        List<CacheNode> nodes = ring.nodesFor("account:42");

        assertEquals(2, nodes.size());
        assertFalse(nodes.stream().anyMatch(node -> node.getId().equals(unavailableNode.getId())));
    }

    @Test
    void constructorRejectsInvalidVirtualNodeCount() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ConsistentHashRing(clusterInfo(1), 0)
        );

        assertEquals("Virtual node count must be at least 1", exception.getMessage());
    }

    private ClusterInfo clusterInfo(int replicationFactor) {
        return new ClusterInfo(replicationFactor, List.of(
                node("node-a", 8080, 2020, 10001),
                node("node-b", 8081, 2021, 10002),
                node("node-c", 8082, 2022, 10003)
        ));
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
