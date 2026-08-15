package org.cache.network.http;

import org.cache.cluster.routing.ClusterForwardingException;
import org.cache.core.CacheOperations;
import org.cache.core.metrics.Snapshot;
import org.cache.network.http.dto.GetResponseDto;
import org.cache.network.http.dto.ListResponseDto;
import org.cache.network.http.dto.MetricsResponseDto;
import org.cache.network.http.dto.SizeResponseDto;
import org.cache.network.http.dto.ValueRequestDto;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.handlers.WrongValueTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private static final String PING_DEFAULT_RESPONSE = "PONG";

    private final CacheOperations<Object> cacheService;
    private final KeyCodec<Object> keyCodec;

    public CacheController(CacheOperations<Object> cacheService, KeyCodec<Object> keyCodec) {
        this.cacheService = cacheService;
        this.keyCodec = keyCodec;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @RequestBody ValueRequestDto request) {
        try {
            cacheService.putString(decodeKey(key), request.value(), ttlMillis(request));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (ClusterForwardingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<GetResponseDto> get(@PathVariable String key) {
        try {
            return cacheService.getString(decodeKey(key))
                    .map(value -> ResponseEntity.ok(new GetResponseDto(value)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ClusterForwardingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        try {
            cacheService.delete(decodeKey(key));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (ClusterForwardingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/size")
    public ResponseEntity<SizeResponseDto> size() {
        return ResponseEntity.ok(new SizeResponseDto(cacheService.size()));
    }

    @GetMapping("/ping")
    public ResponseEntity<GetResponseDto> ping(@RequestParam(required = false) String value) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.ok(new GetResponseDto(PING_DEFAULT_RESPONSE));
        }

        return ResponseEntity.ok(new GetResponseDto(value));
    }

    @GetMapping("/metrics")
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

    @PostMapping("/{key}/list")
    public ResponseEntity<Void> push(@PathVariable String key, @RequestBody ValueRequestDto request) {
        try {
            cacheService.push(decodeKey(key), request.value());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ClusterForwardingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/{key}/list")
    public ResponseEntity<ListResponseDto> lrange(
            @PathVariable String key,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam int to
    ) {
        try {
            return cacheService.lrange(decodeKey(key), from, to)
                    .map(values -> ResponseEntity.ok(new ListResponseDto(values)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (WrongValueTypeException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ClusterForwardingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private long ttlMillis(ValueRequestDto request) {
        return request.ttlMillis() == null ? 0 : request.ttlMillis();
    }

    private Object decodeKey(String key) {
        return keyCodec.decode(key);
    }
}
