package org.cache.core;

import org.cache.eviction.LruEvictionPolicy;
import org.cache.core.metrics.Snapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalCacheTest {

    private static final int DEFAULT_CAPACITY = 10;

    private LocalCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = createCache(DEFAULT_CAPACITY);
    }

    @AfterEach
    void tearDown() {
        cache.close();
    }

    @Test
    void putStoresValueAndType() {
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        cache.put("fruit", value, ValueType.STRING, 0);

        Optional<CacheEntry> entry = cache.get("fruit");
        assertTrue(entry.isPresent());
        assertArrayEquals(value, entry.get().getValue());
        assertEquals(ValueType.STRING, entry.get().getType());
    }

    @Test
    void getReturnsStoredEntry() {
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        cache.put("fruit", value, ValueType.STRING, 0);

        Optional<CacheEntry> entry = cache.get("fruit");
        assertTrue(entry.isPresent());
        assertArrayEquals(value, entry.get().getValue());
        assertEquals(ValueType.STRING, entry.get().getType());
    }

    @Test
    void getReturnsEmptyWhenKeyDoesNotExist() {
        Optional<CacheEntry> entry = cache.get("missing");

        assertFalse(entry.isPresent());
    }

    @Test
    void deleteEntryAndReturnsEmpty() {
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        cache.put("fruit", value, ValueType.STRING, 0);

        Optional<CacheEntry> entry = cache.get("fruit");
        assertTrue(entry.isPresent());
        assertArrayEquals(value, entry.get().getValue());
        assertEquals(ValueType.STRING, entry.get().getType());

        cache.delete("fruit");

        entry = cache.get("fruit");
        assertFalse(entry.isPresent());
    }

    @Test
    void sizeReturnsNumberOfEntry() {
        int givenSize = 3;
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < givenSize; i++) {
            cache.put("fruit" + i, value, ValueType.STRING, 0);
        }

        int cacheSize = cache.size();

        assertEquals(givenSize, cacheSize);
    }

    @Test
    void clearReturnsEmptyCache() {
        int givenSize = 3;
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < givenSize; i++) {
            cache.put("fruit" + i, value, ValueType.STRING, 0);
        }

        int cacheSize = cache.size();

        assertEquals(givenSize, cacheSize);

        cache.clear();

        int clearedSize = cache.size();
        assertEquals(0, clearedSize);
    }

    @Test
    void metricsReturnsSnapshot() {
        cache.close();
        cache = createCache(1);
        byte[] value = "apple".getBytes(StandardCharsets.UTF_8);

        cache.put("fruit", value, ValueType.STRING, 0);
        cache.get("fruit");
        cache.get("missing");

        cache.put("color", value, ValueType.STRING, 0);

        Snapshot metrics = cache.metrics();

        assertEquals(1, metrics.getHits());
        assertEquals(1, metrics.getMisses());
        assertEquals(1, metrics.getEvictions());
        assertEquals(0, metrics.getExpirations());
        assertEquals(0.5, metrics.getHitRate());
    }

    private LocalCache<String> createCache(int capacity) {
        return new LocalCache<>(capacity, new LruEvictionPolicy<>());
    }
}
