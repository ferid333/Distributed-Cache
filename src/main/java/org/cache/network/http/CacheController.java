package org.cache.network.http;

import org.cache.core.CacheService;
import org.cache.core.metrics.Snapshot;
import org.cache.network.http.dto.GetResponseDto;
import org.cache.network.http.dto.ListResponseDto;
import org.cache.network.http.dto.MetricsResponseDto;
import org.cache.network.http.dto.SizeResponseDto;
import org.cache.network.http.dto.ValueRequestDto;
import org.cache.protocol.handlers.WrongValueTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheController {

    private final CacheService<String> cacheService;

    public CacheController(CacheService<String> cacheService) {
        this.cacheService = cacheService;
    }

    @PutMapping("/cache/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @RequestBody ValueRequestDto request) {
        cacheService.putString(key, request.value(), ttlMillis(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cache/{key}")
    public ResponseEntity<GetResponseDto> get(@PathVariable String key) {
        try {
            return cacheService.getString(key)
                    .map(value -> ResponseEntity.ok(new GetResponseDto(value)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @DeleteMapping("/cache/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        cacheService.delete(key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clear() {
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cache/size")
    public ResponseEntity<SizeResponseDto> size() {
        return ResponseEntity.ok(new SizeResponseDto(cacheService.size()));
    }

    @GetMapping("/cache/metrics")
    public ResponseEntity<MetricsResponseDto> metrics() {
        Snapshot snapshot = cacheService.metrics();
        return ResponseEntity.ok(new MetricsResponseDto(
                snapshot.getHits(),
                snapshot.getMisses(),
                snapshot.getEvictions(),
                snapshot.getExpirations(),
                snapshot.getHitRate()
        ));
    }

    @PostMapping("/cache/{key}/list")
    public ResponseEntity<Void> push(@PathVariable String key, @RequestBody ValueRequestDto request) {
        try {
            cacheService.push(key, request.value());
            return ResponseEntity.noContent().build();
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/cache/{key}/list")
    public ResponseEntity<ListResponseDto> lrange(
            @PathVariable String key,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam int to
    ) {
        try {
            return cacheService.lrange(key, from, to)
                    .map(values -> ResponseEntity.ok(new ListResponseDto(values)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private long ttlMillis(ValueRequestDto request) {
        return request.ttlMillis() == null ? 0 : request.ttlMillis();
    }
}
