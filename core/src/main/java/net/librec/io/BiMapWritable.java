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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * BiMap Writable
 *
 * @author WangYuFeng
 */
public class BiMapWritable extends MapWritable {

    private BiMap<String, Integer> value;

    /**
     * Empty constructor.
     */
    public BiMapWritable() {
    }

    /**
     * Construct with the value of BiMapWritable.
     *
     * @param value the value of BiMapWritable
     */
    public BiMapWritable(BiMap<String, Integer> value) {
        super();
        this.value = value;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        if (value != null && value.size() > 0) {
            out.writeInt(value.keySet().size());
            out.writeByte(WritableEnum.getWritableEnum(String.class).getValue());
            out.writeByte(WritableEnum.getWritableEnum(IntWritable.class).getValue());
            for (Map.Entry<String, Integer> entry : value.entrySet()) {
                if (entry.getKey() != null) {
                    out.writeUTF(entry.getKey());
                    out.writeInt(entry.getValue());
                }
            }
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int size = in.readInt();
        if (size > 0) {
            value = HashBiMap.create();
            in.readByte();
            in.readByte();
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                int val = in.readInt();
                value.put(key, val);
            }
        }
    }


    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (BiMap<String, Integer>) value;
    }
}
