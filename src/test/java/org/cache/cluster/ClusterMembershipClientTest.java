package org.cache.cluster;

import org.cache.cluster.routing.ClusterForwardingClient;
import org.cache.cluster.routing.ClusterForwardingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterMembershipClientTest {

    private final CacheNode node = new CacheNode("node-a", "localhost", 8080, 2020, 10001);

    @Test
    void pingReturnsTrueWhenNodeRespondsWithPong() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("PING"))).thenReturn(List.of("PONG"));

        assertTrue(membershipClient.ping(node));
        verify(forwardingClient).forward(node, List.of("PING"));
    }

    @Test
    void pingReturnsFalseWhenNodeDoesNotRespondWithPong() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("PING"))).thenReturn(List.of("ERROR", "broken"));

        assertFalse(membershipClient.ping(node));
    }

    @Test
    void pingReturnsFalseWhenForwardingFails() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("PING"))).thenThrow(new ClusterForwardingException("closed"));

        assertFalse(membershipClient.ping(node));
    }

    @Test
    void topologyDigestParsesVersionAndFingerprint() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("TOPOLOGY_DIGEST")))
                .thenReturn(List.of("TOPOLOGY_DIGEST", "3", "abc"));

        var digest = membershipClient.topologyDigest(node).orElseThrow();

        assertEquals(3, digest.version());
        assertEquals("abc", digest.fingerprint());
    }

    @Test
    void topologyParsesFullTopologyResponse() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterTopology topology = new ClusterTopology(3, List.of(node), 1, 128);
        String encodedTopology = new ClusterTopologyCodec().encode(topology);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("TOPOLOGY_GET")))
                .thenReturn(List.of("TOPOLOGY", encodedTopology));

        ClusterTopology response = membershipClient.topology(node).orElseThrow();

        assertEquals(3, response.version());
        assertEquals(List.of(node), response.nodes());
    }

    @Test
    void applyTopologySendsEncodedTopology() {
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        ClusterTopology topology = new ClusterTopology(3, List.of(node), 1, 128);
        String encodedTopology = new ClusterTopologyCodec().encode(topology);
        ClusterMembershipClient membershipClient = new ClusterMembershipClient(forwardingClient);

        when(forwardingClient.forward(node, List.of("TOPOLOGY_APPLY", encodedTopology))).thenReturn(List.of("OK"));

        assertTrue(membershipClient.applyTopology(node, topology));
        verify(forwardingClient).forward(node, List.of("TOPOLOGY_APPLY", encodedTopology));
    }
}
