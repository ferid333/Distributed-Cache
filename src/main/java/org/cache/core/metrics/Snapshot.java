package org.cache.core.metrics;

public class Snapshot {
    private final long hits;
    private final long misses;
    private final long evictions;
    private final long expirations;
    private final double hitRate;

    public Snapshot(long hits, long misses, long evictions, long expirations, double hitRate) {
        this.hits = hits;
        this.misses = misses;
        this.evictions = evictions;
        this.expirations = expirations;
        this.hitRate = hitRate;
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public long getEvictions() {
        return evictions;
    }

    public long getExpirations() {
        return expirations;
    }

    public double getHitRate() {
        return hitRate;
    }
}
