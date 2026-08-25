package org.cache.protocol.handlers;

import org.cache.core.ValueType;

public class WrongValueTypeException extends RuntimeException {

    private final ValueType expected;
    private final ValueType actual;

    public WrongValueTypeException(ValueType expected, ValueType actual) {
        super("expected " + expected.name().toLowerCase()
                + " but found " + actual.name().toLowerCase());
        this.expected = expected;
        this.actual = actual;
    }

    public ValueType getExpected() {
        return expected;
    }

    public ValueType getActual() {
        return actual;
    }
}
