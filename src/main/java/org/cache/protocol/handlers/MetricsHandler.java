package org.cache.protocol.handlers;

import org.cache.core.metrics.Snapshot;
import org.cache.core.CacheOperations;

import java.util.List;

public class MetricsHandler implements CommandHandler {

    private static final int COMMAND_PARTS = 1;

    private final CacheOperations<?> cacheService;

    public MetricsHandler(CacheOperations<?> cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: METRICS");
        }

        Snapshot metrics = cacheService.metrics();
        return ResponseConstants.METRICS.name() + " hits=" + metrics.getHits()
                + " misses=" + metrics.getMisses()
                + " evictions=" + metrics.getEvictions()
                + " expirations=" + metrics.getExpirations()
                + " hitRate=" + metrics.getHitRate();
    }
}
