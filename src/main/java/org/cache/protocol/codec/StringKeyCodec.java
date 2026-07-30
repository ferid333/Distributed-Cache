package org.cache.protocol.codec;

public class StringKeyCodec implements KeyCodec<String> {

    @Override
    public String decode(String value) {
        return value;
    }

    @Override
    public String encode(String value) {
        return value;
    }
}
