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

class LrangeCommandTest {

    @Test
    void processReturnsListRange() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new ListValueCodec().encode(List.of("apple", "banana", "orange")), ValueType.LIST, 0);
            var command = new LrangeCommand<String>("fruits", 0, 2);

            String response = command.process(cache, valueCodecs);

            assertEquals("LIST apple, banana", response);
        }
    }

    @Test
    void processBoundsToIndexToListSize() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new ListValueCodec().encode(List.of("apple", "banana")), ValueType.LIST, 0);
            var command = new LrangeCommand<String>("fruits", 0, 10);

            String response = command.process(cache, valueCodecs);

            assertEquals("LIST apple, banana", response);
        }
    }

    @Test
    void processReturnsEmptyListWhenFromIsOutOfRange() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new ListValueCodec().encode(List.of("apple")), ValueType.LIST, 0);
            var command = new LrangeCommand<String>("fruits", 2, 3);

            String response = command.process(cache, valueCodecs);

            assertEquals(ResponseConstants.LIST.name(), response);
        }
    }

    @Test
    void processReturnsNotFoundForMissingKey() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var command = new LrangeCommand<String>("missing", 0, 1);

            String response = command.process(cache, valueCodecs());

            assertEquals(ResponseConstants.NOT_FOUND.name(), response);
        }
    }

    @Test
    void processReturnsErrorForInvalidRange() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var command = new LrangeCommand<String>("fruits", 2, 1);

            String response = command.process(cache, valueCodecs());

            assertEquals("ERROR invalid range: from must be >= 0 and to must be >= from", response);
        }
    }

    @Test
    void processReturnsErrorWhenKeyContainsString() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new StringValueCodec().encode("apple"), ValueType.STRING, 0);
            var command = new LrangeCommand<String>("fruits", 0, 1);

            String response = command.process(cache, valueCodecs);

            assertEquals("ERROR key contains string value", response);
        }
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
