package org.cache.client;

import org.cache.client.serializer.Serializer;
import org.cache.core.metrics.Snapshot;
import org.cache.network.tcp.connection.CacheResponseParser;
import org.cache.network.tcp.connection.RespCommandClient;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.handlers.ResponseConstants;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class TcpCacheClient<K, V> implements CacheClient<K, V> {

    private static final int TWO_PART_RESPONSE_SIZE = 2;
    private static final int FIRST_VALUE_INDEX = 1;
    private static final int METRIC_NAME_VALUE_START_INDEX = 1;
    private static final int METRIC_NAME_VALUE_PAIR_SIZE = 2;
    private static final int DEFAULT_LRANGE_FROM_INDEX = 0;

    private final RespCommandClient commandClient;
    private final String address;
    private final int port;
    private final KeyCodec<K> keyCodec;
    private final CacheResponseParser responseParser;
    private final Serializer<V> valueSerializer;

    TcpCacheClient(
            RespCommandClient commandClient,
            String address,
            int port,
            KeyCodec<K> keyCodec,
            Serializer<V> valueSerializer
    ) {
        this(commandClient, address, port, keyCodec, valueSerializer, new CacheResponseParser());
    }

    TcpCacheClient(
            RespCommandClient commandClient,
            String address,
            int port,
            KeyCodec<K> keyCodec,
            Serializer<V> valueSerializer,
            CacheResponseParser responseParser
    ) {
        this.commandClient = commandClient;
        this.address = address;
        this.port = port;
        this.keyCodec = keyCodec;
        this.valueSerializer = valueSerializer;
        this.responseParser = responseParser;
    }

    @Override
    public void put(K key, V value, long ttl) {
        expectOk(List.of("PUT", keyCodec.encode(key), valueSerializer.encode(value), Long.toString(ttl)));
    }

    @Override
    public void put(K key, V value) {
        expectOk(List.of("PUT", keyCodec.encode(key), valueSerializer.encode(value)));
    }

    @Override
    public Optional<V> get(K key) {
        List<String> response = send(List.of("GET", keyCodec.encode(key)));

        if (responseParser.isNotFound(response)) {
            return Optional.empty();
        }

        return responseParser.value(response)
                .map(valueSerializer::decode)
                .or(() -> {
                    throw unexpectedResponse(response);
                });
    }

    @Override
    public void delete(K key) {
        expectOk(List.of("DELETE", keyCodec.encode(key)));
    }

    @Override
    public Snapshot metrics() {
        List<String> response = send(List.of("METRICS"));

        if (response.isEmpty() || !response.getFirst().equals(ResponseConstants.METRICS.name())) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }

        long hits = 0, misses = 0, evictions = 0, expirations = 0;
        double hitRate = 0.0;

        if ((response.size() - METRIC_NAME_VALUE_START_INDEX) % METRIC_NAME_VALUE_PAIR_SIZE != 0) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }

        for (int i = METRIC_NAME_VALUE_START_INDEX; i < response.size(); i += METRIC_NAME_VALUE_PAIR_SIZE) {
            String name = response.get(i);
            String value = response.get(i + FIRST_VALUE_INDEX);

            try {
                switch (name) {
                    case "hits" -> hits = Long.parseLong(value);
                    case "misses" -> misses = Long.parseLong(value);
                    case "evictions" -> evictions = Long.parseLong(value);
                    case "expirations" -> expirations = Long.parseLong(value);
                    case "hitRate" -> hitRate = Double.parseDouble(value);
                    default -> throw new CacheClientException("unexpected cache server response: " + response);
                }
            } catch (NumberFormatException exception) {
                throw new CacheClientException("unexpected cache server response: " + response);
            }
        }

        return new Snapshot(hits, misses, evictions, expirations, hitRate);
    }

    @Override
    public int size() {
        List<String> response = send(List.of("SIZE"));

        if (!responseParser.isResponse(response, ResponseConstants.SIZE, TWO_PART_RESPONSE_SIZE)) {
            throw unexpectedResponse(response);
        }

        return Integer.parseInt(response.get(FIRST_VALUE_INDEX));
    }

    @Override
    public void clear() {
        expectOk(List.of("CLEAR"));
    }

    @Override
    public void push(K key, V value) {
        expectOk(List.of("PUSH", keyCodec.encode(key), valueSerializer.encode(value)));
    }

    @Override
    public List<V> lrange(K key, int to) {
        return lrange(key, DEFAULT_LRANGE_FROM_INDEX, to);
    }

    @Override
    public List<V> lrange(K key, int from, int to) {
        List<String> response = send(List.of(
                "LRANGE",
                keyCodec.encode(key),
                Integer.toString(from),
                Integer.toString(to)
        ));

        if (responseParser.isNotFound(response)) {
            return List.of();
        }

        return response.stream()
                .map(valueSerializer::decode)
                .toList();
    }

    private void expectOk(List<String> command) {
        List<String> response = send(command);

        if (!responseParser.isOk(response)) {
            throw unexpectedResponse(response);
        }
    }

    private List<String> send(List<String> command) {
        try {
            List<String> response = commandClient.send(address, port, command);

            if (responseParser.isError(response)) {
                throw unexpectedResponse(response);
            }

            return response;
        } catch (IOException exception) {
            throw new CacheClientException("failed to read response from cache server", exception);
        }
    }

    private CacheClientException unexpectedResponse(List<String> response) {
        return new CacheClientException("unexpected cache server response: " + response);
    }

}
