package org.cache.cluster;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClusterHealthMonitorTest {

    private final CacheNode nodeA = node("node-a", 8080, 2020, 10001);
    private final CacheNode nodeB = node("node-b", 8081, 2021, 10002);
    private final ClusterMembership clusterMembership = membership(nodeA, nodeB);

    @Test
    void checkClusterMovesNodeThroughFailureStatesAfterConsecutiveFailures() {
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterHealthMonitor monitor = monitor(membershipClient);

        when(membershipClient.ping(nodeB)).thenReturn(false);

        monitor.checkCluster();
        assertEquals(NodeStatus.HEALTHY, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.SUSPECTED, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.UNAVAILABLE, nodeB.getStatus());
    }

    @Test
    void checkClusterRestoresHealthyStatusAfterSuccessfulPing() {
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterHealthMonitor monitor = monitor(membershipClient);

        when(membershipClient.ping(nodeB)).thenReturn(false, false, true);

        monitor.checkCluster();
        monitor.checkCluster();
        assertEquals(NodeStatus.SUSPECTED, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.HEALTHY, nodeB.getStatus());
    }

    @Test
    void checkClusterDoesNothingWhenClusterIsDisabled() {
        ClusterMembershipClient membershipClient = mock(ClusterMembershipClient.class);
        ClusterHealthMonitor monitor = new ClusterHealthMonitor(
                nodeA,
                new ClusterMembership(null),
                membershipClient,
                mock(ScheduledExecutorService.class)
        );

        monitor.checkCluster();

        verifyNoInteractions(membershipClient);
    }

    private ClusterHealthMonitor monitor(ClusterMembershipClient membershipClient) {
        return new ClusterHealthMonitor(
                nodeA,
                clusterMembership,
                membershipClient,
                mock(ScheduledExecutorService.class)
        );
    }

    private ClusterMembership membership(CacheNode... nodes) {
        return new ClusterMembership(new ClusterTopology(0, List.of(nodes), 1, 128));
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
