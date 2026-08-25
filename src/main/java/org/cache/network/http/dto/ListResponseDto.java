package org.cache.network.http.dto;

import java.util.List;

public record ListResponseDto(
        List<String> values
) implements HttpResponse {
}
