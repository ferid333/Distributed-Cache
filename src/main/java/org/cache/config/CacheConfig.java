package org.cache.config;

import org.cache.cluster.CacheNode;
import org.cache.eviction.EvictionPolicy;
import org.cache.protocol.codec.KeyCodec;

public record CacheConfig(
        int capacity,
        long defaultTtlMillis,
        KeyCodec<?> keyCodec,
        EvictionPolicy<?> evictionPolicy,
        CacheNode cacheNode
) {
}
