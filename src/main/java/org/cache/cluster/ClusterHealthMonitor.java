package org.cache.cluster;

import org.springframework.context.SmartLifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClusterHealthMonitor implements SmartLifecycle {

    private static final int SUSPECTED_FAILURE_COUNT = 2;
    private static final int UNAVAILABLE_FAILURE_COUNT = 3;
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final long CHECK_INTERVAL_SECONDS = 5;

    private final CacheNode currentNode;
    private final ClusterMembership clusterMembership;
    private final ClusterMembershipClient membershipClient;
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final ScheduledExecutorService executor;
    private volatile boolean running;

    public ClusterHealthMonitor(
            CacheNode currentNode,
            ClusterMembership clusterMembership,
            ClusterMembershipClient membershipClient
    ) {
        this(currentNode, clusterMembership, membershipClient, Executors.newSingleThreadScheduledExecutor());
    }

    ClusterHealthMonitor(
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
                this::checkClusterSafely,
                INITIAL_DELAY_SECONDS,
                CHECK_INTERVAL_SECONDS,
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

    void checkCluster() {
        ClusterTopology topology = clusterMembership.currentTopology();
        if (topology == null) {
            return;
        }

        for (CacheNode node : topology.nodes()) {
            if (!node.getId().equals(currentNode.getId())) {
                checkNode(node);
            }
        }
    }

    private void checkClusterSafely() {
        try {
            checkCluster();
        } catch (RuntimeException exception) {
            System.err.println("Cluster health check failed: " + exception.getMessage());
        }
    }

    private void checkNode(CacheNode node) {
        if (membershipClient.ping(node)) {
            failureCounts.remove(node.getId());
            clusterMembership.markStatus(node.getId(), NodeStatus.HEALTHY);
            return;
        }

        int failures = failureCounts.getOrDefault(node.getId(), 0) + 1;
        failureCounts.put(node.getId(), failures);
        clusterMembership.markStatus(node.getId(), statusFor(failures));
    }

    private NodeStatus statusFor(int failures) {
        if (failures >= UNAVAILABLE_FAILURE_COUNT) {
            return NodeStatus.UNAVAILABLE;
        }

        if (failures == SUSPECTED_FAILURE_COUNT) {
            return NodeStatus.SUSPECTED;
        }

        return NodeStatus.HEALTHY;
    }
}
