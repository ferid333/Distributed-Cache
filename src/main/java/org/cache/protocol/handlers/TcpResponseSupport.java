package org.cache.protocol.handlers;

final class TcpResponseSupport {

    private TcpResponseSupport() {
    }

    static String error(String message) {
        return ResponseConstants.ERROR.name() + " " + message;
    }

    static String wrongType(WrongValueTypeException exception) {
        return error("key contains " + exception.getActual().name().toLowerCase() + " value");
    }
}
