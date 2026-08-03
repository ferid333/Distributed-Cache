package org.cache.protocol.codec;

import org.cache.core.ValueType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueCodecRegistryTest {

    @Test
    void getReturnsRegisteredCodec() {
        var registry = new ValueCodecRegistry();
        var codec = new StringValueCodec();

        registry.register(ValueType.STRING, codec);

        assertSame(codec, registry.get(ValueType.STRING));
    }

    @Test
    void registerReturnsSameRegistryForChaining() {
        var registry = new ValueCodecRegistry();

        ValueCodecRegistry returned = registry.register(ValueType.STRING, new StringValueCodec());

        assertSame(registry, returned);
    }

    @Test
    void getThrowsForUnsupportedValueType() {
        var registry = new ValueCodecRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.get(ValueType.STRING));
    }
}
