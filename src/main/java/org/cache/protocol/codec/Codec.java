package org.cache.protocol.codec;

public interface Codec<T> {

    T decode(String value);

    String encode(T value);
}
