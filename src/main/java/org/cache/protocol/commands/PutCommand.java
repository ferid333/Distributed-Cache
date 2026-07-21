package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class PutCommand<K, V> implements CacheCommand<K, V> {

    private final K key;
    private final V value;
    private final long ttlMillis;

    public PutCommand(K key, V value, long ttlMillis) {
        this.key = key;
        this.value = value;
        this.ttlMillis = ttlMillis;
    }

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        cache.put(key, value, ttlMillis);
        return OK.name();
    }

    public V getValue() {
        return value;
    }
}
