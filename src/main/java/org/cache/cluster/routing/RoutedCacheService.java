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

    public RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterInfo clusterInfo,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec
    ) {
        this(localService, currentNode, clusterInfo, forwardingClient, keyCodec, new CacheResponseParser());
    }

    RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterInfo clusterInfo,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec,
            CacheResponseParser responseParser
    ) {
        this.localService = localService;
        this.currentNode = currentNode;
        this.forwardingClient = forwardingClient;
        this.hashRing = clusterInfo == null ? null : new ConsistentHashRing(clusterInfo);
        this.keyCodec = keyCodec;
        this.responseParser = responseParser;
    }

    @Override
    public void putString(K key, String value, long ttlMillis) {
        CacheNode owner = ownerFor(key);
        if (isLocal(owner)) {
            localService.putString(key, value, ttlMillis);
            return;
        }

        expectOk(forwardingClient.forward(owner, List.of(
                "PUT",
                keyCodec.encode(key),
                value,
                Long.toString(ttlMillis)
        )));
    }

    @Override
    public Optional<String> getString(K key) {
        CacheNode owner = ownerFor(key);
        if (isLocal(owner)) {
            return localService.getString(key);
        }

        List<String> response = forwardingClient.forward(owner, List.of("GET", keyCodec.encode(key)));
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
        CacheNode owner = ownerFor(key);
        if (isLocal(owner)) {
            localService.push(key, value);
            return;
        }

        expectOk(forwardingClient.forward(owner, List.of("PUSH", keyCodec.encode(key), value)));
    }

    @Override
    public Optional<List<String>> lrange(K key, int from, int to) {
        CacheNode owner = ownerFor(key);
        if (isLocal(owner)) {
            return localService.lrange(key, from, to);
        }

        List<String> response = forwardingClient.forward(owner, List.of(
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
        CacheNode owner = ownerFor(key);
        if (isLocal(owner)) {
            localService.delete(key);
            return;
        }

        expectOk(forwardingClient.forward(owner, List.of("DELETE", keyCodec.encode(key))));
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

    private CacheNode ownerFor(K key) {
        return hashRing == null ? currentNode : hashRing.nodeFor(keyCodec.encode(key));
    }

    private boolean isLocal(CacheNode owner) {
        return owner.id().equals(currentNode.id());
    }

    private void expectOk(List<String> response) {
        if (responseParser.isOk(response)) {
            return;
        }

        throw new ClusterForwardingException("Unexpected cluster response: " + response);
    }
}
