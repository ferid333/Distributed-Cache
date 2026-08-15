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
    UNKNOWN;
}
