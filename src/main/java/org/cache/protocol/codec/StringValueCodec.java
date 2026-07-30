package org.cache.protocol.codec;

import java.nio.charset.StandardCharsets;

public class StringValueCodec implements ValueCodec<String> {

    @Override
    public byte[] encode(String value) {
        if (value == null) {
            throw new CodecConversionException("Value must not be null for encoding");
        }

        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String decode(byte[] value) {

        if (value == null) {
            throw new CodecConversionException("Value must not be null for decoding");
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    @Override
    public String toString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
