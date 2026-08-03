package org.cache.protocol.commands;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetCommandTest {

    @Test
    void processReturnsValueForStringEntry() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruit", new StringValueCodec().encode("apple"), ValueType.STRING, 0);
            var command = new GetCommand<String>("fruit");

            String response = command.process(cache, valueCodecs);

            assertEquals("VALUE apple", response);
        }
    }

    @Test
    void processReturnsNotFoundForMissingKey() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var command = new GetCommand<String>("missing");

            String response = command.process(cache, valueCodecs());

            assertEquals(ResponseConstants.NOT_FOUND.name(), response);
        }
    }

    @Test
    void processReturnsErrorForListEntry() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruit", new ListValueCodec().encode(List.of("apple")), ValueType.LIST, 0);
            var command = new GetCommand<String>("fruit");

            String response = command.process(cache, valueCodecs);

            assertEquals("ERROR key contains list value", response);
        }
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
