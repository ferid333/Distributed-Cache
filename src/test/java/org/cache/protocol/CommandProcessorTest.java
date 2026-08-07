package org.cache.protocol;

import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.IntegerKeyCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.core.CacheService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandProcessorTest {

    @Test
    void processPutStoresStringAndReturnsOk() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);

            String response = processor.process(List.of("PUT", "fruit", "apple"));

            assertEquals("OK", response);
            assertEquals("VALUE apple", processor.process(List.of("GET", "fruit")));
        }
    }

    @Test
    void processGetReturnsNotFoundForMissingKey() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);

            String response = processor.process(List.of("GET", "missing"));

            assertEquals("NOT_FOUND", response);
        }
    }

    @Test
    void processPushAndLrangeReturnsListValues() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);

            assertEquals("OK", processor.process(List.of("PUSH", "fruits", "apple")));
            assertEquals("OK", processor.process(List.of("PUSH", "fruits", "banana")));

            String response = processor.process(List.of("LRANGE", "fruits", "0", "2"));

            assertEquals("LIST apple, banana", response);
        }
    }

    @Test
    void processLrangeWithoutFromUsesZeroAsFromIndex() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);
            processor.process(List.of("PUSH", "fruits", "apple"));
            processor.process(List.of("PUSH", "fruits", "banana"));
            processor.process(List.of("PUSH", "fruits", "orange"));

            String response = processor.process(List.of("LRANGE", "fruits", "2"));

            assertEquals("LIST apple, banana", response);
        }
    }

    @Test
    void processDeleteRemovesKey() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);
            processor.process(List.of("PUT", "fruit", "apple"));

            String response = processor.process(List.of("DELETE", "fruit"));

            assertEquals("OK", response);
            assertEquals("NOT_FOUND", processor.process(List.of("GET", "fruit")));
        }
    }

    @Test
    void processSizeReturnsCacheSize() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);
            processor.process(List.of("PUT", "fruit", "apple"));

            String response = processor.process(List.of("SIZE"));

            assertEquals("SIZE 1", response);
        }
    }

    @Test
    void processClearRemovesAllKeys() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);
            processor.process(List.of("PUT", "fruit", "apple"));

            String response = processor.process(List.of("CLEAR"));

            assertEquals("OK", response);
            assertEquals("SIZE 0", processor.process(List.of("SIZE")));
        }
    }

    @Test
    void processMetricsReturnsSnapshot() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);
            processor.process(List.of("GET", "missing"));

            String response = processor.process(List.of("METRICS"));

            assertEquals("METRICS hits=0 misses=1 evictions=0 expirations=0 hitRate=0.0", response);
        }
    }

    @Test
    void processReturnsInvalidUsageErrors() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);

            assertEquals("ERROR usage: GET key", processor.process(List.of("GET")));
            assertEquals("ERROR ttl must be a number", processor.process(List.of("PUT", "fruit", "apple", "soon")));
            assertEquals("ERROR range indexes must be numbers", processor.process(List.of("LRANGE", "fruits", "start", "2")));
        }
    }

    @Test
    void processReturnsUnknownCommandError() {
        try (var cache = new LocalCache<String>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<String> processor = processor(cache);

            assertEquals("ERROR unknown command", processor.process(List.of("NOPE")));
        }
    }

    @Test
    void processReturnsInvalidKeyErrorForConfiguredIntegerKeys() {
        try (var cache = new LocalCache<Integer>(10, new LruEvictionPolicy<>())) {
            CommandProcessor<Integer> processor = new CommandProcessor<>(
                    new IntegerKeyCodec(),
                    new CacheService<>(cache, valueCodecs())
            );

            assertEquals("ERROR key must be an integer: fruit", processor.process(List.of("PUT", "fruit", "apple")));
            assertEquals("OK", processor.process(List.of("PUT", "42", "apple")));
            assertEquals("VALUE apple", processor.process(List.of("GET", "42")));
        }
    }

    private static CommandProcessor<String> processor(LocalCache<String> cache) {
        return new CommandProcessor<>(new StringKeyCodec(), new CacheService<>(cache, valueCodecs()));
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
