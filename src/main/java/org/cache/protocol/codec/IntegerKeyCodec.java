package org.cache.protocol.codec;

public class IntegerKeyCodec implements KeyCodec<Integer> {

    @Override
    public String encode(Integer value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    @Override
    public Integer decode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new KeyCodecException("key must be an integer: " + value, e);
        }
    }
}
