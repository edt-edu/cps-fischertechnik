package io.github.mbdo.factoryscada.utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that represents a map with two keys.
 * It allows the storage and retrieval of values using two keys.
 *
 * @param <K1> the type of the first key
 * @param <K2> the type of the second key
 * @param <V>  the type of the value
 */
public class MultiKeyMap<K1, K2, V> {

    private final Map<K1, Map<K2, V>> map;

    /**
     * Constructs an empty MultiKeyMap.
     */
    public MultiKeyMap() {
        map = new HashMap<>();
    }

    /**
     * Puts a value into the map with the specified keys.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @param value the value to be put in the map
     */
    public void put(K1 key1, K2 key2, V value) {
        map.computeIfAbsent(key1, k -> new HashMap<>()).put(key2, value);
    }

    /**
     * Retrieves the value associated with the specified keys.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return the value associated with the keys, or null if the keys are not present
     */
    public V get(K1 key1, K2 key2) {
        Map<K2, V> innerMap = map.get(key1);
        return innerMap == null ? null : innerMap.get(key2);
    }

    /**
     * Checks if the map contains the specified keys.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return true if the map contains the keys, false otherwise
     */
    public boolean containsKeys(K1 key1, K2 key2) {
        return map.containsKey(key1) && map.get(key1).containsKey(key2);
    }

    /**
     * Removes the value associated with the specified keys.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return the removed value, or null if the keys are not present
     */
    public V remove(K1 key1, K2 key2) {
        Map<K2, V> innerMap = map.get(key1);
        if (innerMap == null) {
            return null;
        }
        V removedValue = innerMap.remove(key2);
        if (innerMap.isEmpty()) {
            map.remove(key1);
        }
        return removedValue;
    }

    /**
     * Returns the total number of key-value mappings in the map.
     *
     * @return the number of key-value mappings in the map
     */
    public int size() {
        int size = 0;
        for (Map<K2, V> innerMap : map.values()) {
            size += innerMap.size();
        }
        return size;
    }

    /**
     * Clears all the key-value mappings in the map.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns a string representation of the map.
     *
     * @return a string representation of the map
     */
    @Override
    public String toString() {
        return map.toString();
    }

}
