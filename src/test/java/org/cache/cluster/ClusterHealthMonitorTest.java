package org.cache.cluster;

import org.cache.cluster.routing.ClusterForwardingClient;
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
    private final ClusterInfo clusterInfo = new ClusterInfo(1, List.of(nodeA, nodeB));

    @Test
    void checkClusterMovesNodeThroughFailureStatesAfterConsecutiveFailures() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterHealthMonitor monitor = monitor(forwardingClient);

        when(forwardingClient.ping(nodeB)).thenReturn(false);

        monitor.checkCluster();
        assertEquals(NodeStatus.HEALTHY, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.SUSPECTED, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.UNAVAILABLE, nodeB.getStatus());
    }

    @Test
    void checkClusterRestoresHealthyStatusAfterSuccessfulPing() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterHealthMonitor monitor = monitor(forwardingClient);

        when(forwardingClient.ping(nodeB)).thenReturn(false, false, true);

        monitor.checkCluster();
        monitor.checkCluster();
        assertEquals(NodeStatus.SUSPECTED, nodeB.getStatus());

        monitor.checkCluster();
        assertEquals(NodeStatus.HEALTHY, nodeB.getStatus());
    }

    @Test
    void checkClusterDoesNothingWhenClusterIsDisabled() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterHealthMonitor monitor = new ClusterHealthMonitor(
                nodeA,
                null,
                forwardingClient,
                mock(ScheduledExecutorService.class)
        );

        monitor.checkCluster();

        verifyNoInteractions(forwardingClient);
    }

    private ClusterHealthMonitor monitor(ClusterForwardingClient forwardingClient) {
        return new ClusterHealthMonitor(
                nodeA,
                clusterInfo,
                forwardingClient,
                mock(ScheduledExecutorService.class)
        );
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
