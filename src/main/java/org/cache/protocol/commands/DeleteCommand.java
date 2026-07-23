package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class DeleteCommand<K, V> implements CacheCommand<K, V> {

    private final K key;

    public DeleteCommand(K key) {
        this.key = key;
    }

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        cache.delete(key);
        return OK.name();
    }
}
