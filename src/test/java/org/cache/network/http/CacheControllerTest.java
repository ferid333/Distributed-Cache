package org.cache.network.http;

import org.cache.core.CacheOperations;
import org.cache.core.ValueType;
import org.cache.core.metrics.Snapshot;
import org.cache.network.http.dto.GetResponseDto;
import org.cache.network.http.dto.ListResponseDto;
import org.cache.network.http.dto.MetricsResponseDto;
import org.cache.network.http.dto.SizeResponseDto;
import org.cache.network.http.dto.ValueRequestDto;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.codec.StringKeyCodec;
import org.cache.protocol.handlers.WrongValueTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheControllerTest {

    private CacheOperations<Object> cacheService;
    private CacheController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        cacheService = mock(CacheOperations.class);
        controller = new CacheController(cacheService, (KeyCodec<Object>) (KeyCodec<?>) new StringKeyCodec());
    }

    @Test
    void putStoresValueWithProvidedTtl() {
        ResponseEntity<Void> response = controller.put("fruit", new ValueRequestDto("apple", 1_000L));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cacheService).putString("fruit", "apple", 1_000);
    }

    @Test
    void putUsesZeroTtlWhenRequestTtlIsMissing() {
        ResponseEntity<Void> response = controller.put("fruit", new ValueRequestDto("apple", null));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cacheService).putString("fruit", "apple", 0);
    }

    @Test
    void getReturnsOkWithValueWhenPresent() {
        when(cacheService.getString("fruit")).thenReturn(Optional.of("apple"));

        ResponseEntity<GetResponseDto> response = controller.get("fruit");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new GetResponseDto("apple"), response.getBody());
    }

    @Test
    void getReturnsNotFoundWhenMissingAndConflictForWrongType() {
        when(cacheService.getString("missing")).thenReturn(Optional.empty());
        when(cacheService.getString("items")).thenThrow(new WrongValueTypeException(ValueType.STRING, ValueType.LIST));

        assertEquals(HttpStatus.NOT_FOUND, controller.get("missing").getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.get("items").getStatusCode());
    }

    @Test
    void deleteAndClearReturnNoContent() {
        assertEquals(HttpStatus.NO_CONTENT, controller.delete("fruit").getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.clear().getStatusCode());
        verify(cacheService).delete("fruit");
        verify(cacheService).clear();
    }

    @Test
    void sizeReturnsCurrentSize() {
        when(cacheService.size()).thenReturn(2);

        ResponseEntity<SizeResponseDto> response = controller.size();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new SizeResponseDto(2), response.getBody());
    }

    @Test
    void metricsReturnsSnapshotValues() {
        when(cacheService.metrics()).thenReturn(new Snapshot(1, 2, 3, 4, 0.5));

        ResponseEntity<MetricsResponseDto> response = controller.metrics();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new MetricsResponseDto(1, 2, 3, 4, 0.5), response.getBody());
    }

    @Test
    void pushReturnsNoContentAndConflictForWrongType() {
        doThrow(new WrongValueTypeException(ValueType.LIST, ValueType.STRING)).when(cacheService).push("fruit", "banana");

        assertEquals(HttpStatus.NO_CONTENT, controller.push("items", new ValueRequestDto("one", null)).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.push("fruit", new ValueRequestDto("banana", null)).getStatusCode());
        verify(cacheService).push("items", "one");
    }

    @Test
    void lrangeReturnsOkNotFoundBadRequestAndConflict() {
        when(cacheService.lrange("items", 0, 2)).thenReturn(Optional.of(List.of("one", "two")));
        when(cacheService.lrange("missing", 0, 2)).thenReturn(Optional.empty());
        when(cacheService.lrange("items", -1, 2)).thenThrow(new IllegalArgumentException("invalid"));
        when(cacheService.lrange("fruit", 0, 2)).thenThrow(new WrongValueTypeException(ValueType.LIST, ValueType.STRING));

        ResponseEntity<ListResponseDto> okResponse = controller.lrange("items", 0, 2);

        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        assertEquals(new ListResponseDto(List.of("one", "two")), okResponse.getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.lrange("missing", 0, 2).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.lrange("items", -1, 2).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, controller.lrange("fruit", 0, 2).getStatusCode());
    }
}
