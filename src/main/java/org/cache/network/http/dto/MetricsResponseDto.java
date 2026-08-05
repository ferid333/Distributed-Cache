package org.cache.network.http.dto;

public record MetricsResponseDto(
        long hits,
        long misses,
        long evictions,
        long expirations,
        double hitRate
) implements HttpResponse {
}
