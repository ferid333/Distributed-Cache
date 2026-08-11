package org.cache.protocol.handlers;

public enum CommandType {
    PUT(true),
    GET(true),
    DELETE(true),
    SIZE(false),
    CLEAR(false),
    METRICS(false),
    PUSH(true),
    LRANGE(true),
    UNKNOWN(false);

    private final boolean isKeyCommand;

    CommandType(boolean isKeyCommand) {
        this.isKeyCommand = isKeyCommand;
    }

    public boolean isKeyCommand() {
        return isKeyCommand;
    }
}
