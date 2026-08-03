package org.cache.eviction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MruEvictionPolicyTest {

    private MruEvictionPolicy<String> evictionPolicy;

    @BeforeEach
    void setUp() {
        evictionPolicy = new MruEvictionPolicy<>();
    }

    @Test
    void onKeyAddedMakesNewestKeyTheVictim() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        assertEquals("second", evictionPolicy.selectVictim().orElseThrow());
    }

    @Test
    void onKeyAccessedMakesAccessedKeyTheVictim() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        evictionPolicy.onKeyAccessed("first");

        assertEquals("first", evictionPolicy.selectVictim().orElseThrow());
    }

    @Test
    void onKeyRemovedRemovesKeyFromVictimSelection() {
        evictionPolicy.onKeyAdded("first");
        evictionPolicy.onKeyAdded("second");

        evictionPolicy.onKeyRemoved("second");

        assertEquals("first", evictionPolicy.selectVictim().orElseThrow());
    }
}
