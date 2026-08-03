package org.cache.protocol;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.protocol.commands.CacheCommand;
import org.cache.protocol.commands.ClearCommand;
import org.cache.protocol.commands.DeleteCommand;
import org.cache.protocol.commands.GetCommand;
import org.cache.protocol.commands.InvalidCommand;
import org.cache.protocol.commands.LrangeCommand;
import org.cache.protocol.commands.MetricsCommand;
import org.cache.protocol.commands.PushCommand;
import org.cache.protocol.commands.PutCommand;
import org.cache.protocol.commands.SizeCommand;
import org.cache.protocol.commands.UnknownCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CommandParserTest {

    private final CommandParser<String> parser = new CommandParser<>(new StringKeyCodec());

    @Test
    void parseReturnsPutCommand() {
        CacheCommand<String> command = parser.parse(List.of("PUT", "fruit", "apple"));

        assertInstanceOf(PutCommand.class, command);
    }

    @Test
    void parseReturnsGetCommand() {
        CacheCommand<String> command = parser.parse(List.of("GET", "fruit"));

        assertInstanceOf(GetCommand.class, command);
    }

    @Test
    void parseReturnsDeleteCommand() {
        CacheCommand<String> command = parser.parse(List.of("DELETE", "fruit"));

        assertInstanceOf(DeleteCommand.class, command);
    }

    @Test
    void parseReturnsPushCommand() {
        CacheCommand<String> command = parser.parse(List.of("PUSH", "fruits", "apple"));

        assertInstanceOf(PushCommand.class, command);
    }

    @Test
    void parseReturnsLrangeCommandWithFromAndTo() {
        CacheCommand<String> command = parser.parse(List.of("LRANGE", "fruits", "1", "3"));

        assertInstanceOf(LrangeCommand.class, command);
    }

    @Test
    void parseLrangeWithoutFromUsesZeroAsFromIndex() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new ListValueCodec().encode(List.of("apple", "banana", "orange")), ValueType.LIST, 0);
            CacheCommand<String> command = parser.parse(List.of("LRANGE", "fruits", "2"));

            String response = command.process(cache, valueCodecs);

            assertEquals("LIST apple, banana", response);
        }
    }

    @Test
    void parseReturnsSizeCommand() {
        CacheCommand<String> command = parser.parse(List.of("SIZE"));

        assertInstanceOf(SizeCommand.class, command);
    }

    @Test
    void parseReturnsClearCommand() {
        CacheCommand<String> command = parser.parse(List.of("CLEAR"));

        assertInstanceOf(ClearCommand.class, command);
    }

    @Test
    void parseReturnsMetricsCommand() {
        CacheCommand<String> command = parser.parse(List.of("METRICS"));

        assertInstanceOf(MetricsCommand.class, command);
    }

    @Test
    void parseReturnsUnknownCommandForUnknownType() {
        CacheCommand<String> command = parser.parse(List.of("NOPE"));

        assertInstanceOf(UnknownCommand.class, command);
    }

    @Test
    void parseReturnsInvalidCommandForWrongArgumentCount() {
        CacheCommand<String> command = parser.parse(List.of("GET"));

        assertInstanceOf(InvalidCommand.class, command);
    }

    @Test
    void parseReturnsInvalidCommandForInvalidPutTtl() {
        CacheCommand<String> command = parser.parse(List.of("PUT", "fruit", "apple", "soon"));

        assertInstanceOf(InvalidCommand.class, command);
    }

    @Test
    void parseReturnsInvalidCommandForInvalidLrangeIndex() {
        CacheCommand<String> command = parser.parse(List.of("LRANGE", "fruits", "start", "2"));

        assertInstanceOf(InvalidCommand.class, command);
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
