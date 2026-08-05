package org.cache.protocol.handlers;

import org.cache.core.CacheService;
import org.cache.core.LocalCache;
import org.cache.core.ValueType;
import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandHandlerTest {

    private LocalCache<String> cache;
    private CacheService<String> cacheService;
    private ClearHandler clearHandler;
    private DeleteHandler<String> deleteHandler;
    private GetHandler<String> getHandler;
    private LrangeHandler<String> lrangeHandler;
    private MetricsHandler metricsHandler;
    private PushHandler<String> pushHandler;
    private PutHandler<String> putHandler;
    private SizeHandler sizeHandler;

    @BeforeEach
    void setUp() {
        cache = new LocalCache<>(10, new LruEvictionPolicy<>());
        cacheService = new CacheService<>(cache, valueCodecs());
        var keyCodec = new StringKeyCodec();
        clearHandler = new ClearHandler(cacheService);
        deleteHandler = new DeleteHandler<>(keyCodec, cacheService);
        getHandler = new GetHandler<>(keyCodec, cacheService);
        lrangeHandler = new LrangeHandler<>(keyCodec, cacheService);
        metricsHandler = new MetricsHandler(cacheService);
        pushHandler = new PushHandler<>(keyCodec, cacheService);
        putHandler = new PutHandler<>(keyCodec, cacheService);
        sizeHandler = new SizeHandler(cacheService);
    }

    @AfterEach
    void tearDown() {
        cache.close();
    }

    @Test
    void putStoresStringWithOptionalTtl() {
        String response = putHandler.handle(List.of("PUT", "fruit", "apple", "1000"));

        assertEquals("OK", response);
        assertEquals("VALUE apple", getHandler.handle(List.of("GET", "fruit")));
    }

    @Test
    void putReturnsErrorsForInvalidUsageAndTtl() {
        assertEquals("ERROR usage: PUT key value [ttlMillis]", putHandler.handle(List.of("PUT", "key")));
        assertEquals("ERROR ttl must be a number", putHandler.handle(List.of("PUT", "key", "value", "later")));
    }

    @Test
    void getReturnsStoredValueMissingKeyAndWrongType() {
        putHandler.handle(List.of("PUT", "fruit", "apple"));
        pushHandler.handle(List.of("PUSH", "items", "one"));

        assertEquals("VALUE apple", getHandler.handle(List.of("GET", "fruit")));
        assertEquals("NOT_FOUND", getHandler.handle(List.of("GET", "missing")));
        assertEquals("ERROR key contains list value", getHandler.handle(List.of("GET", "items")));
    }

    @Test
    void getReturnsUsageError() {
        assertEquals("ERROR usage: GET key", getHandler.handle(List.of("GET")));
    }

    @Test
    void deleteRemovesKeyAndRejectsInvalidUsage() {
        putHandler.handle(List.of("PUT", "fruit", "apple"));

        assertEquals("OK", deleteHandler.handle(List.of("DELETE", "fruit")));
        assertEquals("NOT_FOUND", getHandler.handle(List.of("GET", "fruit")));
        assertEquals("ERROR usage: DELETE key", deleteHandler.handle(List.of("DELETE")));
    }

    @Test
    void clearRemovesAllKeysAndRejectsInvalidUsage() {
        putHandler.handle(List.of("PUT", "fruit", "apple"));

        assertEquals("OK", clearHandler.handle(List.of("CLEAR")));
        assertEquals("SIZE 0", sizeHandler.handle(List.of("SIZE")));
        assertEquals("ERROR usage: CLEAR", clearHandler.handle(List.of("CLEAR", "now")));
    }

    @Test
    void sizeReturnsCacheSizeAndRejectsInvalidUsage() {
        putHandler.handle(List.of("PUT", "fruit", "apple"));

        assertEquals("SIZE 1", sizeHandler.handle(List.of("SIZE")));
        assertEquals("ERROR usage: SIZE", sizeHandler.handle(List.of("SIZE", "extra")));
    }

    @Test
    void pushCreatesListAndRejectsWrongTypeAndInvalidUsage() {
        putHandler.handle(List.of("PUT", "fruit", "apple"));

        assertEquals("OK", pushHandler.handle(List.of("PUSH", "items", "one")));
        assertEquals("ERROR key contains string value", pushHandler.handle(List.of("PUSH", "fruit", "banana")));
        assertEquals("ERROR usage: PUSH key value", pushHandler.handle(List.of("PUSH", "items")));
    }

    @Test
    void lrangeReturnsValuesWithExplicitAndDefaultFromIndexes() {
        pushHandler.handle(List.of("PUSH", "items", "one"));
        pushHandler.handle(List.of("PUSH", "items", "two"));
        pushHandler.handle(List.of("PUSH", "items", "three"));

        assertEquals("LIST two", lrangeHandler.handle(List.of("LRANGE", "items", "1", "2")));
        assertEquals("LIST one, two", lrangeHandler.handle(List.of("LRANGE", "items", "2")));
    }

    @Test
    void lrangeReturnsNotFoundEmptyListAndErrors() {
        pushHandler.handle(List.of("PUSH", "items", "one"));
        putHandler.handle(List.of("PUT", "fruit", "apple"));

        assertEquals("NOT_FOUND", lrangeHandler.handle(List.of("LRANGE", "missing", "0", "1")));
        assertEquals("LIST", lrangeHandler.handle(List.of("LRANGE", "items", "5", "6")));
        assertEquals("ERROR usage: LRANGE key [from] to", lrangeHandler.handle(List.of("LRANGE", "items")));
        assertEquals("ERROR range indexes must be numbers", lrangeHandler.handle(List.of("LRANGE", "items", "start", "2")));
        assertEquals(
                "ERROR invalid range: from must be >= 0 and to must be >= from",
                lrangeHandler.handle(List.of("LRANGE", "items", "2", "1"))
        );
        assertEquals("ERROR key contains string value", lrangeHandler.handle(List.of("LRANGE", "fruit", "0", "1")));
    }

    @Test
    void metricsReturnsSnapshotAndRejectsInvalidUsage() {
        getHandler.handle(List.of("GET", "missing"));

        assertEquals("METRICS hits=0 misses=1 evictions=0 expirations=0 hitRate=0.0", metricsHandler.handle(List.of("METRICS")));
        assertEquals("ERROR usage: METRICS", metricsHandler.handle(List.of("METRICS", "extra")));
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
