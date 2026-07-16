package org.cache.eviction;

import java.util.Optional;

public interface EvictionPolicy<K> {

    void onKeyAdded(K key);

    void onKeyAccessed(K key);

    void onKeyRemoved(K key);

    Optional<K> selectVictim();
}
