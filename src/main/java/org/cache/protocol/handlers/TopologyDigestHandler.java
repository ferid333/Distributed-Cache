package org.cache.protocol.handlers;

import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterTopology;

import java.util.List;

public class TopologyDigestHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 1;

    private final ClusterMembership clusterMembership;

    public TopologyDigestHandler(ClusterMembership clusterMembership) {
        this.clusterMembership = clusterMembership;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: TOPOLOGY_DIGEST");
        }

        ClusterTopology topology = clusterMembership.currentTopology();
        if (topology == null) {
            return TcpResponseSupport.error("cluster is not enabled");
        }

        return "TOPOLOGY_DIGEST " + topology.version() + " " + topology.fingerprint();
    }
}
