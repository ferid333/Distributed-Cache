package org.cache.core;

import org.cache.core.metrics.Snapshot;
import org.cache.protocol.codec.ValueCodec;
import org.cache.protocol.codec.ValueCodecRegistry;
import org.cache.protocol.handlers.WrongValueTypeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CacheService<K> implements CacheOperations<K> {

    private final Cache<K> cache;
    private final ValueCodecRegistry valueCodecs;

    public CacheService(Cache<K> cache, ValueCodecRegistry valueCodecs) {
        this.cache = cache;
        this.valueCodecs = valueCodecs;
    }

    @SuppressWarnings("unchecked")
    public void putString(K key, String value, long ttlMillis) {
        ValueCodec<String> codec = (ValueCodec<String>) valueCodecs.get(ValueType.STRING);
        cache.put(key, codec.encode(value), ValueType.STRING, ttlMillis);
    }

    public Optional<String> getString(K key) {
        Optional<CacheEntry> entry = cache.get(key);

        if (entry.isEmpty()) {
            return Optional.empty();
        }

        CacheEntry cacheEntry = entry.get();
        if (cacheEntry.getType() != ValueType.STRING) {
            throw new WrongValueTypeException(ValueType.STRING, cacheEntry.getType());
        }

        return Optional.of(valueCodecs.get(ValueType.STRING).toString(cacheEntry.getValue()));
    }

    @SuppressWarnings("unchecked")
    public void push(K key, String value) {
        Optional<CacheEntry> existingList = cache.get(key);
        ValueCodec<List<String>> listCodec = (ValueCodec<List<String>>) valueCodecs.get(ValueType.LIST);

        List<String> list;

        if (existingList.isEmpty()) {
            list = new ArrayList<>();
        } else {
            CacheEntry entry = existingList.get();
            if (entry.getType() != ValueType.LIST) {
                throw new WrongValueTypeException(ValueType.LIST, entry.getType());
            }

            list = listCodec.decode(entry.getValue());
        }

        list.add(value);
        cache.put(key, listCodec.encode(list), ValueType.LIST, 0);
    }

    @SuppressWarnings("unchecked")
    public Optional<List<String>> lrange(K key, int from, int to) {
        if (from < 0 || to < from) {
            throw new IllegalArgumentException("from must be >= 0 and to must be >= from");
        }

        Optional<CacheEntry> entry = cache.get(key);

        if (entry.isEmpty()) {
            return Optional.empty();
        }

        CacheEntry cacheEntry = entry.get();
        if (cacheEntry.getType() != ValueType.LIST) {
            throw new WrongValueTypeException(ValueType.LIST, cacheEntry.getType());
        }

        ValueCodec<List<String>> listCodec = (ValueCodec<List<String>>) valueCodecs.get(ValueType.LIST);
        List<String> list = listCodec.decode(cacheEntry.getValue());

        if (from >= list.size()) {
            return Optional.of(List.of());
        }

        int boundedTo = Math.min(to, list.size());
        return Optional.of(List.copyOf(list.subList(from, boundedTo)));
    }

    public void delete(K key) {
        cache.delete(key);
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    public Snapshot metrics() {
        return cache.metrics();
    }
}
