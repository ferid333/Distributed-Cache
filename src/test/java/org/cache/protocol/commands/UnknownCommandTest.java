package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class UnknownCommandTest {

    @Test
    void processReturnsUnknownCommandError() {
        var command = new UnknownCommand<String>();

        String response = command.process(mock(Cache.class), new ValueCodecRegistry());

        assertEquals("ERROR unknown command", response);
    }
}
