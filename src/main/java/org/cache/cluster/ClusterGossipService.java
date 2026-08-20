package org.cache.cluster;

import org.springframework.context.SmartLifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClusterGossipService implements SmartLifecycle {

    private static final long INITIAL_DELAY_SECONDS = 3;
    private static final long GOSSIP_INTERVAL_SECONDS = 3;

    private final CacheNode currentNode;
    private final ClusterMembership clusterMembership;
    private final ClusterMembershipClient membershipClient;
    private final ScheduledExecutorService executor;
    private volatile boolean running;

    public ClusterGossipService(
            CacheNode currentNode,
            ClusterMembership clusterMembership,
            ClusterMembershipClient membershipClient
    ) {
        this(currentNode, clusterMembership, membershipClient, Executors.newSingleThreadScheduledExecutor());
    }

    ClusterGossipService(
            CacheNode currentNode,
            ClusterMembership clusterMembership,
            ClusterMembershipClient membershipClient,
            ScheduledExecutorService executor
    ) {
        this.currentNode = currentNode;
        this.clusterMembership = clusterMembership;
        this.membershipClient = membershipClient;
        this.executor = executor;
    }

    @Override
    public void start() {
        if (running || clusterMembership.currentTopology() == null) {
            return;
        }

        running = true;
        executor.scheduleAtFixedRate(
                this::gossipOnce,
                INITIAL_DELAY_SECONDS,
                GOSSIP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void broadcastTopology() {
        ClusterTopology topology = clusterMembership.currentTopology();
        if (topology == null) {
            return;
        }

        for (CacheNode peer : topology.nodes()) {
            if (isNotCurrentNode(peer)) {
                membershipClient.applyTopology(peer, topology);
            }
        }
    }

    void gossipOnce() {
        try {
            ClusterTopology topology = clusterMembership.currentTopology();
            if (topology == null) {
                return;
            }

            for (CacheNode peer : topology.nodes()) {
                if (isNotCurrentNode(peer)) {
                    gossipWith(peer);
                }
            }
        } catch (RuntimeException exception) {
            System.err.println("Cluster gossip failed: " + exception.getMessage());
        }

    }

    private void gossipWith(CacheNode peer) {
        ClusterTopology localTopology = clusterMembership.currentTopology();
        membershipClient.topologyDigest(peer).ifPresent(peerDigest -> {
            if (peerDigest.version() < localTopology.version()) {
                membershipClient.applyTopology(peer, localTopology);
                return;
            }

            if (peerDigest.version() > localTopology.version()) {
                applyPeerTopology(peer);
                return;
            }

            if (!peerDigest.fingerprint().equals(localTopology.fingerprint())) {
                resolveSameVersionConflict(peer, localTopology);
            }
        });
    }

    private void resolveSameVersionConflict(CacheNode peer, ClusterTopology localTopology) {
        boolean appliedPeerTopology = applyPeerTopology(peer);
        if (!appliedPeerTopology) {
            membershipClient.applyTopology(peer, localTopology);
        }
    }

    private boolean applyPeerTopology(CacheNode peer) {
        return membershipClient.topology(peer)
                .map(clusterMembership::applyTopology)
                .orElse(false);
    }

    private boolean isNotCurrentNode(CacheNode node) {
        return !node.getId().equals(currentNode.getId());
    }
}
