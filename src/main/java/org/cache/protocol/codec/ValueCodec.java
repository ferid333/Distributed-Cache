package org.cache.protocol.codec;

public interface ValueCodec<V> {

    byte[] encode(V value);

    V decode(byte[] value);

    String toString(byte[] value);
}
