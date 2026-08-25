package org.cache.protocol.handlers;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterGossipService;
import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterNumberParser;

import java.util.List;

public class ClusterAddNodeHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 6;
    private static final int NODE_ID_INDEX = 1;
    private static final int NODE_HOST_INDEX = 2;
    private static final int NODE_HTTP_PORT_INDEX = 3;
    private static final int NODE_TCP_PORT_INDEX = 4;
    private static final int NODE_CLUSTER_PORT_INDEX = 5;

    private final ClusterMembership clusterMembership;
    private final ClusterGossipService clusterGossipService;

    public ClusterAddNodeHandler(ClusterMembership clusterMembership, ClusterGossipService clusterGossipService) {
        this.clusterMembership = clusterMembership;
        this.clusterGossipService = clusterGossipService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: CLUSTER_ADD_NODE id host httpPort tcpPort clusterPort");
        }

        try {
            clusterMembership.addNode(new CacheNode(
                    parts.get(NODE_ID_INDEX),
                    parts.get(NODE_HOST_INDEX),
                    ClusterNumberParser.parseInt(parts.get(NODE_HTTP_PORT_INDEX), "httpPort"),
                    ClusterNumberParser.parseInt(parts.get(NODE_TCP_PORT_INDEX), "tcpPort"),
                    ClusterNumberParser.parseInt(parts.get(NODE_CLUSTER_PORT_INDEX), "clusterPort")
            ));
            clusterGossipService.broadcastTopology();
            return ResponseConstants.OK.name();
        } catch (IllegalArgumentException exception) {
            return TcpResponseSupport.error(exception.getMessage());
        }
    }
}
