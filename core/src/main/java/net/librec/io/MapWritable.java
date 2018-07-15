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
package net.librec.io;

import net.librec.util.ReflectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapWritable extends AbstractMapWritable implements Map<Writable, Writable> {

    private Map<Writable, Writable> instance;

    public MapWritable() {
        super();
        this.instance = new HashMap<Writable, Writable>();
    }

    public MapWritable(MapWritable other) {
        this();
        copy(other);
    }

    @Override
    public void clear() {
        instance.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return instance.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return instance.containsValue(value);
    }

    @Override
    public Set<Map.Entry<Writable, Writable>> entrySet() {
        return instance.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MapWritable) {
            Map map = (Map) obj;
            if (size() != map.size()) {
                return false;
            }

            return entrySet().equals(map.entrySet());
        }

        return false;
    }

    @Override
    public Writable get(Object key) {
        return instance.get(key);
    }

    @Override
    public int hashCode() {
        return 1 + this.instance.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return instance.isEmpty();
    }

    @Override
    public Set<Writable> keySet() {
        return instance.keySet();
    }

    @Override
    public Writable put(Writable key, Writable value) {
        addToMap(key.getClass());
        addToMap(value.getClass());
        return instance.put(key, value);
    }

    @Override
    public void putAll(Map<? extends Writable, ? extends Writable> t) {
        for (Map.Entry<? extends Writable, ? extends Writable> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Writable remove(Object key) {
        return instance.remove(key);
    }

    @Override
    public int size() {
        return instance.size();
    }

    @Override
    public Collection<Writable> values() {
        return instance.values();
    }

    // Writable

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        // Write out the number of entries in the map

        out.writeInt(instance.size());

        // Then write out each key/value pair

        for (Map.Entry<Writable, Writable> e : instance.entrySet()) {
            out.writeByte(getId(e.getKey().getClass()));
            e.getKey().write(out);
            out.writeByte(getId(e.getValue().getClass()));
            e.getValue().write(out);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);

        // First clear the map. Otherwise we will just accumulate
        // entries every time this method is called.
        this.instance.clear();

        // Read the number of entries in the map

        int entries = in.readInt();

        // Then read each key/value pair

        for (int i = 0; i < entries; i++) {
            Writable key = (Writable) ReflectionUtil.newInstance(getClass(in.readByte()));

            key.readFields(in);

            Writable value = (Writable) ReflectionUtil.newInstance(getClass(in.readByte()));

            value.readFields(in);
            instance.put(key, value);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("[");
        for (Map.Entry<Writable, Writable> entry : entrySet()) {
            sb.append("{");
            sb.append(entry.getKey().toString());
            sb.append(":");
            sb.append(entry.getValue().toString());
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object getValue() {
        return instance;
    }

    @Override
    public void setValue(Object value) {
        this.instance = (Map<Writable, Writable>) value;
    }
}
