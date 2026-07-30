package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class DeleteCommand<K> implements CacheCommand<K> {

    private final K key;

    public DeleteCommand(K key) {
        this.key = key;
    }

    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        cache.delete(key);
        return OK.name();
    }
}
