package org.cache.protocol;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.protocol.commands.CacheCommand;

import java.util.List;

public class CommandProcessor<K> {

    private final Cache<K> cache;
    private final CommandParser<K> parser;
    private final ValueCodecRegistry valueCodecs;

    public CommandProcessor(Cache<K> cache, CommandParser<K> parser, ValueCodecRegistry valueCodecs) {
        this.cache = cache;
        this.parser = parser;
        this.valueCodecs = valueCodecs;
    }

    public String process(List<String> commandParts) {
        CacheCommand<K> command = parser.parse(commandParts);
        return command.process(cache, valueCodecs);
    }
}
