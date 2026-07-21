package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.ERROR;

public class UnknownCommand<K, V> implements CacheCommand<K, V> {

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        return ERROR.name() + " unknown command";
    }
}
