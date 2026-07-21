package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.metrics.Snapshot;
import org.cache.protocol.codec.Codec;

import static org.cache.protocol.commands.ResponseConstants.METRICS;

public class MetricsCommand<K, V> implements CacheCommand<K, V> {

    @Override
    public String process(Cache<K, V> cache, Codec<V> valueCodec) {
        Snapshot metrics = cache.metrics();
        return METRICS.name() + " hits=" + metrics.getHits()
                + " misses=" + metrics.getMisses()
                + " evictions=" + metrics.getEvictions()
                + " expirations=" + metrics.getExpirations()
                + " hitRate=" + metrics.getHitRate();
    }
}
