package org.cache.protocol.handlers;

import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterTopology;
import org.cache.cluster.ClusterTopologyCodec;

import java.util.List;

public class TopologyApplyHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 2;
    private static final int TOPOLOGY_INDEX = 1;

    private final ClusterMembership clusterMembership;
    private final ClusterTopologyCodec topologyCodec;

    public TopologyApplyHandler(ClusterMembership clusterMembership, ClusterTopologyCodec topologyCodec) {
        this.clusterMembership = clusterMembership;
        this.topologyCodec = topologyCodec;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: TOPOLOGY_APPLY topology");
        }

        try {
            ClusterTopology topology = topologyCodec.decode(parts.get(TOPOLOGY_INDEX));
            clusterMembership.applyTopology(topology);
            return ResponseConstants.OK.name();
        } catch (IllegalArgumentException exception) {
            return TcpResponseSupport.error(exception.getMessage());
        }
    }
}
