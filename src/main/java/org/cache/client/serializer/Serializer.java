package org.cache.client.serializer;

public interface Serializer<V> {

    String encode(V value);

    V decode(String value);
}
