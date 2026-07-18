package org.cache.core.metrics;

import java.util.concurrent.atomic.LongAdder;

public class CacheMetrics {

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder evictions = new LongAdder();
    private final LongAdder expirations = new LongAdder();

    public void recordHit() {
        hits.increment();
    }

    public void recordMiss() {
        misses.increment();
    }

    public void recordEviction() {
        evictions.increment();
    }

    public void recordExpiration() {
        expirations.increment();
    }

    public Snapshot snapshot() {
        var hitCount = hits.sum();
        var missCount = misses.sum();
        var requestCount = hitCount + missCount;

        return new Snapshot(
                hitCount,
                missCount,
                evictions.sum(),
                expirations.sum(),
                requestCount == 0 ? 0.0 : (double) hitCount / requestCount
        );
    }
}
