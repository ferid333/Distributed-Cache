package org.cache.protocol.codec;

import org.cache.core.ValueType;

import java.util.EnumMap;

public class ValueCodecRegistry {

    private final EnumMap<ValueType, ValueCodec<?>> codecs = new EnumMap<>(ValueType.class);

    public ValueCodecRegistry register(ValueType type, ValueCodec<?> valueCodec) {
        codecs.put(type, valueCodec);
        return this;
    }

    public ValueCodec<?> get(ValueType type) {
        ValueCodec<?> valueCodec = codecs.get(type);

        if (valueCodec == null) {
            throw new IllegalArgumentException("unsupported value type: " + type);
        }

        return valueCodec;
    }
}
