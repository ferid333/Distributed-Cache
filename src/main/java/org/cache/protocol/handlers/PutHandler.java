package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodec;
import org.cache.core.CacheService;

import java.util.List;

public class PutHandler<K> implements CommandHandler {

    private static final int VALUE_COMMAND_PARTS = 3;
    private static final int TTL_COMMAND_PARTS = 4;
    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;
    private static final int TTL_INDEX = 3;

    private final KeyCodec<K> keyCodec;
    private final CacheService<K> cacheService;

    public PutHandler(KeyCodec<K> keyCodec, CacheService<K> cacheService) {
        this.keyCodec = keyCodec;
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != VALUE_COMMAND_PARTS && parts.size() != TTL_COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: PUT key value [ttlMillis]");
        }

        try {
            long ttlMillis = parts.size() == TTL_COMMAND_PARTS ? Long.parseLong(parts.get(TTL_INDEX)) : 0;
            cacheService.putString(keyCodec.decode(parts.get(KEY_INDEX)), parts.get(VALUE_INDEX), ttlMillis);
            return ResponseConstants.OK.name();
        } catch (NumberFormatException exception) {
            return TcpResponseSupport.error("ttl must be a number");
        }
    }
}
