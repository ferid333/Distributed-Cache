package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class ClearCommand<K> implements CacheCommand<K> {

    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        cache.clear();
        return OK.name();
    }
}
