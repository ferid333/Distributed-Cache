package org.cache.protocol.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegerKeyCodecTest {

    private final IntegerKeyCodec codec = new IntegerKeyCodec();

    @Test
    void encodeConvertsIntegerToString() {
        assertEquals("42", codec.encode(42));
    }

    @Test
    void decodeConvertsNumericStringToInteger() {
        assertEquals(42, codec.decode("42"));
    }

    @Test
    void encodeAndDecodeEmptyValuesReturnNull() {
        assertNull(codec.encode(null));
        assertNull(codec.decode(null));
        assertNull(codec.decode(" "));
    }

    @Test
    void decodeThrowsForNonNumericString() {
        var exception = assertThrows(KeyCodecException.class, () -> codec.decode("fruit"));

        assertEquals("key must be an integer: fruit", exception.getMessage());
    }
}
