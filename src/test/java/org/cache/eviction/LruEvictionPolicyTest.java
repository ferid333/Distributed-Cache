package org.cache.eviction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LruEvictionPolicyTest {

    private LruEvictionPolicy<String> evictionPolicy;

    @BeforeEach
    void setUp() {
        evictionPolicy = new LruEvictionPolicy<>();
    }

    @Test
    void onKeyAddedMakesOldestKeyTheVictim() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        assertEquals("first", evictionPolicy.selectVictim().orElseThrow());
    }

    @Test
    void onKeyAccessedMakesNotAccessedKeyTheVictim() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        evictionPolicy.onKeyAccessed("first");

        assertEquals("second", evictionPolicy.selectVictim().orElseThrow());
    }

    @Test
    void onKeyRemovedRemovesKeyFromVictimSelection() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        evictionPolicy.onKeyRemoved("first");

        assertEquals("second", evictionPolicy.selectVictim().orElseThrow());
    }
}
