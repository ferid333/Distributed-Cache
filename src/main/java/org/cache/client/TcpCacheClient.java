package org.cache.client;

import org.cache.core.metrics.Snapshot;
import org.cache.network.connection.RespConnection;
import org.cache.protocol.codec.Codec;
import org.cache.protocol.codec.StringCodec;
import org.cache.protocol.commands.ResponseConstants;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static org.cache.protocol.commands.ResponseConstants.ERROR;
import static org.cache.protocol.commands.ResponseConstants.OK;

public class TcpCacheClient<K, V> implements CacheClient<K, V>, AutoCloseable {

    private final RespConnection connection;
    private final Codec<K> keyCodec;
    private final Codec<V> valueCodec;

    @SuppressWarnings("unchecked")
    public TcpCacheClient(String address, int port) {
        this(
                address,
                port,
                (Codec<K>) new StringCodec(),
                (Codec<V>) new StringCodec()
        );
    }

    public TcpCacheClient(String address, int port, Codec<K> keyCodec, Codec<V> valueCodec) {
        try {
            this.connection = new RespConnection(new Socket(address, port));
            this.keyCodec = keyCodec;
            this.valueCodec = valueCodec;
        } catch (IOException exception) {
            throw new CacheClientException("failed to connect to cache server", exception);
        }
    }

    @Override
    public void put(K key, V value, long ttl) {
        expectOk(List.of("PUT", keyCodec.encode(key), valueCodec.encode(value), Long.toString(ttl)));
    }

    @Override
    public void put(K key, V value) {
        expectOk(List.of("PUT", keyCodec.encode(key), valueCodec.encode(value)));
    }

    @Override
    public Optional<V> get(K key) {
        String response = send(List.of("GET", keyCodec.encode(key)));

        if (response.equals(ResponseConstants.NOT_FOUND.name())) {
            return Optional.empty();
        }

        String prefix = ResponseConstants.VALUE.name() + " ";
        if (response.startsWith(prefix)) {
            return Optional.of(valueCodec.decode(response.substring(prefix.length())));
        }

        throw protocolException(response);
    }

    @Override
    public boolean delete(K key) {
        expectOk(List.of("DELETE", keyCodec.encode(key)));
        return true;
    }

    @Override
    public Snapshot metrics() {
        String response = send(List.of("METRICS"));
        String prefix = ResponseConstants.METRICS.name() + " ";

        if (!response.startsWith(prefix)) {
            throw protocolException(response);
        }

        long hits = 0;
        long misses = 0;
        long evictions = 0;
        long expirations = 0;
        double hitRate = 0;

        String[] metrics = response.substring(prefix.length()).split("\\s+");
        for (String metric : metrics) {
            String[] parts = metric.split("=", 2);
            if (parts.length != 2) {
                throw protocolException(response);
            }

            switch (parts[0]) {
                case "hits" -> hits = Long.parseLong(parts[1]);
                case "misses" -> misses = Long.parseLong(parts[1]);
                case "evictions" -> evictions = Long.parseLong(parts[1]);
                case "expirations" -> expirations = Long.parseLong(parts[1]);
                case "hitRate" -> hitRate = Double.parseDouble(parts[1]);
                default -> throw protocolException(response);
            }
        }

        return new Snapshot(hits, misses, evictions, expirations, hitRate);
    }

    @Override
    public int size() {
        String response = send(List.of("SIZE"));
        String prefix = ResponseConstants.SIZE.name() + " ";

        if (!response.startsWith(prefix)) {
            throw protocolException(response);
        }

        return Integer.parseInt(response.substring(prefix.length()));
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
        String response = send(command);

        if (!response.equals(OK.name())) {
            throw protocolException(response);
        }
    }

    private String send(List<String> command) {
        try {
            String response = connection.sendCommand(command);

            if (response.startsWith(ERROR.name() + " ")) {
                throw protocolException(response);
            }

            return response;
        } catch (IOException exception) {
            throw new CacheClientException("failed to read response from cache server", exception);
        }
    }

    private CacheClientException protocolException(String response) {
        return new CacheClientException("unexpected cache server response: " + response);
    }
}
