package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SizeCommandTest {

    @Test
    void processReturnsCacheSize() {
        Cache<String> cache = mock(Cache.class);
        when(cache.size()).thenReturn(3);
        var command = new SizeCommand<String>();

        String response = command.process(cache, new ValueCodecRegistry());

        assertEquals("SIZE 3", response);
    }
}
