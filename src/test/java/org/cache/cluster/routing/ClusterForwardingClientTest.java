package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;
import org.cache.network.tcp.connection.RespCommandClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterForwardingClientTest {

    private final CacheNode node = new CacheNode("node-a", "localhost", 8080, 2020, 10001);

    @Test
    void forwardSendsCommandToClusterPort() throws Exception {
        RespCommandClient commandClient = mock(RespCommandClient.class);
        ClusterForwardingClient forwardingClient = new ClusterForwardingClient(commandClient);

        when(commandClient.send("localhost", 10001, List.of("GET", "fruit"))).thenReturn(List.of("VALUE", "apple"));

        assertEquals(List.of("VALUE", "apple"), forwardingClient.forward(node, List.of("GET", "fruit")));
        verify(commandClient).send("localhost", 10001, List.of("GET", "fruit"));
    }

    @Test
    void forwardWrapsIoFailure() throws Exception {
        RespCommandClient commandClient = mock(RespCommandClient.class);
        ClusterForwardingClient forwardingClient = new ClusterForwardingClient(commandClient);

        when(commandClient.send("localhost", 10001, List.of("GET", "fruit"))).thenThrow(new IOException("closed"));

        assertThrows(ClusterForwardingException.class, () -> forwardingClient.forward(node, List.of("GET", "fruit")));
    }

    @Test
    void pingReturnsTrueWhenNodeRespondsWithPong() throws Exception {
        RespCommandClient commandClient = mock(RespCommandClient.class);
        ClusterForwardingClient forwardingClient = new ClusterForwardingClient(commandClient);

        when(commandClient.send("localhost", 10001, List.of("PING"))).thenReturn(List.of("PONG"));

        assertTrue(forwardingClient.ping(node));
        verify(commandClient).send("localhost", 10001, List.of("PING"));
    }

    @Test
    void pingReturnsFalseWhenNodeDoesNotRespondWithPong() throws Exception {
        RespCommandClient commandClient = mock(RespCommandClient.class);
        ClusterForwardingClient forwardingClient = new ClusterForwardingClient(commandClient);

        when(commandClient.send("localhost", 10001, List.of("PING"))).thenReturn(List.of("ERROR", "broken"));

        assertFalse(forwardingClient.ping(node));
    }

    @Test
    void pingReturnsFalseWhenConnectionFails() throws Exception {
        RespCommandClient commandClient = mock(RespCommandClient.class);
        ClusterForwardingClient forwardingClient = new ClusterForwardingClient(commandClient);

        when(commandClient.send("localhost", 10001, List.of("PING"))).thenThrow(new IOException("closed"));

        assertFalse(forwardingClient.ping(node));
    }
}
