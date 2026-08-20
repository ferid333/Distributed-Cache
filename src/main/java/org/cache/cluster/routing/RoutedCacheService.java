package org.cache.cluster.routing;

import org.cache.cluster.CacheNode;
import org.cache.cluster.ClusterMembership;
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
    private final KeyCodec<K> keyCodec;
    private final CacheResponseParser responseParser;
    private final boolean forwardingAllowed;
    private final ClusterMembership clusterMembership;

    public RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec,
            boolean forwardingAllowed,
            ClusterMembership clusterMembership
    ) {
        this(
                localService,
                currentNode,
                forwardingClient,
                keyCodec,
                new CacheResponseParser(),
                forwardingAllowed,
                clusterMembership
        );
    }

    RoutedCacheService(
            CacheOperations<K> localService,
            CacheNode currentNode,
            ClusterForwardingClient forwardingClient,
            KeyCodec<K> keyCodec,
            CacheResponseParser responseParser,
            boolean forwardingAllowed,
            ClusterMembership clusterMembership
    ) {
        this.localService = localService;
        this.currentNode = currentNode;
        this.forwardingClient = forwardingClient;
        this.keyCodec = keyCodec;
        this.responseParser = responseParser;
        this.forwardingAllowed = forwardingAllowed;
        this.clusterMembership = clusterMembership;
    }

    @Override
    public void putString(K key, String value, long ttlMillis) {
        ReplicationTargets targets = writeTargetsFor(key);
        if (targets.includesCurrentNode()) {
            localService.putString(key, value, ttlMillis);
        }

        for (CacheNode replica : targets.remoteNodes()) {
            expectOk(forwardingClient.forward(replica, List.of(
                    "PUT",
                    keyCodec.encode(key),
                    value,
                    Long.toString(ttlMillis)
            )));
        }
    }

    @Override
    public Optional<String> getString(K key) {
        ClusterForwardingException failure = null;

        for (CacheNode owner : readOwnersFor(key)) {
            try {
                if (isCurrentNode(owner)) {
                    return localService.getString(key);
                }

                return remoteGetString(owner, key);
            } catch (ClusterForwardingException exception) {
                failure = exception;
            }
        }

        if (failure == null) {
            throw new ClusterForwardingException("No replica owner found for key: " + keyCodec.encode(key));
        }

        throw failure;
    }

    @Override
    public void push(K key, String value) {
        ReplicationTargets targets = writeTargetsFor(key);
        if (targets.includesCurrentNode()) {
            localService.push(key, value);
        }

        for (CacheNode replica : targets.remoteNodes()) {
            expectOk(forwardingClient.forward(replica, List.of("PUSH", keyCodec.encode(key), value)));
        }
    }

    @Override
    public Optional<List<String>> lrange(K key, int from, int to) {
        ClusterForwardingException failure = null;

        for (CacheNode owner : readOwnersFor(key)) {
            try {
                if (isCurrentNode(owner)) {
                    return localService.lrange(key, from, to);
                }

                return remoteLrange(owner, key, from, to);
            } catch (ClusterForwardingException exception) {
                failure = exception;
            }
        }

        if (failure == null) {
            throw new ClusterForwardingException("No replica owner found for key: " + keyCodec.encode(key));
        }

        throw failure;
    }

    @Override
    public void delete(K key) {
        ReplicationTargets targets = writeTargetsFor(key);
        if (targets.includesCurrentNode()) {
            localService.delete(key);
        }

        for (CacheNode replica : targets.remoteNodes()) {
            expectOk(forwardingClient.forward(replica, List.of("DELETE", keyCodec.encode(key))));
        }
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

    private ReplicationTargets writeTargetsFor(K key) {
        ConsistentHashRing hashRing = hashRing();

        if (hashRing == null) {
            return new ReplicationTargets(true, List.of());
        }

        List<CacheNode> owners = hashRing.nodesFor(keyCodec.encode(key));
        boolean includesCurrentNode = owners.stream()
                .anyMatch(this::isCurrentNode);

        if (!includesCurrentNode && !forwardingAllowed) {
            throw new ClusterForwardingException("Request routed to wrong node. Expected one of owners: "
                    + ownerIds(owners) + ", current node: " + currentNode.getId());
        }

        if (!forwardingAllowed) {
            return new ReplicationTargets(true, List.of());
        }

        return new ReplicationTargets(
                includesCurrentNode,
                owners.stream()
                        .filter(owner -> !isCurrentNode(owner))
                        .toList()
        );
    }

    private List<CacheNode> readOwnersFor(K key) {

        ConsistentHashRing hashRing = hashRing();

        if (hashRing == null) {
            return List.of(currentNode);
        }

        List<CacheNode> owners = hashRing.nodesFor(keyCodec.encode(key));
        boolean includesCurrentNode = owners.stream()
                .anyMatch(this::isCurrentNode);

        if (!includesCurrentNode && !forwardingAllowed) {
            throw new ClusterForwardingException("Request routed to wrong node. Expected one of owners: "
                    + ownerIds(owners) + ", current node: " + currentNode.getId());
        }

        if (!forwardingAllowed) {
            return List.of(currentNode);
        }

        return owners;
    }

    private Optional<String> remoteGetString(CacheNode owner, K key) {
        List<String> response = forwardingClient.forward(owner, List.of("GET", keyCodec.encode(key)));
        if (responseParser.isNotFound(response)) {
            return Optional.empty();
        }

        return responseParser.value(response)
                .or(() -> {
                    throw new ClusterForwardingException("Unexpected cluster response: " + response);
                });
    }

    private Optional<List<String>> remoteLrange(CacheNode owner, K key, int from, int to) {
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

    private boolean isCurrentNode(CacheNode owner) {
        return owner.getId().equals(currentNode.getId());
    }

    private ConsistentHashRing hashRing() {
        return clusterMembership == null ? null : clusterMembership.getHashRing();
    }

    private String ownerIds(List<CacheNode> owners) {
        return owners.stream()
                .map(CacheNode::getId)
                .toList()
                .toString();
    }

    private void expectOk(List<String> response) {
        if (responseParser.isOk(response)) {
            return;
        }

        throw new ClusterForwardingException("Unexpected cluster response: " + response);
    }
}
