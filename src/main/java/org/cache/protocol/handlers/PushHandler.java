package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodec;
import org.cache.core.CacheOperations;
import org.cache.protocol.codec.KeyCodecException;

import java.util.List;

public class PushHandler<K> implements CommandHandler {

    private static final int VALUE_COMMAND_PARTS = 3;
    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;

    private final KeyCodec<K> keyCodec;
    private final CacheOperations<K> cacheService;

    public PushHandler(KeyCodec<K> keyCodec, CacheOperations<K> cacheService) {
        this.keyCodec = keyCodec;
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != VALUE_COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: PUSH key value");
        }

        try {
            cacheService.push(keyCodec.decode(parts.get(KEY_INDEX)), parts.get(VALUE_INDEX));
            return ResponseConstants.OK.name();
        } catch (WrongValueTypeException exception) {
            return TcpResponseSupport.wrongType(exception);
        } catch (KeyCodecException exception) {
            return TcpResponseSupport.invalidKey(exception);
        }
    }
}
