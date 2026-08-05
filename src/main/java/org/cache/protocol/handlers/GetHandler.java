package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodec;
import org.cache.core.CacheService;

import java.util.List;

public class GetHandler<K> implements CommandHandler {

    private static final int KEY_COMMAND_PARTS = 2;
    private static final int KEY_INDEX = 1;

    private final KeyCodec<K> keyCodec;
    private final CacheService<K> cacheService;

    public GetHandler(KeyCodec<K> keyCodec, CacheService<K> cacheService) {
        this.keyCodec = keyCodec;
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != KEY_COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: GET key");
        }

        try {
            return cacheService.getString(keyCodec.decode(parts.get(KEY_INDEX)))
                    .map(value -> ResponseConstants.VALUE.name() + " " + value)
                    .orElse(ResponseConstants.NOT_FOUND.name());
        } catch (WrongValueTypeException exception) {
            return TcpResponseSupport.wrongType(exception);
        }
    }
}
