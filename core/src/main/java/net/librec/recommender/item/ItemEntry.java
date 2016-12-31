/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.item;

import java.util.Map;
import java.util.Objects;

/**
 * @author YuFeng Wang
 */

/**
 * Hashtable bucket collision list entry
 */
public class ItemEntry<K, V> implements Map.Entry<K, V> {
    K key;
    V value;

    // Map.Entry Ops

    public ItemEntry(K key, V value) {
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
        if (!(o instanceof ItemEntry))
            return false;
        ItemEntry<?, ?> e = (ItemEntry<?, ?>) o;
        return key.equals(e.getKey()) && value.equals(e.getValue());
    }

    public int hashCode() {
        return (Objects.hashCode(key) ^ Objects.hashCode(value));
    }

    public String toString() {
        return key.toString() + "=" + value.toString();
    }
}