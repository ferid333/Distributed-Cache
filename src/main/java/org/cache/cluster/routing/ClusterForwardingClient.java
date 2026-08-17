package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;
import org.cache.network.tcp.connection.RespCommandClient;

import java.io.IOException;
import java.util.List;

public class ClusterForwardingClient {

    private static final List<String> PING_COMMAND = List.of("PING");
    private static final List<String> PONG_RESPONSE = List.of("PONG");

    private final RespCommandClient commandClient;

    public ClusterForwardingClient() {
        this(new RespCommandClient());
    }

    ClusterForwardingClient(RespCommandClient commandClient) {
        this.commandClient = commandClient;
    }

    public List<String> forward(CacheNode targetNode, List<String> commandParts) {
        try {
            return commandClient.send(targetNode.getHost(), targetNode.getClusterPort(), commandParts);
        } catch (IOException exception) {
            throw new ClusterForwardingException("Failed to forward request to node: " + targetNode.getId(), exception);
        }
    }

    public boolean ping(CacheNode targetNode) {
        try {
            return PONG_RESPONSE.equals(commandClient.send(targetNode.getHost(), targetNode.getClusterPort(), PING_COMMAND));
        } catch (IOException exception) {
            return false;
        }
    }
}
