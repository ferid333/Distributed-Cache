package org.cache.network.http;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterGossipService;
import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterTopology;
import org.cache.network.http.dto.ClusterNodeRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClusterControllerTest {

    private final CacheNode nodeA = new CacheNode("node-a", "localhost", 8080, 2020, 10001);
    private final CacheNode nodeB = new CacheNode("node-b", "localhost", 8081, 2021, 10002);

    @Test
    void addNodeUpdatesMembership() {
        ClusterMembership membership = new ClusterMembership(new ClusterTopology(0, List.of(nodeA), 1, 128));
        ClusterGossipService gossipService = mock(ClusterGossipService.class);
        ClusterController controller = new ClusterController(membership, gossipService);

        var response = controller.addNode(new ClusterNodeRequestDto("node-b", "localhost", 8081, 2021, 10002));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(membership.findNode("node-b").isPresent());
        verify(gossipService).broadcastTopology();
    }

    @Test
    void removeNodeUpdatesMembership() {
        ClusterMembership membership = new ClusterMembership(new ClusterTopology(0, List.of(nodeA, nodeB), 1, 128));
        ClusterGossipService gossipService = mock(ClusterGossipService.class);
        ClusterController controller = new ClusterController(membership, gossipService);

        var response = controller.removeNode("node-b");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(membership.findNode("node-b").isEmpty());
        verify(gossipService).broadcastTopology();
    }
}
