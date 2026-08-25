package org.cache.protocol.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringKeyCodecTest {

    private final StringKeyCodec codec = new StringKeyCodec();

    @Test
    void encodeReturnsSameValue() {
        assertEquals("fruit", codec.encode("fruit"));
    }

    @Test
    void decodeReturnsSameValue() {
        assertEquals("fruit", codec.decode("fruit"));
    }

    @Test
    void encodeAndDecodeAllowNull() {
        assertNull(codec.encode(null));
        assertNull(codec.decode(null));
    }
}
