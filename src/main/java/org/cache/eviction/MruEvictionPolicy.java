package org.cache.eviction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MruEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Node<K> right;
    private final Node<K> left;
    private final Map<K, Node<K>> keyMap;

    public MruEvictionPolicy() {
        this.keyMap = new HashMap<>();
        this.right = new Node<>(null);
        this.left = new Node<>(null);

        right.prev = left;
        left.next = right;
    }

    @Override
    public void onKeyAdded(K key) {

        if (keyMap.containsKey(key)) {
            onKeyRemoved(key);
        }

        var newNode = new Node<>(key);
        var prevNode = right.prev;

        prevNode.next = newNode;
        newNode.next = right;
        newNode.prev = prevNode;
        right.prev = newNode;

        keyMap.put(key, newNode);
    }

    @Override
    public void onKeyAccessed(K key) {
        onKeyRemoved(key);
        onKeyAdded(key);
    }

    @Override
    public void onKeyRemoved(K key) {
        var removedNode = keyMap.get(key);
        if (removedNode == null) return;

        var prevNode = removedNode.prev;
        var nextNode = removedNode.next;

        nextNode.prev = prevNode;
        prevNode.next = nextNode;

        keyMap.remove(key);
    }

    @Override
    public Optional<K> selectVictim() {
        if (keyMap.isEmpty()) {
            return Optional.empty();
        }

        var selectedNode = right.prev;

        return Optional.ofNullable(selectedNode.value);
    }
}