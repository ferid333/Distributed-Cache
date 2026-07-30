package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.CacheEntry;
import org.cache.core.ValueType;
import org.cache.protocol.codec.ValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.cache.protocol.commands.ResponseConstants.ERROR;
import static org.cache.protocol.commands.ResponseConstants.OK;

public class PushCommand<K> implements CacheCommand<K> {
    private final K key;
    private final String value;
    private final ValueType type;

    public PushCommand(K key, String value, ValueType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        Optional<CacheEntry> existingList = cache.get(key);
        ValueCodec<List<String>> listCodec = (ValueCodec<List<String>>) valueCodecs.get(ValueType.LIST);

        List<String> list;
        byte[] finalValue;

        if (existingList.isEmpty() || existingList.get().getValue() == null) {
            list = new ArrayList<>();
        } else {
            CacheEntry entry = existingList.get();
            if (entry.getType() != ValueType.LIST) {
                return ERROR.name() + " key contains " + entry.getType().name().toLowerCase() + " value";
            }

            list = listCodec.decode(entry.getValue());
        }

        list.add(value);
        finalValue = listCodec.encode(list);

        cache.put(key, finalValue, type, 0);
        return OK.name();
    }
}
