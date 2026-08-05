package org.cache.protocol.handlers;

import org.cache.core.CacheService;

import java.util.List;

public class SizeHandler implements CommandHandler {

    private static final int COMMAND_PARTS = 1;

    private final CacheService<?> cacheService;

    public SizeHandler(CacheService<?> cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: SIZE");
        }

        return ResponseConstants.SIZE.name() + " " + cacheService.size();
    }
}
