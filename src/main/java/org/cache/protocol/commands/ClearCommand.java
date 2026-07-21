package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class ClearCommand<K, V> implements CacheCommand<K, V> {

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        cache.clear();
        return OK.name();
    }
}
