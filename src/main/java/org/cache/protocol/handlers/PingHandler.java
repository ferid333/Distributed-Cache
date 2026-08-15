package org.cache.protocol.handlers;

import java.util.List;

public class PingHandler implements CommandHandler {

    private static final int COMMAND_PARTS_SIZE = 1;
    private static final int VALUE_COMMAND_PARTS_SIZE = 2;
    private static final int VALUE_INDEX = 1;
    private static final String DEFAULT_RESPONSE = "PONG";

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS_SIZE && parts.size() != VALUE_COMMAND_PARTS_SIZE) {
            return TcpResponseSupport.error("usage: PING [value]");
        }

        if (parts.size() == VALUE_COMMAND_PARTS_SIZE) {
            return parts.get(VALUE_INDEX);
        }

        return DEFAULT_RESPONSE;
    }
}
