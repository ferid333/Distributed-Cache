package org.cache.protocol.codec;

public interface KeyCodec<K> {

    K decode(String value);

    String encode(K value);
}
