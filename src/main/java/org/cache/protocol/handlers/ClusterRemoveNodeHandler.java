package org.cache.protocol.handlers;

import org.cache.cluster.ClusterGossipService;
import org.cache.cluster.ClusterMembership;

import java.util.List;

public class ClusterRemoveNodeHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 2;
    private static final int NODE_ID_INDEX = 1;

    private final ClusterMembership clusterMembership;
    private final ClusterGossipService clusterGossipService;

    public ClusterRemoveNodeHandler(ClusterMembership clusterMembership, ClusterGossipService clusterGossipService) {
        this.clusterMembership = clusterMembership;
        this.clusterGossipService = clusterGossipService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: CLUSTER_REMOVE_NODE id");
        }

        try {
            clusterMembership.removeNode(parts.get(NODE_ID_INDEX));
            clusterGossipService.broadcastTopology();
            return ResponseConstants.OK.name();
        } catch (IllegalArgumentException exception) {
            return TcpResponseSupport.error(exception.getMessage());
        }
    }
}
