package org.cache.protocol.commands;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClearCommandTest {


    @Test
    void processClearsCacheAndReturnsOk() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            ValueCodecRegistry codecRegistry = new ValueCodecRegistry()
                    .register(ValueType.STRING, new StringValueCodec());

            cache.put("x", "123".getBytes(StandardCharsets.UTF_8), ValueType.STRING, 10000);

            assertEquals(1, cache.size());

            var clearCommand = new ClearCommand<String>();

            String response = clearCommand.process(cache, codecRegistry);

            assertEquals(ResponseConstants.OK.name(), response);
            assertEquals(0, cache.size());
        }
    }
}
