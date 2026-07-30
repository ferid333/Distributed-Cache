package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;

public interface CacheCommand<K> {

    String process(Cache<K> cache, ValueCodecRegistry valueCodecs);
}
