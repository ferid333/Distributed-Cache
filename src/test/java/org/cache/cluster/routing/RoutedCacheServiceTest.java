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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoutedCacheServiceTest {

    private final CacheNode nodeA = node("node-a", 8080, 2020, 10001);
    private final CacheNode nodeB = node("node-b", 8081, 2021, 10002);
    private final CacheNode nodeC = node("node-c", 8082, 2022, 10003);
    private final ClusterInfo clusterInfo = new ClusterInfo(1, List.of(nodeA, nodeB));
    private final ClusterInfo replicatedClusterInfo = new ClusterInfo(2, List.of(nodeA, nodeB, nodeC));
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
    void getStringFallsBackToRemoteReplicaWhenPrimaryForwardFails() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedTo(nodeB, nodeC);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(nodeB, List.of("GET", key)))
                .thenThrow(new ClusterForwardingException("primary unavailable"));
        when(forwardingClient.forward(nodeC, List.of("GET", key))).thenReturn(List.of("VALUE", "apple"));

        assertEquals(Optional.of("apple"), service.getString(key));
        verify(forwardingClient).forward(nodeB, List.of("GET", key));
        verify(forwardingClient).forward(nodeC, List.of("GET", key));
        verifyNoInteractions(localService);
    }

    @Test
    void getStringFallsBackToLocalReplicaWhenPrimaryForwardFails() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedToRemotePrimaryAndLocalReplica();
        CacheNode primaryOwner = replicaOwnersFor(key).getFirst();
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(primaryOwner, List.of("GET", key)))
                .thenThrow(new ClusterForwardingException("primary unavailable"));
        when(localService.getString(key)).thenReturn(Optional.of("apple"));

        assertEquals(Optional.of("apple"), service.getString(key));
        verify(forwardingClient).forward(primaryOwner, List.of("GET", key));
        verify(localService).getString(key);
    }

    @Test
    void getStringRejectsRemoteOwnerWhenForwardingIsDisabled() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyNotReplicatedTo(nodeA);
        List<String> ownerIds = replicaOwnersFor(key)
                .stream()
                .map(CacheNode::id)
                .toList();
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec,
                false
        );

        var exception = assertThrows(ClusterForwardingException.class, () -> service.getString(key));

        assertEquals("Request routed to wrong node. Expected one of owners: " + ownerIds + ", current node: node-a",
                exception.getMessage());
        verifyNoInteractions(localService, forwardingClient);
    }

    @Test
    void putStringWritesToLocalAndRemoteReplicas() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedIncluding(nodeA);
        List<CacheNode> remoteOwners = replicaOwnersFor(key)
                .stream()
                .filter(owner -> !owner.id().equals(nodeA.id()))
                .toList();
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec
        );

        for (CacheNode owner : remoteOwners) {
            when(forwardingClient.forward(owner, List.of("PUT", key, "apple", "1000"))).thenReturn(List.of("OK"));
        }

        service.putString(key, "apple", 1_000);

        verify(localService).putString(key, "apple", 1_000);
        for (CacheNode owner : remoteOwners) {
            verify(forwardingClient).forward(owner, List.of("PUT", key, "apple", "1000"));
        }
    }

    @Test
    void putStringWritesToAllRemoteReplicasWhenCurrentNodeIsNotReplicaOwner() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedTo(nodeB, nodeC);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(nodeB, List.of("PUT", key, "apple", "1000"))).thenReturn(List.of("OK"));
        when(forwardingClient.forward(nodeC, List.of("PUT", key, "apple", "1000"))).thenReturn(List.of("OK"));

        service.putString(key, "apple", 1_000);

        verify(forwardingClient).forward(nodeB, List.of("PUT", key, "apple", "1000"));
        verify(forwardingClient).forward(nodeC, List.of("PUT", key, "apple", "1000"));
        verifyNoInteractions(localService);
    }

    @Test
    void putStringExecutesLocallyOnReplicaOwnerWhenForwardingIsDisabled() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedIncluding(nodeA);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec,
                false
        );

        service.putString(key, "apple", 1_000);

        verify(localService).putString(key, "apple", 1_000);
        verifyNoInteractions(forwardingClient);
    }

    @Test
    void putStringRejectsNonReplicaOwnerWhenForwardingIsDisabled() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyNotReplicatedTo(nodeA);
        List<String> ownerIds = replicaOwnersFor(key)
                .stream()
                .map(CacheNode::id)
                .toList();
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec,
                false
        );

        var exception = assertThrows(ClusterForwardingException.class, () -> service.putString(key, "apple", 1_000));

        assertEquals("Request routed to wrong node. Expected one of owners: " + ownerIds + ", current node: node-a",
                exception.getMessage());
        verifyNoInteractions(localService, forwardingClient);
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
    void lrangeFallsBackToRemoteReplicaWhenPrimaryForwardFails() {
        CacheOperations<String> localService = localService();
        ClusterForwardingClient forwardingClient = mock(ClusterForwardingClient.class);
        String key = keyReplicatedTo(nodeB, nodeC);
        RoutedCacheService<String> service = new RoutedCacheService<>(
                localService,
                nodeA,
                replicatedClusterInfo,
                forwardingClient,
                keyCodec
        );

        when(forwardingClient.forward(nodeB, List.of("LRANGE", key, "0", "2")))
                .thenThrow(new ClusterForwardingException("primary unavailable"));
        when(forwardingClient.forward(nodeC, List.of("LRANGE", key, "0", "2"))).thenReturn(List.of("one", "two"));

        assertEquals(Optional.of(List.of("one", "two")), service.lrange(key, 0, 2));
        verify(forwardingClient).forward(nodeB, List.of("LRANGE", key, "0", "2"));
        verify(forwardingClient).forward(nodeC, List.of("LRANGE", key, "0", "2"));
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

    private String keyReplicatedTo(CacheNode firstOwner, CacheNode secondOwner) {
        ConsistentHashRing ring = new ConsistentHashRing(replicatedClusterInfo);

        for (int index = 0; index < 10_000; index++) {
            String key = "replica-key-" + index;
            List<String> ownerIds = ring.nodesFor(key)
                    .stream()
                    .map(CacheNode::id)
                    .toList();

            if (ownerIds.equals(List.of(firstOwner.id(), secondOwner.id()))) {
                return key;
            }
        }

        throw new IllegalStateException("Could not find key replicated to owners: "
                + firstOwner.id() + ", " + secondOwner.id());
    }

    private String keyReplicatedIncluding(CacheNode owner) {
        for (int index = 0; index < 10_000; index++) {
            String key = "replica-key-" + index;
            boolean containsOwner = replicaOwnersFor(key).stream()
                    .anyMatch(replicaOwner -> replicaOwner.id().equals(owner.id()));

            if (containsOwner) {
                return key;
            }
        }

        throw new IllegalStateException("Could not find key replicated to owner: " + owner.id());
    }

    private String keyNotReplicatedTo(CacheNode owner) {
        for (int index = 0; index < 10_000; index++) {
            String key = "replica-key-" + index;
            boolean containsOwner = replicaOwnersFor(key).stream()
                    .anyMatch(replicaOwner -> replicaOwner.id().equals(owner.id()));

            if (!containsOwner) {
                return key;
            }
        }

        throw new IllegalStateException("Could not find key not replicated to owner: " + owner.id());
    }

    private String keyReplicatedToRemotePrimaryAndLocalReplica() {
        for (int index = 0; index < 10_000; index++) {
            String key = "replica-key-" + index;
            List<CacheNode> owners = replicaOwnersFor(key);
            boolean hasRemotePrimary = !owners.getFirst().id().equals(nodeA.id());
            boolean hasLocalReplica = owners.stream()
                    .skip(1)
                    .anyMatch(owner -> owner.id().equals(nodeA.id()));

            if (hasRemotePrimary && hasLocalReplica) {
                return key;
            }
        }

        throw new IllegalStateException("Could not find key with remote primary and local replica");
    }

    private List<CacheNode> replicaOwnersFor(String key) {
        return new ConsistentHashRing(replicatedClusterInfo).nodesFor(key);
    }

    private CacheNode node(String id, int httpPort, int tcpPort, int clusterPort) {
        return new CacheNode(id, "localhost", httpPort, tcpPort, clusterPort);
    }
}
