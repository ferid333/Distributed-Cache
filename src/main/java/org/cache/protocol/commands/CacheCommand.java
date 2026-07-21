package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;

public interface CacheCommand<K, V> {

    String process(Cache<K, V> cache, Codec<V> valueCodec);
}
