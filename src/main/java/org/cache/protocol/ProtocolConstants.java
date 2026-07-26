package org.cache.protocol;

public final class ProtocolConstants {

    public static final char SIMPLE_STRING_PREFIX = '+';
    public static final char ERROR_PREFIX = '-';
    public static final char INTEGER_PREFIX = ':';
    public static final char BULK_STRING_PREFIX = '$';
    public static final char ARRAY_PREFIX = '*';
    public static final char CARRIAGE_RETURN = '\r';
    public static final char NEW_LINE = '\n';
    public static final String CRLF = "\r\n";

    private ProtocolConstants() {
    }
}
