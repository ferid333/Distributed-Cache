package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodec;
import org.cache.core.CacheService;
import org.cache.protocol.codec.KeyCodecException;

import java.util.List;

public class DeleteHandler<K> implements CommandHandler {

    private static final int KEY_COMMAND_PARTS = 2;
    private static final int KEY_INDEX = 1;

    private final KeyCodec<K> keyCodec;
    private final CacheService<K> cacheService;

    public DeleteHandler(KeyCodec<K> keyCodec, CacheService<K> cacheService) {
        this.keyCodec = keyCodec;
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != KEY_COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: DELETE key");
        }

        try {
            cacheService.delete(keyCodec.decode(parts.get(KEY_INDEX)));
            return ResponseConstants.OK.name();
        } catch (KeyCodecException exception) {
            return TcpResponseSupport.invalidKey(exception);
        }
    }
}
