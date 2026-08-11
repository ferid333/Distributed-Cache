package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;
import org.cache.cluster.hashing.ConsistentHashRing;
import org.cache.core.CacheOperations;
import org.cache.protocol.codec.StringKeyCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoutedCacheServiceTest {

    private final CacheNode nodeA = node("node-a", 8080, 2020, 10001);
    private final CacheNode nodeB = node("node-b", 8081, 2021, 10002);
    private final ClusterInfo clusterInfo = new ClusterInfo(1, List.of(nodeA, nodeB));
    private final StringKeyCodec keyCodec = new StringKeyCodec();

    @Test
    void getStringUsesLocalServiceWhenCurrentNodeOwnsKey() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyOwnedBy(nodeA);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                clusterInfo,
                forwardingClient,
                keyCodec
        );

        when(localService.getString(key)).thenReturn(Optional.of("apple"));

        assertEquals(Optional.of("apple"), service.getString(key));
        verify(localService).getString(key);
        verifyNoInteractions(forwardingClient);
    }

    @Test
    void getStringForwardsWhenAnotherNodeOwnsKey() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyOwnedBy(nodeB);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                clusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(nodeB, List.of("GET", key))).thenReturn(List.of("VALUE", "apple"));

        assertEquals(Optional.of("apple"), service.getString(key));
        verify(forwardingClient).forward(nodeB, List.of("GET", key));
        verifyNoInteractions(localService);
    }

    @Test
    void lrangeForwardsAndReturnsListValues() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyOwnedBy(nodeB);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                clusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(nodeB, List.of("LRANGE", key, "0", "2"))).thenReturn(List.of("one", "two"));

        assertEquals(Optional.of(List.of("one", "two")), service.lrange(key, 0, 2));
        verify(forwardingClient).forward(nodeB, List.of("LRANGE", key, "0", "2"));
        verifyNoInteractions(localService);
    }

    @Test
    void clusterDisabledUsesLocalService() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        RoutedCacheService<String> service = new RoutedCacheService<>(localService, nodeA, null, forwardingClient, keyCodec);

        when(localService.getString("fruit")).thenReturn(Optional.of("apple"));

        assertEquals(Optional.of("apple"), service.getString("fruit"));
        verify(localService).getString("fruit");
        verifyNoInteractions(forwardingClient);
    }

    @SuppressWarnings("unchecked")
    private CacheOperations<String> localService() {
        return mock(CacheOperations.class);
    }

    private String keyOwnedBy(CacheNode owner) {
        ConsistentHashRing ring = new ConsistentHashRing(clusterInfo);

        for (int index = 0; index < 10_000; index++) {
            String key = "key-" + index;
            if (ring.nodeFor(key).id().equals(owner.id())) {
                return key;
            }
        }

        throw new IllegalStateException("Could not find key owned by node: " + owner.id());
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
