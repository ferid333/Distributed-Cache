package org.cache.protocol;

import org.cache.core.ValueType;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.commands.CacheCommand;
import org.cache.protocol.commands.ClearCommand;
import org.cache.protocol.commands.CommandType;
import org.cache.protocol.commands.DeleteCommand;
import org.cache.protocol.commands.GetCommand;
import org.cache.protocol.commands.InvalidCommand;
import org.cache.protocol.commands.LrangeCommand;
import org.cache.protocol.commands.MetricsCommand;
import org.cache.protocol.commands.PushCommand;
import org.cache.protocol.commands.PutCommand;
import org.cache.protocol.commands.SizeCommand;
import org.cache.protocol.commands.UnknownCommand;

import java.util.List;

public class CommandParser<K> {

    private final KeyCodec<K> keyCodec;

    private static final int COMMAND_PARTS = 1;
    private static final int KEY_COMMAND_PARTS = 2;
    private static final int VALUE_COMMAND_PARTS = 3;
    private static final int TTL_COMMAND_PARTS = 4;
    private static final int RANGE_TO_COMMAND_PARTS = 3;
    private static final int RANGE_FROM_TO_COMMAND_PARTS = 4;

    private static final int COMMAND_INDEX = 0;
    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;
    private static final int TTL_INDEX = 3;
    private static final int FROM_INDEX = 2;
    private static final int TO_INDEX = 3;
    private static final int DEFAULT_FROM_INDEX = 0;

    public CommandParser(KeyCodec<K> keyCodec) {
        this.keyCodec = keyCodec;
    }

    public CacheCommand<K> parse(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return new UnknownCommand<>();
        }

        try {
            CommandType type = CommandType.valueOf(parts.get(COMMAND_INDEX).toUpperCase());
            return switch (type) {
                case PUT -> parsePut(parts);
                case GET -> parseGet(parts);
                case DELETE -> parseDelete(parts);
                case SIZE -> parseSize(parts);
                case CLEAR -> parseClear(parts);
                case METRICS -> parseMetrics(parts);
                case PUSH -> parsePush(parts);
                case LRANGE -> parseLrange(parts);
                case UNKNOWN -> new UnknownCommand<>();
            };
        } catch (IllegalArgumentException exception) {
            return new UnknownCommand<>();
        }
    }


    private CacheCommand<K> parsePut(List<String> parts) {
        if (parts.size() != VALUE_COMMAND_PARTS && parts.size() != TTL_COMMAND_PARTS) {
            return new InvalidCommand<>("usage: PUT key value [ttlMillis]");
        }

        try {
            long ttlMillis = parts.size() == TTL_COMMAND_PARTS ? Long.parseLong(parts.get(TTL_INDEX)) : 0;

            return new PutCommand<>(
                    keyCodec.decode(parts.get(KEY_INDEX)),
                    parts.get(VALUE_INDEX),
                    ValueType.STRING,
                    ttlMillis
            );
        } catch (NumberFormatException exception) {
            return new InvalidCommand<>("ttl must be a number");
        }
    }

    private CacheCommand<K> parsePush(List<String> parts) {
        if (parts.size() != VALUE_COMMAND_PARTS) {
            return new InvalidCommand<>("usage: PUSH key value");
        }

        return new PushCommand<>(
                keyCodec.decode(parts.get(KEY_INDEX)),
                parts.get(VALUE_INDEX),
                ValueType.LIST
        );
    }

    private CacheCommand<K> parseGet(List<String> parts) {
        if (parts.size() != KEY_COMMAND_PARTS) {
            return new InvalidCommand<>("usage: GET key");
        }

        return new GetCommand<>(keyCodec.decode(parts.get(KEY_INDEX)));
    }

    private CacheCommand<K> parseLrange(List<String> parts) {
        if (parts.size() != RANGE_TO_COMMAND_PARTS && parts.size() != RANGE_FROM_TO_COMMAND_PARTS) {
            return new InvalidCommand<>("usage: LRANGE key [from] to");
        }

        try {
            int from = parts.size() == RANGE_FROM_TO_COMMAND_PARTS
                    ? Integer.parseInt(parts.get(FROM_INDEX))
                    : DEFAULT_FROM_INDEX;
            int to = parts.size() == RANGE_FROM_TO_COMMAND_PARTS
                    ? Integer.parseInt(parts.get(TO_INDEX))
                    : Integer.parseInt(parts.get(FROM_INDEX));

            return new LrangeCommand<>(
                    keyCodec.decode(parts.get(KEY_INDEX)),
                    from,
                    to
            );
        } catch (NumberFormatException exception) {
            return new InvalidCommand<>("range indexes must be numbers");
        }
    }

    private CacheCommand<K> parseDelete(List<String> parts) {
        if (parts.size() != KEY_COMMAND_PARTS) {
            return new InvalidCommand<>("usage: DELETE key");
        }

        return new DeleteCommand<>(keyCodec.decode(parts.get(KEY_INDEX)));
    }

    private CacheCommand<K> parseSize(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return new InvalidCommand<>("usage: SIZE");
        }

        return new SizeCommand<>();
    }

    private CacheCommand<K> parseClear(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return new InvalidCommand<>("usage: CLEAR");
        }

        return new ClearCommand<>();
    }

    private CacheCommand<K> parseMetrics(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return new InvalidCommand<>("usage: METRICS");
        }

        return new MetricsCommand<>();
    }
}
