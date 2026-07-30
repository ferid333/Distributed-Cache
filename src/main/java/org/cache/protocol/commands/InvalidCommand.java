package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.protocol.codec.ValueCodecRegistry;

import static org.cache.protocol.commands.ResponseConstants.ERROR;

public class InvalidCommand<K> implements CacheCommand<K> {

    private final String message;

    public InvalidCommand(String message) {
        this.message = message;
    }

    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        return ERROR.name() + " " + message;
    }
}
