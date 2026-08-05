package org.cache.network.http.dto;

public record ValueRequestDto(
        String value,
        Long ttlMillis
) {
}
