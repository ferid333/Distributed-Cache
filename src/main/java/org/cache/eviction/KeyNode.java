package org.cache.eviction;

public class KeyNode<T> {
    public T value;
    public KeyNode<T> next;
    public KeyNode<T> prev;

    public KeyNode(T value) {
        this.value = value;
    }
}
