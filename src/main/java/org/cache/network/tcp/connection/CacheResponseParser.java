package org.cache.network.tcp.connection;

import org.cache.protocol.handlers.ResponseConstants;

import java.util.List;
import java.util.Optional;

public class CacheResponseParser {

    private static final int RESPONSE_TYPE_INDEX = 0;
    private static final int RESPONSE_VALUE_INDEX = 1;
    private static final int SINGLE_PART_RESPONSE_SIZE = 1;
    private static final int TWO_PART_RESPONSE_SIZE = 2;

    public boolean isOk(List<String> response) {
        return isResponse(response, ResponseConstants.OK, SINGLE_PART_RESPONSE_SIZE);
    }

    public boolean isError(List<String> response) {
        return isResponse(response, ResponseConstants.ERROR);
    }

    public boolean isNotFound(List<String> response) {
        return isResponse(response, ResponseConstants.NOT_FOUND, SINGLE_PART_RESPONSE_SIZE);
    }

    public Optional<String> value(List<String> response) {
        if (isResponse(response, ResponseConstants.VALUE, TWO_PART_RESPONSE_SIZE)) {
            return Optional.of(response.get(RESPONSE_VALUE_INDEX));
        }

        return Optional.empty();
    }

    public Optional<String> errorMessage(List<String> response) {
        if (isError(response) && response.size() > RESPONSE_VALUE_INDEX) {
            return Optional.of(response.get(RESPONSE_VALUE_INDEX));
        }

        return Optional.empty();
    }

    public boolean isKnownResponse(List<String> response) {
        return !response.isEmpty() && isKnownResponse(response.getFirst());
    }

    public boolean isKnownResponse(String value) {
        for (ResponseConstants responseType : ResponseConstants.values()) {
            if (responseType.name().equals(value)) {
                return true;
            }
        }

        return false;
    }

    public boolean isResponse(List<String> response, ResponseConstants responseType) {
        return !response.isEmpty() && response.get(RESPONSE_TYPE_INDEX).equals(responseType.name());
    }

    public boolean isResponse(List<String> response, ResponseConstants responseType, int size) {
        return response.size() == size && response.get(RESPONSE_TYPE_INDEX).equals(responseType.name());
    }
}
