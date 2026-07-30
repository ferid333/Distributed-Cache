package org.cache.client;

import org.cache.client.serializer.Serializer;
import org.cache.core.metrics.Snapshot;
import org.cache.network.connection.RespConnection;
import org.cache.protocol.codec.KeyCodec;
import org.cache.protocol.commands.ResponseConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.cache.protocol.commands.ResponseConstants.ERROR;
import static org.cache.protocol.commands.ResponseConstants.OK;

public class TcpCacheClient<K, V> implements CacheClient<K, V>, AutoCloseable {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 2_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5_000;

    private final RespConnection connection;
    private final KeyCodec<K> keyCodec;
    private final Serializer<V> valueSerializer;


    public TcpCacheClient(String address, int port, KeyCodec<K> keyCodec, Serializer<V> valueSerializer) {
        this(address, port, keyCodec, valueSerializer, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    public TcpCacheClient(
            String address,
            int port,
            KeyCodec<K> keyCodec,
            Serializer<V> valueSerializer,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) {
        try {
            this.connection = new RespConnection(connect(address, port, connectTimeoutMillis, readTimeoutMillis));
            this.keyCodec = keyCodec;
            this.valueSerializer = valueSerializer;
        } catch (IOException exception) {
            throw new CacheClientException("failed to connect to cache server", exception);
        }
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

        if (isResponse(response, ResponseConstants.NOT_FOUND.name(), 1)) {
            return Optional.empty();
        }

        if (isResponse(response, ResponseConstants.VALUE.name(), 2)) {
            return Optional.of(valueSerializer.decode(response.get(1)));
        }

        throw new CacheClientException("unexpected cache server response: " + response);
    }

    @Override
    public void delete(K key) {
        expectOk(List.of("DELETE", keyCodec.encode(key)));
    }

    @Override
    public Snapshot metrics() {
        List<String> response = send(List.of("METRICS"));

        if (response.isEmpty() || !response.get(0).equals(ResponseConstants.METRICS.name())) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }

        long hits = 0;
        long misses = 0;
        long evictions = 0;
        long expirations = 0;
        double hitRate = 0;

        if ((response.size() - 1) % 2 != 0) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }

        for (int i = 1; i < response.size(); i += 2) {
            String name = response.get(i);
            String value = response.get(i + 1);

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

        if (!isResponse(response, ResponseConstants.SIZE.name(), 2)) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }

        return Integer.parseInt(response.get(1));
    }

    @Override
    public void clear() {
        expectOk(List.of("CLEAR"));
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (IOException exception) {
            throw new CacheClientException("failed to close cache client", exception);
        }
    }

    private void expectOk(List<String> command) {
        List<String> response = send(command);

        if (!isResponse(response, OK.name(), 1)) {
            throw new CacheClientException("unexpected cache server response: " + response);
        }
    }

    private List<String> send(List<String> command) {
        try {
            List<String> response = connection.sendCommandForResponse(command);

            if (!response.isEmpty() && response.getFirst().equals(ERROR.name())) {
                throw new CacheClientException("unexpected cache server response: " + response);
            }

            return response;
        } catch (IOException exception) {
            throw new CacheClientException("failed to read response from cache server", exception);
        }
    }

    private boolean isResponse(List<String> response, String type, int size) {
        return response.size() == size && response.getFirst().equals(type);
    }

    private static Socket connect(
            String address,
            int port,
            int connectTimeoutMillis,
            int readTimeoutMillis
    ) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);
            return socket;
        } catch (IOException exception) {
            socket.close();
            throw exception;
        }
    }
}
