package org.cache.network.http;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterGossipService;
import org.cache.cluster.ClusterMembership;
import org.cache.network.http.dto.ClusterNodeRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster")
public class ClusterController {

    private final ClusterMembership clusterMembership;
    private final ClusterGossipService clusterGossipService;

    public ClusterController(ClusterMembership clusterMembership, ClusterGossipService clusterGossipService) {
        this.clusterMembership = clusterMembership;
        this.clusterGossipService = clusterGossipService;
    }

    @PostMapping("/nodes")
    public ResponseEntity<Void> addNode(@RequestBody ClusterNodeRequestDto request) {
        try {
            clusterMembership.addNode(new CacheNode(
                    request.id(),
                    request.host(),
                    request.httpPort(),
                    request.tcpPort(),
                    request.clusterPort()
            ));
            clusterGossipService.broadcastTopology();
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<Void> removeNode(@PathVariable String nodeId) {
        try {
            clusterMembership.removeNode(nodeId);
            clusterGossipService.broadcastTopology();
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
