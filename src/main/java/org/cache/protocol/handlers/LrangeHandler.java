package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodec;
import org.cache.core.CacheService;

import java.util.List;

public class LrangeHandler<K> implements CommandHandler {

    private static final int RANGE_TO_COMMAND_PARTS = 3;
    private static final int RANGE_FROM_TO_COMMAND_PARTS = 4;
    private static final int KEY_INDEX = 1;
    private static final int FROM_INDEX = 2;
    private static final int TO_INDEX = 3;
    private static final int DEFAULT_FROM_INDEX = 0;

    private final KeyCodec<K> keyCodec;
    private final CacheService<K> cacheService;

    public LrangeHandler(KeyCodec<K> keyCodec, CacheService<K> cacheService) {
        this.keyCodec = keyCodec;
        this.cacheService = cacheService;
    }

    @Override
    public String handle(List<String> parts) {
        if (parts.size() != RANGE_TO_COMMAND_PARTS && parts.size() != RANGE_FROM_TO_COMMAND_PARTS) {
            return TcpResponseSupport.error("usage: LRANGE key [from] to");
        }

        try {
            int from = parts.size() == RANGE_FROM_TO_COMMAND_PARTS
                    ? Integer.parseInt(parts.get(FROM_INDEX))
                    : DEFAULT_FROM_INDEX;
            int to = parts.size() == RANGE_FROM_TO_COMMAND_PARTS
                    ? Integer.parseInt(parts.get(TO_INDEX))
                    : Integer.parseInt(parts.get(FROM_INDEX));

            return cacheService.lrange(keyCodec.decode(parts.get(KEY_INDEX)), from, to)
                    .map(this::formatList)
                    .orElse(ResponseConstants.NOT_FOUND.name());
        } catch (NumberFormatException exception) {
            return TcpResponseSupport.error("range indexes must be numbers");
        } catch (IllegalArgumentException exception) {
            return TcpResponseSupport.error("invalid range: from must be >= 0 and to must be >= from");
        } catch (WrongValueTypeException exception) {
            return TcpResponseSupport.wrongType(exception);
        }
    }

    private String formatList(List<String> values) {
        if (values.isEmpty()) {
            return ResponseConstants.LIST.name();
        }

        return ResponseConstants.LIST.name() + " " + String.join(", ", values);
    }
}
