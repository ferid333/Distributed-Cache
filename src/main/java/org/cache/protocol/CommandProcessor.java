package org.cache.protocol;

import org.cache.core.Cache;
import org.cache.protocol.codec.Codec;
import org.cache.protocol.commands.CacheCommand;

public class CommandProcessor<K, V> {

    private final Cache<K, V> cache;
    private final CommandParser<K, V> parser;
    private final Codec<V> valueCodec;

    public CommandProcessor(Cache<K, V> cache, CommandParser<K, V> parser, Codec<V> valueCodec) {
        this.cache = cache;
        this.parser = parser;
        this.valueCodec = valueCodec;
    }

    public String process(String rawCommand) {
        CacheCommand<K, V> command = parser.parse(rawCommand);
        return command.process(cache, valueCodec);
    }
}
