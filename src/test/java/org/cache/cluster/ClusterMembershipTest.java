package org.cache.cluster;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterMembershipTest {

    private final CacheNode nodeA = node("node-a", 8080, 2020, 10001);
    private final CacheNode nodeB = node("node-b", 8081, 2021, 10002);
    private final CacheNode nodeC = node("node-c", 8082, 2022, 10003);
    private final CacheNode nodeD = node("node-d", "localhost", 8083, 2019, 10004);
    private final CacheNode nodeE = node("node-e", "localhost", 8084, 2023, 10005);

    @Test
    void addNodeIncrementsVersionAndRebuildsTopology() {
        ClusterMembership membership = membership(nodeA, nodeB);

        membership.addNode(nodeC);

        assertEquals(1, membership.currentTopology().version());
        assertEquals(3, membership.currentTopology().nodes().size());
        assertTrue(membership.findNode("node-c").isPresent());
    }

    @Test
    void removeNodeIncrementsVersionAndRebuildsTopology() {
        ClusterMembership membership = membership(nodeA, nodeB, nodeC);

        membership.removeNode("node-c");

        assertEquals(1, membership.currentTopology().version());
        assertFalse(membership.findNode("node-c").isPresent());
    }

    @Test
    void applyTopologyAcceptsOnlyNewerTopology() {
        ClusterMembership membership = membership(nodeA, nodeB);
        ClusterTopology olderTopology = new ClusterTopology(-1, List.of(nodeA, nodeB, nodeC), 1, 128);
        ClusterTopology newerTopology = new ClusterTopology(2, List.of(nodeA, nodeB, nodeC), 1, 128);

        assertFalse(membership.applyTopology(olderTopology));
        assertEquals(2, membership.currentTopology().nodes().size());

        assertTrue(membership.applyTopology(newerTopology));
        assertEquals(2, membership.currentTopology().version());
        assertEquals(3, membership.currentTopology().nodes().size());
    }

    @Test
    void applyTopologyUsesSmallerTcpAddressesAsSameVersionTieBreaker() {
        ClusterMembership membership = membership(nodeA, nodeB);
        ClusterTopology sameVersionWithSmallerTcpAddress = new ClusterTopology(0, List.of(nodeA, nodeD), 1, 128);

        assertTrue(membership.applyTopology(sameVersionWithSmallerTcpAddress));

        assertTrue(membership.findNode("node-d").isPresent());
        assertFalse(membership.findNode("node-b").isPresent());
    }

    @Test
    void applyTopologyRejectsSameVersionWithLargerTcpAddresses() {
        ClusterMembership membership = membership(nodeA, nodeB);
        ClusterTopology sameVersionWithLargerTcpAddress = new ClusterTopology(0, List.of(nodeA, nodeE), 1, 128);

        assertFalse(membership.applyTopology(sameVersionWithLargerTcpAddress));

        assertTrue(membership.findNode("node-b").isPresent());
        assertFalse(membership.findNode("node-e").isPresent());
    }

    @Test
    void markStatusUpdatesCurrentNodeStatus() {
        ClusterMembership membership = membership(nodeA, nodeB);

        membership.markStatus("node-b", NodeStatus.UNAVAILABLE);

        assertEquals(NodeStatus.UNAVAILABLE, nodeB.getStatus());
    }

    @Test
    void removeNodeRejectsUnknownNode() {
        ClusterMembership membership = membership(nodeA, nodeB);

        var exception = assertThrows(CacheInfoException.class, () -> membership.removeNode("node-c"));

        assertEquals("Cluster node does not exist: node-c", exception.getMessage());
    }

    private ClusterMembership membership(CacheNode... nodes) {
        return new ClusterMembership(new ClusterTopology(0, List.of(nodes), 1, 128));
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }

    private CacheNode node(String id, String host, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, host, httpPort, tcpPort, clusterPort);
    }
}
