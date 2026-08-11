package org.cache.protocol.handlers;

import org.cache.core.CacheOperations;

import java.util.List;

public class ClearHandler implements CommandHandler {

    private static final int COMMAND_PARTS = 1;

    private final CacheOperations<?> cacheService;

    public ClearHandler(CacheOperations<?> cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: CLEAR");
        }

        cacheService.clear();
        return ResponseConstants.OK.name();
    }
}
