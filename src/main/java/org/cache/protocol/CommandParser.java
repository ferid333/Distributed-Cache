package org.cache.protocol;

import org.cache.protocol.codec.Codec;
import org.cache.protocol.commands.*;

import java.util.List;

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

        return parse(List.of(rawCommand.trim().split("\\s+")));
    }

    public CacheCommand<K, V> parse(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return new UnknownCommand<>();
        }

        try {
            CommandType type = CommandType.valueOf(parts.get(0).toUpperCase());
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

    private CacheCommand<K, V> parsePut(List<String> parts) {
        if (parts.size() != THREE && parts.size() != FOUR) {
            return new InvalidCommand<>("usage: PUT key value [ttlMillis]");
        }

        try {
            long ttlMillis = parts.size() == FOUR ? Long.parseLong(parts.get(THREE)) : 0;

            return new PutCommand<>(
                    keyCodec.decode(parts.get(ONE)),
                    valueCodec.decode(parts.get(TWO)),
                    ttlMillis
            );
        } catch (NumberFormatException exception) {
            return new InvalidCommand<>("ttl must be a number");
        }
    }

    private CacheCommand<K, V> parseGet(List<String> parts) {
        if (parts.size() != TWO) {
            return new InvalidCommand<>("usage: GET key");
        }

        return new GetCommand<>(keyCodec.decode(parts.get(ONE)));
    }

    private CacheCommand<K, V> parseDelete(List<String> parts) {
        if (parts.size() != TWO) {
            return new InvalidCommand<>("usage: DELETE key");
        }

        return new DeleteCommand<>(keyCodec.decode(parts.get(ONE)));
    }

    private CacheCommand<K, V> parseSize(List<String> parts) {
        if (parts.size() != ONE) {
            return new InvalidCommand<>("usage: SIZE");
        }

        return new SizeCommand<>();
    }

    private CacheCommand<K, V> parseClear(List<String> parts) {
        if (parts.size() != ONE) {
            return new InvalidCommand<>("usage: CLEAR");
        }

        return new ClearCommand<>();
    }

    private CacheCommand<K, V> parseMetrics(List<String> parts) {
        if (parts.size() != ONE) {
            return new InvalidCommand<>("usage: METRICS");
        }

        return new MetricsCommand<>();
    }
}
