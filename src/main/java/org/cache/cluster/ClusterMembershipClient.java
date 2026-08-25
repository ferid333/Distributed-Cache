package org.cache.cluster;

import org.cache.cluster.routing.ClusterForwardingClient;
import org.cache.cluster.routing.ClusterForwardingException;
import org.cache.protocol.handlers.CommandType;
import org.cache.protocol.handlers.ResponseConstants;

import java.util.List;
import java.util.Optional;

public class ClusterMembershipClient {

    private static final List<String> PING_COMMAND = List.of("PING");
    private static final List<String> PONG_RESPONSE = List.of("PONG");
    private static final int TOPOLOGY_DIGEST_RESPONSE_SIZE = 3;
    private static final int TOPOLOGY_RESPONSE_SIZE = 2;
    private static final int TOPOLOGY_VERSION_INDEX = 1;
    private static final int TOPOLOGY_FINGERPRINT_INDEX = 2;
    private static final int TOPOLOGY_VALUE_INDEX = 1;

    private final ClusterForwardingClient forwardingClient;
    private final ClusterTopologyCodec topologyCodec;

    public ClusterMembershipClient(ClusterForwardingClient forwardingClient) {
        this(forwardingClient, new ClusterTopologyCodec());
    }

    ClusterMembershipClient(ClusterForwardingClient forwardingClient, ClusterTopologyCodec topologyCodec) {
        this.forwardingClient = forwardingClient;
        this.topologyCodec = topologyCodec;
    }

    public boolean ping(CacheNode targetNode) {
        try {
            return PONG_RESPONSE.equals(forwardingClient.forward(targetNode, PING_COMMAND));
        } catch (ClusterForwardingException exception) {
            return false;
        }
    }

    public Optional<TopologyDigest> topologyDigest(CacheNode targetNode) {
        try {
            List<String> response = forwardingClient.forward(targetNode, List.of(CommandType.TOPOLOGY_DIGEST.name()));
            if (response.size() != TOPOLOGY_DIGEST_RESPONSE_SIZE
                    || !CommandType.TOPOLOGY_DIGEST.name().equals(response.getFirst())) {
                return Optional.empty();
            }

            return Optional.of(new TopologyDigest(
                    ClusterNumberParser.parseLong(response.get(TOPOLOGY_VERSION_INDEX), TopologyField.VERSION.getValue()),
                    response.get(TOPOLOGY_FINGERPRINT_INDEX)
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Optional<ClusterTopology> topology(CacheNode targetNode) {
        try {
            List<String> response = forwardingClient.forward(targetNode, List.of(CommandType.TOPOLOGY_GET.name()));
            if (response.size() != TOPOLOGY_RESPONSE_SIZE
                    || !ResponseConstants.TOPOLOGY.name().equals(response.getFirst())) {
                return Optional.empty();
            }

            return Optional.of(topologyCodec.decode(response.get(TOPOLOGY_VALUE_INDEX)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public boolean applyTopology(CacheNode targetNode, ClusterTopology topology) {
        try {
            List<String> response = forwardingClient.forward(
                    targetNode,
                    List.of(CommandType.TOPOLOGY_APPLY.name(), topologyCodec.encode(topology))
            );
            return List.of(ResponseConstants.OK.name()).equals(response);
        } catch (ClusterForwardingException exception) {
            return false;
        }
    }
}
