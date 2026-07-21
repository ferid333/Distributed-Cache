package org.cache.protocol;

import org.cache.protocol.codec.Codec;
import org.cache.protocol.commands.*;

import java.util.concurrent.TimeUnit;

public class CommandParser<K, V> {

    private final Codec<K> keyCodec;
    private final Codec<V> valueCodec;


    private final static int ONE = 1;
    private final static int TWO = 2;
    private final static int THREE = 3;
    private final static int FOUR = 4;

    public CommandParser(Codec<K> keyCodec, Codec<V> valueCodec) {
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    public CacheCommand<K, V> parse(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return new UnknownCommand<>();
        }

        String[] parts = rawCommand.trim().split("\\s+");

        try {
            CommandType type = CommandType.valueOf(parts[0].toUpperCase());
            return switch (type) {
                case PUT -> parsePut(parts);
                case GET -> parseGet(parts);
                case DELETE -> parseDelete(parts);
                case SIZE -> parseSize(parts);
                case CLEAR -> parseClear(parts);
                case METRICS -> parseMetrics(parts);
                case UNKNOWN -> new UnknownCommand<>();
            };
        } catch (IllegalArgumentException exception) {
            return new UnknownCommand<>();
        }
    }

    private CacheCommand<K, V> parsePut(String[] parts) {
        if (parts.length != THREE && parts.length != FOUR) {
            return new InvalidCommand<>("usage: PUT key value [ttlSeconds]");
        }

        try {
            return new PutCommand<>(
                    keyCodec.decode(parts[ONE]),
                    valueCodec.decode(parts[TWO]),
                    Long.parseLong(parts[THREE])
            );
        } catch (NumberFormatException exception) {
            return new InvalidCommand<>("ttl must be a number");
        }
    }

    private CacheCommand<K, V> parseGet(String[] parts) {
        if (parts.length != TWO) {
            return new InvalidCommand<>("usage: GET key");
        }

        return new GetCommand<>(keyCodec.decode(parts[1]));
    }

    private CacheCommand<K, V> parseDelete(String[] parts) {
        if (parts.length != TWO) {
            return new InvalidCommand<>("usage: DELETE key");
        }

        return new DeleteCommand<>(keyCodec.decode(parts[1]));
    }

    private CacheCommand<K, V> parseSize(String[] parts) {
        if (parts.length != ONE) {
            return new InvalidCommand<>("usage: SIZE");
        }

        return new SizeCommand<>();
    }

    private CacheCommand<K, V> parseClear(String[] parts) {
        if (parts.length != ONE) {
            return new InvalidCommand<>("usage: CLEAR");
        }

        return new ClearCommand<>();
    }

    private CacheCommand<K, V> parseMetrics(String[] parts) {
        if (parts.length != ONE) {
            return new InvalidCommand<>("usage: METRICS");
        }

        return new MetricsCommand<>();
    }
}
