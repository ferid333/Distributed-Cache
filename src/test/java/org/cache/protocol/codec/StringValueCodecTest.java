package org.cache.protocol.codec;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringValueCodecTest {

    private final StringValueCodec codec = new StringValueCodec();

    @Test
    void encodeConvertsStringToUtf8Bytes() {
        assertArrayEquals("apple".getBytes(UTF_8), codec.encode("apple"));
    }

    @Test
    void decodeConvertsUtf8BytesToString() {
        assertEquals("apple", codec.decode("apple".getBytes(UTF_8)));
    }

    @Test
    void toStringConvertsUtf8BytesToString() {
        assertEquals("apple", codec.toString("apple".getBytes(UTF_8)));
    }

    @Test
    void encodeAndDecodeThrowForNull() {
        assertThrows(CodecConversionException.class, () -> codec.encode(null));
        assertThrows(CodecConversionException.class, () -> codec.decode(null));
    }
}
