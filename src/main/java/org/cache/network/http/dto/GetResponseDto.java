package org.cache.network.http.dto;

public record GetResponseDto (
        String value
) implements HttpResponse {
}
