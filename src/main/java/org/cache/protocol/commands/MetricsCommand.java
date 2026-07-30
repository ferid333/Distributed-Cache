package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.metrics.Snapshot;
import org.cache.protocol.codec.ValueCodecRegistry;

import static org.cache.protocol.commands.ResponseConstants.METRICS;

public class MetricsCommand<K> implements CacheCommand<K> {

    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        Snapshot metrics = cache.metrics();
        return METRICS.name() + " hits=" + metrics.getHits()
                + " misses=" + metrics.getMisses()
                + " evictions=" + metrics.getEvictions()
                + " expirations=" + metrics.getExpirations()
                + " hitRate=" + metrics.getHitRate();
    }
}
