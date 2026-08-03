package org.cache.protocol;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.protocol.commands.CacheCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandProcessorTest {

    @Test
    void processParsesCommandAndReturnsCommandResponse() {
        Cache<String> cache = mock(Cache.class);
        CommandParser<String> parser = mock(CommandParser.class);
        ValueCodecRegistry valueCodecs = mock(ValueCodecRegistry.class);
        CacheCommand<String> command = mock(CacheCommand.class);
        List<String> commandParts = List.of("GET", "fruit");

        when(parser.parse(commandParts)).thenReturn(command);
        when(command.process(cache, valueCodecs)).thenReturn("VALUE apple");
        var processor = new CommandProcessor<>(cache, parser, valueCodecs);

        String response = processor.process(commandParts);

        verify(parser).parse(commandParts);
        verify(command).process(cache, valueCodecs);
        assertEquals("VALUE apple", response);
    }
}
