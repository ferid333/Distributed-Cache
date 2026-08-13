package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterInfo;
import org.cache.cluster.hashing.ConsistentHashRing;
import org.cache.core.CacheOperations;
import org.cache.core.metrics.Snapshot;
import org.cache.network.tcp.connection.CacheResponseParser;
import org.cache.protocol.codec.KeyCodec;

import java.util.List;
import java.util.Optional;

public class RoutedCacheService<K> implements CacheOperations<K> {

    private final CacheOperations<K> localService;
    private final CacheNode currentNode;
    private final ClusterForwardingClient forwardingClient;
    private final ConsistentHashRing hashRing;
    private final KeyCodec<K> keyCodec;
    private final CacheResponseParser responseParser;
    private final boolean forwardingAllowed;

    public RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterInfo clusterInfo,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec
    ) {
        this(localService, currentNode, clusterInfo, forwardingClient, keyCodec, true);
    }

    public RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterInfo clusterInfo,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec,
            boolean forwardingAllowed
    ) {
        this(localService, currentNode, clusterInfo, forwardingClient, keyCodec, new CacheResponseParser(), forwardingAllowed);
    }

    RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterInfo clusterInfo,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec,
            CacheResponseParser responseParser,
            boolean forwardingAllowed
    ) {
        this.localService = localService;
        this.currentNode = currentNode;
        this.forwardingClient = forwardingClient;
        this.hashRing = clusterInfo == null ? null : new ConsistentHashRing(clusterInfo);
        this.keyCodec = keyCodec;
        this.responseParser = responseParser;
        this.forwardingAllowed = forwardingAllowed;
    }

    @Override
    public void putString(K key, String value, long ttlMillis) {
        Optional<CacheNode> remoteOwner = remoteOwnerFor(key);
        if (remoteOwner.isEmpty()) {
            localService.putString(key, value, ttlMillis);
            return;
        }

        expectOk(forwardingClient.forward(remoteOwner.get(), List.of(
                "PUT",
                keyCodec.encode(key),
                value,
                Long.toString(ttlMillis)
        )));
    }

    @Override
    public Optional<String> getString(K key) {
        Optional<CacheNode> remoteOwner = remoteOwnerFor(key);
        if (remoteOwner.isEmpty()) {
            return localService.getString(key);
        }

        List<String> response = forwardingClient.forward(remoteOwner.get(), List.of("GET", keyCodec.encode(key)));
        if (responseParser.isNotFound(response)) {
            return Optional.empty();
        }

        return responseParser.value(response)
                .or(() -> {
                    throw new ClusterForwardingException("Unexpected cluster response: " + response);
                });
    }

    @Override
    public void push(K key, String value) {
        Optional<CacheNode> remoteOwner = remoteOwnerFor(key);
        if (remoteOwner.isEmpty()) {
            localService.push(key, value);
            return;
        }

        expectOk(forwardingClient.forward(remoteOwner.get(), List.of("PUSH", keyCodec.encode(key), value)));
    }

    @Override
    public Optional<List<String>> lrange(K key, int from, int to) {
        Optional<CacheNode> remoteOwner = remoteOwnerFor(key);
        if (remoteOwner.isEmpty()) {
            return localService.lrange(key, from, to);
        }

        List<String> response = forwardingClient.forward(remoteOwner.get(), List.of(
                "LRANGE",
                keyCodec.encode(key),
                Integer.toString(from),
                Integer.toString(to)
        ));

        if (responseParser.isNotFound(response)) {
            return Optional.empty();
        }

        if (response.isEmpty()) {
            return Optional.of(List.of());
        }

        if (!responseParser.isKnownResponse(response)) {
            return Optional.of(List.copyOf(response));
        }

        throw new ClusterForwardingException("Unexpected cluster response: " + response);
    }

    @Override
    public void delete(K key) {
        Optional<CacheNode> remoteOwner = remoteOwnerFor(key);
        if (remoteOwner.isEmpty()) {
            localService.delete(key);
            return;
        }

        expectOk(forwardingClient.forward(remoteOwner.get(), List.of("DELETE", keyCodec.encode(key))));
    }

    @Override
    public int size() {
        return localService.size();
    }

    @Override
    public void clear() {
        localService.clear();
    }

    @Override
    public Snapshot metrics() {
        return localService.metrics();
    }

    private Optional<CacheNode> remoteOwnerFor(K key) {
        CacheNode owner = hashRing == null ? currentNode : hashRing.nodeFor(keyCodec.encode(key));
        if (owner.id().equals(currentNode.id())) {
            return Optional.empty();
        }

        if (!forwardingAllowed) {
            throw new ClusterForwardingException("Request routed to wrong node. Expected owner: " + owner.id()
                    + ", current node: " + currentNode.id());
        }

        return Optional.of(owner);
    }

    private void expectOk(List<String> response) {
        if (responseParser.isOk(response)) {
            return;
        }

        throw new ClusterForwardingException("Unexpected cluster response: " + response);
    }
}
