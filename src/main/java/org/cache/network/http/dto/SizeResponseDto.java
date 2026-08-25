package org.cache.network.http.dto;

public record SizeResponseDto(
        int size
) implements HttpResponse {
}
