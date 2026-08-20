package org.cache.protocol.handlers;

public enum CommandType {
    PUT,
    GET,
    DELETE,
    SIZE,
    CLEAR,
    METRICS,
    PUSH,
    LRANGE,
    PING,
    TOPOLOGY_DIGEST,
    TOPOLOGY_GET,
    TOPOLOGY_APPLY,
    CLUSTER_ADD_NODE,
    CLUSTER_REMOVE_NODE,
    UNKNOWN;
}
