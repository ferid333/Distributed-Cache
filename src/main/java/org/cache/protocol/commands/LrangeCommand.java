package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.CacheEntry;
import org.cache.core.ValueType;
import org.cache.protocol.codec.ValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;

import java.util.List;
import java.util.Optional;

import static org.cache.protocol.commands.ResponseConstants.*;

public class LrangeCommand<K> implements CacheCommand<K> {

    private final K key;
    private final int from;
    private final int to;

    public LrangeCommand(K key, int from, int to) {
        this.key = key;
        this.from = from;
        this.to = to;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        Optional<CacheEntry> entry = cache.get(key);

        if (from < 0 || to < from) {
            return ERROR.name() + " invalid range: from must be >= 0 and to must be >= from";
        }

        if (entry.isEmpty()) {
            return NOT_FOUND.name();
        }

        CacheEntry cacheEntry = entry.get();
        if (cacheEntry.getType() != ValueType.LIST) {
            return ERROR.name() + " key contains " + cacheEntry.getType().name().toLowerCase() + " value";
        }

        var listCodec = (ValueCodec<List<String>>) valueCodecs.get(ValueType.LIST);
        List<String> list = listCodec.decode(cacheEntry.getValue());

        if (from >= list.size()) {
            return LIST.name();
        }

        int boundedTo = Math.min(to, list.size());
        List<String> values = list.subList(from, boundedTo);

        return LIST.name() + " " + String.join(", ", values);
    }
}
