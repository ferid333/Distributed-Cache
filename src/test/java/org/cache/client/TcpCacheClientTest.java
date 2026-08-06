package org.cache.client;

import org.cache.client.serializer.StringSerializer;
import org.cache.core.metrics.Snapshot;
import org.cache.network.tcp.connection.RespConnection;
import org.cache.protocol.codec.StringKeyCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TcpCacheClientTest {

    private RespConnection connection;
    private TcpCacheClient<String, String> client;

    @BeforeEach
    void setUp() {
        connection = mock(RespConnection.class);
        client = new TcpCacheClient<>(connection, new StringKeyCodec(), new StringSerializer());
    }

    @Test
    void putSendsPutCommand() throws Exception {
        when(connection.sendCommandForResponse(List.of("PUT", "fruit", "apple"))).thenReturn(List.of("OK"));

        client.put("fruit", "apple");

        verify(connection).sendCommandForResponse(List.of("PUT", "fruit", "apple"));
    }

    @Test
    void putWithTtlSendsPutCommandWithTtl() throws Exception {
        when(connection.sendCommandForResponse(List.of("PUT", "fruit", "apple", "1000"))).thenReturn(List.of("OK"));

        client.put("fruit", "apple", 1_000);

        verify(connection).sendCommandForResponse(List.of("PUT", "fruit", "apple", "1000"));
    }

    @Test
    void getReturnsValueWhenServerReturnsValue() throws Exception {
        when(connection.sendCommandForResponse(List.of("GET", "fruit"))).thenReturn(List.of("VALUE", "apple"));

        Optional<String> value = client.get("fruit");

        assertEquals(Optional.of("apple"), value);
    }

    @Test
    void getReturnsEmptyWhenServerReturnsNotFound() throws Exception {
        when(connection.sendCommandForResponse(List.of("GET", "missing"))).thenReturn(List.of("NOT_FOUND"));

        Optional<String> value = client.get("missing");

        assertEquals(Optional.empty(), value);
    }

    @Test
    void deleteSendsDeleteCommand() throws Exception {
        when(connection.sendCommandForResponse(List.of("DELETE", "fruit"))).thenReturn(List.of("OK"));

        client.delete("fruit");

        verify(connection).sendCommandForResponse(List.of("DELETE", "fruit"));
    }

    @Test
    void sizeReturnsParsedSize() throws Exception {
        when(connection.sendCommandForResponse(List.of("SIZE"))).thenReturn(List.of("SIZE", "3"));

        int size = client.size();

        assertEquals(3, size);
    }

    @Test
    void clearSendsClearCommand() throws Exception {
        when(connection.sendCommandForResponse(List.of("CLEAR"))).thenReturn(List.of("OK"));

        client.clear();

        verify(connection).sendCommandForResponse(List.of("CLEAR"));
    }

    @Test
    void pushSendsPushCommand() throws Exception {
        when(connection.sendCommandForResponse(List.of("PUSH", "fruits", "apple"))).thenReturn(List.of("OK"));

        client.push("fruits", "apple");

        verify(connection).sendCommandForResponse(List.of("PUSH", "fruits", "apple"));
    }

    @Test
    void lrangeSendsRangeCommandAndReturnsValues() throws Exception {
        when(connection.sendCommandForResponse(List.of("LRANGE", "fruits", "0", "2")))
                .thenReturn(List.of("apple", "banana"));

        List<String> values = client.lrange("fruits", 0, 2);

        assertEquals(List.of("apple", "banana"), values);
    }

    @Test
    void lrangeWithToUsesZeroAsFromIndex() throws Exception {
        when(connection.sendCommandForResponse(List.of("LRANGE", "fruits", "0", "2")))
                .thenReturn(List.of("apple", "banana"));

        List<String> values = client.lrange("fruits", 2);

        assertEquals(List.of("apple", "banana"), values);
    }

    @Test
    void lrangeReturnsEmptyListWhenServerReturnsNotFound() throws Exception {
        when(connection.sendCommandForResponse(List.of("LRANGE", "missing", "0", "2")))
                .thenReturn(List.of("NOT_FOUND"));

        List<String> values = client.lrange("missing", 0, 2);

        assertEquals(List.of(), values);
    }

    @Test
    void metricsReturnsParsedSnapshot() throws Exception {
        when(connection.sendCommandForResponse(List.of("METRICS"))).thenReturn(List.of(
                "METRICS",
                "hits", "1",
                "misses", "2",
                "evictions", "3",
                "expirations", "4",
                "hitRate", "0.5"
        ));

        Snapshot metrics = client.metrics();

        assertEquals(1, metrics.getHits());
        assertEquals(2, metrics.getMisses());
        assertEquals(3, metrics.getEvictions());
        assertEquals(4, metrics.getExpirations());
        assertEquals(0.5, metrics.getHitRate());
    }

    @Test
    void commandThrowsWhenServerReturnsError() throws Exception {
        when(connection.sendCommandForResponse(List.of("GET", "fruit"))).thenReturn(List.of("ERROR", "broken"));

        assertThrows(CacheClientException.class, () -> client.get("fruit"));
    }

    @Test
    void commandThrowsWhenConnectionFails() throws Exception {
        when(connection.sendCommandForResponse(List.of("GET", "fruit"))).thenThrow(new IOException("closed"));

        assertThrows(CacheClientException.class, () -> client.get("fruit"));
    }

    @Test
    void closeClosesConnection() throws Exception {
        client.close();

        verify(connection).close();
    }
}
