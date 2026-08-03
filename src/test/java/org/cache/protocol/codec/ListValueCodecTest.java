package org.cache.protocol.codec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListValueCodecTest {

    private final ListValueCodec codec = new ListValueCodec();

    @Test
    void encodeAndDecodeRoundTripsList() {
        List<String> value = List.of("apple", "banana");

        List<String> decoded = codec.decode(codec.encode(value));

        assertEquals(value, decoded);
    }

    @Test
    void encodeAndDecodeRoundTripsEmptyList() {
        List<String> value = List.of();

        List<String> decoded = codec.decode(codec.encode(value));

        assertEquals(value, decoded);
    }

    @Test
    void toStringJoinsListValues() {
        byte[] encoded = codec.encode(List.of("apple", "banana"));

        assertEquals("apple, banana", codec.toString(encoded));
    }
}
