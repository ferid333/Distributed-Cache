package org.cache.protocol.commands;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PushCommandTest {

    @Test
    void processCreatesListAndReturnsOk() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var command = new PushCommand<>("fruits", "apple", ValueType.LIST);

            String response = command.process(cache, valueCodecs());

            assertEquals(ResponseConstants.OK.name(), response);
            assertEquals("LIST apple", new LrangeCommand<String>("fruits", 0, 1).process(cache, valueCodecs()));
        }
    }

    @Test
    void processAppendsToExistingList() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            new PushCommand<String>("fruits", "apple", ValueType.LIST).process(cache, valueCodecs);
            var command = new PushCommand<String>("fruits", "banana", ValueType.LIST);

            String response = command.process(cache, valueCodecs);

            assertEquals(ResponseConstants.OK.name(), response);
            assertEquals("LIST apple, banana", new LrangeCommand<String>("fruits", 0, 2).process(cache, valueCodecs));
        }
    }

    @Test
    void processReturnsErrorWhenKeyContainsString() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            var valueCodecs = valueCodecs();
            cache.put("fruits", new StringValueCodec().encode("apple"), ValueType.STRING, 0);
            var command = new PushCommand<String>("fruits", "banana", ValueType.LIST);

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
