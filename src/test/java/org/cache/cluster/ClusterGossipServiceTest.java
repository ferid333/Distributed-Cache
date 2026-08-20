package org.cache.cluster;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterGossipServiceTest {

    private final CacheNode nodeA = node("node-a", 8080, 2020, 10001);
    private final CacheNode nodeB = node("node-b", 8081, 2021, 10002);
    private final CacheNode nodeC = node("node-c", 8082, 2022, 10003);
    private final CacheNode nodeD = node("node-d", 8083, 2023, 10004);

    @Test
    void broadcastTopologyPushesCurrentTopologyToPeers() {
        ClusterMembership membership = membership(topology(2, nodeA, nodeB));
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterGossipService gossipService = service(membership, membershipClient);

        gossipService.broadcastTopology();

        verify(membershipClient).applyTopology(nodeB, membership.currentTopology());
    }

    @Test
    void gossipPushesLocalTopologyWhenPeerIsOlder() {
        ClusterMembership membership = membership(topology(2, nodeA, nodeB));
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterGossipService gossipService = service(membership, membershipClient);

        when(membershipClient.topologyDigest(nodeB))
                .thenReturn(Optional.of(new TopologyDigest(1, "old")));

        gossipService.gossipOnce();

        verify(membershipClient).applyTopology(nodeB, membership.currentTopology());
    }

    @Test
    void gossipPullsPeerTopologyWhenPeerIsNewer() {
        ClusterTopology peerTopology = topology(3, nodeA, nodeB, nodeC);
        ClusterMembership membership = membership(topology(2, nodeA, nodeB));
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterGossipService gossipService = service(membership, membershipClient);

        when(membershipClient.topologyDigest(nodeB))
                .thenReturn(Optional.of(new TopologyDigest(3, peerTopology.fingerprint())));
        when(membershipClient.topology(nodeB)).thenReturn(Optional.of(peerTopology));

        gossipService.gossipOnce();

        assertEquals(3, membership.currentTopology().version());
        assertEquals(3, membership.currentTopology().nodes().size());
    }

    @Test
    void gossipPushesLocalTopologyWhenSameVersionConflictKeepsLocalTopology() {
        ClusterTopology localTopology = topology(2, nodeA, nodeB);
        ClusterTopology peerTopology = topology(2, nodeA, nodeD);
        ClusterMembership membership = membership(localTopology);
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterGossipService gossipService = service(membership, membershipClient);

        when(membershipClient.topologyDigest(nodeB))
                .thenReturn(Optional.of(new TopologyDigest(2, peerTopology.fingerprint())));
        when(membershipClient.topology(nodeB)).thenReturn(Optional.of(peerTopology));

        gossipService.gossipOnce();

        verify(membershipClient).applyTopology(nodeB, localTopology);
    }

    private ClusterGossipService service(ClusterMembership membership, ClusterMembershipClient membershipClient) {
        return new ClusterGossipService(
                nodeA,
                membership,
                membershipClient,
                mock(ScheduledExecutorService.class)
        );
    }

    private ClusterMembership membership(ClusterTopology topology) {
        return new ClusterMembership(topology);
    }

    private ClusterTopology topology(long version, CacheNode... nodes) {
        return new ClusterTopology(version, List.of(nodes), 1, 128);
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
