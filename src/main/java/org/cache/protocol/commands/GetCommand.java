package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.CacheEntry;
import org.cache.protocol.codec.ValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;

import java.util.Optional;

import static org.cache.protocol.commands.ResponseConstants.NOT_FOUND;
import static org.cache.protocol.commands.ResponseConstants.VALUE;

public class GetCommand<K> implements CacheCommand<K> {

    private final K key;

    public GetCommand(K key) {
        this.key = key;
    }

    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        Optional<CacheEntry> entry = cache.get(key);

        if (entry.isEmpty()) {
            return NOT_FOUND.name();
        }

        CacheEntry cacheEntry = entry.get();
        ValueCodec<?> codec = valueCodecs.get(cacheEntry.getType());
        return VALUE.name() + " " + codec.toString(cacheEntry.getValue());
    }
}
