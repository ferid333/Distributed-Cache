package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeleteCommandTest {

    @Test
    void processDeletesKeyAndReturnsOk() {
        Cache<String> cache = mock(Cache.class);
        var command = new DeleteCommand<>("fruit");

        String response = command.process(cache, new ValueCodecRegistry());

        verify(cache).delete("fruit");
        assertEquals(ResponseConstants.OK.name(), response);
    }
}
