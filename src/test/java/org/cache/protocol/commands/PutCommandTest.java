package org.cache.protocol.commands;

import org.cache.core.Cache;
import org.cache.core.ValueType;
import org.cache.protocol.codec.StringValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PutCommandTest {

    @Test
    void processStoresEncodedStringValueAndReturnsOk() {
        Cache<String> cache = mock(Cache.class);
        var valueCodecs = new ValueCodecRegistry()
                .register(ValueType.STRING, new StringValueCodec());
        var command = new PutCommand<>("fruit", "apple", ValueType.STRING, 1_000);
        ArgumentCaptor<byte[]> valueCaptor = ArgumentCaptor.forClass(byte[].class);

        String response = command.process(cache, valueCodecs);

        verify(cache).put(eq("fruit"), valueCaptor.capture(), eq(ValueType.STRING), eq(1_000L));
        assertArrayEquals("apple".getBytes(UTF_8), valueCaptor.getValue());
        assertEquals(ResponseConstants.OK.name(), response);
    }
}
