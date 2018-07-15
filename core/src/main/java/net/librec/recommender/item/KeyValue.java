package net.librec.recommender.item;

import java.util.Map;
import java.util.Objects;


/**
 * Hashtable bucket collision list entry
 * @author Keqiang Wang
 */
public class KeyValue<K, V> implements Map.Entry<K, V> {
    K key;
    V value;

    public KeyValue(K key, V value) {
        super();
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        if (value == null)
            throw new NullPointerException();

        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }


    public boolean equals(Object o) {
        if (!(o instanceof KeyValue))
            return false;
        KeyValue<?, ?> e = (KeyValue<?, ?>) o;
        return key.equals(e.getKey()) && value.equals(e.getValue());
    }

    public int hashCode() {
        return (Objects.hashCode(key) ^ Objects.hashCode(value));
    }

    public String toString() {
        return key.toString() + "=" + value.toString();
    }
}
