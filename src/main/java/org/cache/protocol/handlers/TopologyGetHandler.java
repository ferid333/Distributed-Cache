package org.cache.protocol.handlers;

import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterTopology;
import org.cache.cluster.ClusterTopologyCodec;

import java.util.List;

public class TopologyGetHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 1;

    private final ClusterMembership clusterMembership;
    private final ClusterTopologyCodec topologyCodec;

    public TopologyGetHandler(ClusterMembership clusterMembership, ClusterTopologyCodec topologyCodec) {
        this.clusterMembership = clusterMembership;
        this.topologyCodec = topologyCodec;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: TOPOLOGY_GET");
        }

        ClusterTopology topology = clusterMembership.currentTopology();
        if (topology == null) {
            return TcpResponseSupport.error("cluster is not enabled");
        }

        return "TOPOLOGY " + topologyCodec.encode(topology);
    }
}
