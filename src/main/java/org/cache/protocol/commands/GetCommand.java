package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.NOT_FOUND;
import static org.cache.protocol.commands.ResponseConstants.VALUE;

public class GetCommand<K, V> implements CacheCommand<K, V> {

    private final K key;

    public GetCommand(K key) {
        this.key = key;
    }

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        return cache.get(key)
                .map(value -> VALUE.name() + " " + valueCodec.encode(value))
                .orElse(NOT_FOUND.name());
    }
}
