package org.cache.protocol.codec;

public class StringCodec implements Codec<String> {

    @Override
    public String decode(String value) {
        return value;
    }

    @Override
    public String encode(String value) {
        return value;
    }
}
