package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.ValueType;
import org.cache.protocol.codec.ValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;

import static org.cache.protocol.commands.ResponseConstants.OK;

public class PutCommand<K> implements CacheCommand<K> {

    private final K key;
    private final String value;
    private final ValueType type;
    private final long ttlMillis;

    public PutCommand(K key, String value, ValueType type, long ttlMillis) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.ttlMillis = ttlMillis;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String process(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        ValueCodec<String> stringCodec = (ValueCodec<String>) valueCodecs.get(ValueType.STRING);

        byte[] finalValue = stringCodec.encode(value);
        cache.put(key, finalValue, type, ttlMillis);

        return OK.name();
    }
}
