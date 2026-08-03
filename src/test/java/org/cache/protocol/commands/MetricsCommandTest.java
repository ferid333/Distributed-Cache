package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.metrics.Snapshot;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsCommandTest {

    @Test
    void processReturnsMetricsSnapshotValues() {
        Cache<String> cache = mock(Cache.class);
        when(cache.metrics()).thenReturn(new Snapshot(1, 2, 3, 4, 0.5));
        var command = new MetricsCommand<String>();

        String response = command.process(cache, new ValueCodecRegistry());

        assertEquals("METRICS hits=1 misses=2 evictions=3 expirations=4 hitRate=0.5", response);
    }
}
