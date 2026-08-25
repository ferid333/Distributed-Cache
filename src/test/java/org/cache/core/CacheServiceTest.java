package org.cache.core;

import org.cache.eviction.LruEvictionPolicy;
import org.cache.protocol.codec.ListValueCodec;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.protocol.handlers.WrongValueTypeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheServiceTest {

    private LocalCache<String> cache;
    private CacheService<String> service;

    @BeforeEach
    void setUp() {
        cache = new LocalCache<>(10, new LruEvictionPolicy<>());
        service = new CacheService<>(cache, valueCodecs());
    }

    @AfterEach
    void tearDown() {
        cache.close();
    }

    @Test
    void putStringStoresEncodedStringWithTtl() {
        service.putString("fruit", "apple", 1_000);

        assertEquals(Optional.of("apple"), service.getString("fruit"));
    }

    @Test
    void getStringReturnsEmptyForMissingOrExpiredEntry() throws Exception {
        service.putString("short", "apple", 1);
        Thread.sleep(10);

        assertEquals(Optional.empty(), service.getString("missing"));
        assertEquals(Optional.empty(), service.getString("short"));
    }

    @Test
    void getStringRejectsListValue() {
        service.push("items", "one");

        WrongValueTypeException exception = assertThrows(
                WrongValueTypeException.class,
                () -> service.getString("items")
        );

        assertEquals(ValueType.STRING, exception.getExpected());
        assertEquals(ValueType.LIST, exception.getActual());
    }

    @Test
    void pushCreatesAndAppendsListValues() {
        service.push("items", "one");
        service.push("items", "two");

        assertEquals(Optional.of(List.of("one", "two")), service.lrange("items", 0, 2));
    }

    @Test
    void pushRejectsStringValue() {
        service.putString("fruit", "apple", 0);

        WrongValueTypeException exception = assertThrows(
                WrongValueTypeException.class,
                () -> service.push("fruit", "banana")
        );

        assertEquals(ValueType.LIST, exception.getExpected());
        assertEquals(ValueType.STRING, exception.getActual());
    }

    @Test
    void lrangeBoundsRangeAndReturnsEmptyListAfterEnd() {
        service.push("items", "one");
        service.push("items", "two");

        assertEquals(Optional.of(List.of("two")), service.lrange("items", 1, 10));
        assertEquals(Optional.of(List.of()), service.lrange("items", 10, 11));
    }

    @Test
    void lrangeReturnsEmptyOptionalForMissingKeyAndRejectsInvalidRange() {
        assertEquals(Optional.empty(), service.lrange("missing", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> service.lrange("missing", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> service.lrange("missing", 2, 1));
    }

    @Test
    void deleteClearSizeAndMetricsDelegateToCache() {
        service.putString("fruit", "apple", 0);
        service.getString("fruit");
        service.getString("missing");

        assertEquals(1, service.size());
        assertEquals(1, service.metrics().getHits());
        assertEquals(1, service.metrics().getMisses());

        service.delete("fruit");
        assertEquals(0, service.size());

        service.putString("fruit", "apple", 0);
        service.clear();
        assertEquals(0, service.size());
    }

    private static ValueCodecRegistry valueCodecs() {
        return new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec())
                .register(ValueType.LIST, new ListValueCodec());
    }
}
