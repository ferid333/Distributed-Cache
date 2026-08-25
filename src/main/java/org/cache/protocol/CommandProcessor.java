package org.cache.protocol;

import org.cache.cluster.ClusterGossipService;
import org.cache.cluster.ClusterMembership;
import org.cache.cluster.ClusterTopologyCodec;
import org.cache.core.CacheOperations;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.handlers.ClearHandler;
import org.cache.protocol.handlers.ClusterAddNodeHandler;
import org.cache.protocol.handlers.ClusterRemoveNodeHandler;
import org.cache.protocol.handlers.CommandHandler;
import org.cache.protocol.handlers.CommandType;
import org.cache.protocol.handlers.DeleteHandler;
import org.cache.protocol.handlers.GetHandler;
import org.cache.protocol.handlers.LrangeHandler;
import org.cache.protocol.handlers.MetricsHandler;
import org.cache.protocol.handlers.PingHandler;
import org.cache.protocol.handlers.PushHandler;
import org.cache.protocol.handlers.PutHandler;
import org.cache.protocol.handlers.SizeHandler;
import org.cache.protocol.handlers.TopologyApplyHandler;
import org.cache.protocol.handlers.TopologyDigestHandler;
import org.cache.protocol.handlers.TopologyGetHandler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.cache.protocol.handlers.ResponseConstants.ERROR;

public class CommandProcessor<K> {

    private final Map<CommandType, CommandHandler> handlers;

    public CommandProcessor(KeyCodec<K> keyCodec, CacheOperations<K> cacheService) {
        this(keyCodec, cacheService, null, null);
    }

    public CommandProcessor(KeyCodec<K> keyCodec, CacheOperations<K> cacheService, ClusterMembership clusterMembership) {
        this(keyCodec, cacheService, clusterMembership, null);
    }

    public CommandProcessor(
            KeyCodec<K> keyCodec,
            CacheOperations<K> cacheService,
            ClusterMembership clusterMembership,
            ClusterGossipService clusterGossipService
    ) {
        this(keyCodec, cacheService, clusterMembership, clusterGossipService, true);
    }

    public CommandProcessor(
            KeyCodec<K> keyCodec,
            CacheOperations<K> cacheService,
            ClusterMembership clusterMembership,
            ClusterGossipService clusterGossipService,
            boolean registerTopologyCommands
    ) {
        this.handlers = new EnumMap<>(CommandType.class);
        handlers.put(CommandType.PUT, new PutHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.GET, new GetHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.DELETE, new DeleteHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.SIZE, new SizeHandler(cacheService));
        handlers.put(CommandType.CLEAR, new ClearHandler(cacheService));
        handlers.put(CommandType.METRICS, new MetricsHandler(cacheService));
        handlers.put(CommandType.PUSH, new PushHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.LRANGE, new LrangeHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.PING, new PingHandler());

        if (clusterMembership != null && registerTopologyCommands) {
            ClusterTopologyCodec topologyCodec = new ClusterTopologyCodec();
            handlers.put(CommandType.TOPOLOGY_DIGEST, new TopologyDigestHandler(clusterMembership));
            handlers.put(CommandType.TOPOLOGY_GET, new TopologyGetHandler(clusterMembership, topologyCodec));
            handlers.put(CommandType.TOPOLOGY_APPLY, new TopologyApplyHandler(clusterMembership, topologyCodec));
        }

        if (clusterMembership != null && clusterGossipService != null) {
            handlers.put(CommandType.CLUSTER_ADD_NODE, new ClusterAddNodeHandler(clusterMembership, clusterGossipService));
            handlers.put(CommandType.CLUSTER_REMOVE_NODE, new ClusterRemoveNodeHandler(clusterMembership, clusterGossipService));
        }
    }

    public String process(List<String> commandParts) {
        if (commandParts == null || commandParts.isEmpty()) {
            return error("unknown command");
        }

        CommandType type;
        try {
            type = CommandType.valueOf(commandParts.getFirst().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return error("unknown command");
        }

        return handlers.getOrDefault(type, ignored -> error("unknown command")).handle(commandParts);
    }

    private String error(String message) {
        return ERROR.name() + " " + message;
    }
}
