package org.cache.protocol;

import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.handlers.CommandType;
import org.cache.protocol.handlers.ClearHandler;
import org.cache.protocol.handlers.CommandHandler;
import org.cache.protocol.handlers.DeleteHandler;
import org.cache.protocol.handlers.GetHandler;
import org.cache.protocol.handlers.LrangeHandler;
import org.cache.protocol.handlers.MetricsHandler;
import org.cache.protocol.handlers.PushHandler;
import org.cache.protocol.handlers.PutHandler;
import org.cache.protocol.handlers.SizeHandler;
import org.cache.core.CacheService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.cache.protocol.handlers.ResponseConstants.ERROR;

public class CommandProcessor<K> {

    private final Map<CommandType, CommandHandler> handlers;

    public CommandProcessor(KeyCodec<K> keyCodec, CacheService<K> cacheService) {
        this.handlers = new EnumMap<>(CommandType.class);
        handlers.put(CommandType.PUT, new PutHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.GET, new GetHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.DELETE, new DeleteHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.SIZE, new SizeHandler(cacheService));
        handlers.put(CommandType.CLEAR, new ClearHandler(cacheService));
        handlers.put(CommandType.METRICS, new MetricsHandler(cacheService));
        handlers.put(CommandType.PUSH, new PushHandler<>(keyCodec, cacheService));
        handlers.put(CommandType.LRANGE, new LrangeHandler<>(keyCodec, cacheService));
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
