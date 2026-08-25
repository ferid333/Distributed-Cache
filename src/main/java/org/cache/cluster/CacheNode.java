package org.cache.cluster;

import java.util.Objects;

public final class CacheNode {

    private final String id;

    private final String host;

    private final int httpPort;

    private final int tcpPort;

    private final int clusterPort;

    private volatile NodeStatus status;

    public CacheNode(String id, String host, int httpPort, int tcpPort, int clusterPort) {
        this(id, host, httpPort, tcpPort, clusterPort, NodeStatus.HEALTHY);
    }

    public CacheNode(String id, String host, int httpPort, int tcpPort, int clusterPort, NodeStatus status) {
        this.id = Objects.requireNonNull(id, "Node id must not be null");
        this.host = Objects.requireNonNull(host, "Node host must not be null");
        this.httpPort = httpPort;
        this.tcpPort = tcpPort;
        this.clusterPort = clusterPort;
        this.status = Objects.requireNonNull(status, "Node status must not be null");
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getClusterPort() {
        return clusterPort;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = Objects.requireNonNull(status, "Node status must not be null");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof CacheNode cacheNode)) {
            return false;
        }

        return httpPort == cacheNode.httpPort
                && tcpPort == cacheNode.tcpPort
                && clusterPort == cacheNode.clusterPort
                && id.equals(cacheNode.id)
                && host.equals(cacheNode.host)
                && status == cacheNode.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, host, httpPort, tcpPort, clusterPort, status);
    }

}
