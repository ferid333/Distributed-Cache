package org.cache.protocol.handlers;

import org.cache.protocol.codec.KeyCodecException;

final class TcpResponseSupport {

    private TcpResponseSupport() {
    }

    static String error(String message) {
        return ResponseConstants.ERROR.name() + " " + message;
    }

    static String wrongType(WrongValueTypeException exception) {
        return error("key contains " + exception.getActual().name().toLowerCase() + " value");
    }

    static String invalidKey(KeyCodecException exception) {
        return error(exception.getMessage());
    }
}
