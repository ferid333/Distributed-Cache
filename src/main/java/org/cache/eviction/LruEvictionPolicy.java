package org.cache.eviction;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LruEvictionPolicy<K> implements EvictionPolicy<K> {

    private final KeyNode<K> right;
    private final KeyNode<K> left;
    private final Map<K, KeyNode<K>> keyMap;

    public LruEvictionPolicy() {
        this.keyMap = new ConcurrentHashMap<>();
        this.right = new KeyNode<>(null);
        this.left = new KeyNode<>(null);

        right.prev = left;
        left.next = right;
    }

    @Override
    public synchronized void onKeyAdded(K key) {

        if (keyMap.containsKey(key)) {
            onKeyRemoved(key);
        }

        var newNode = new KeyNode<>(key);
        var prevNode = right.prev;

        prevNode.next = newNode;
        newNode.next = right;
        newNode.prev = prevNode;
        right.prev = newNode;

        keyMap.put(key, newNode);
    }

    @Override
    public synchronized void onKeyAccessed(K key) {
        onKeyRemoved(key);
        onKeyAdded(key);
    }

    @Override
    public synchronized void onKeyRemoved(K key) {
        var removedNode = keyMap.get(key);
        if (removedNode == null) {
            return;
        }

        var prevNode = removedNode.prev;
        var nextNode = removedNode.next;

        nextNode.prev = prevNode;
        prevNode.next = nextNode;

        keyMap.remove(key);
    }

    @Override
    public synchronized Optional<K> selectVictim() {
        if (keyMap.isEmpty()) {
            return Optional.empty();
        }

        var selectedNode = left.next;

        return Optional.ofNullable(selectedNode.value);
    }
}
