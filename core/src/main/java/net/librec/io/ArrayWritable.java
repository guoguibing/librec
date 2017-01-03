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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * A Writable for arrays containing instances of a class. The elements of this
 * writable must all be instances of the same class. If this writable will be
 * the input to predict, you will need to create a subclass that sets the
 * value to be of the proper type.
 * <p>
 * For example: <code>
 * public class IntArrayWritable extends ArrayWritable {
 * public IntArrayWritable() {
 * super(IntWritable.class);
 * }
 * }
 * </code>
 */
public class ArrayWritable implements Writable {

    /** class of the values */
    private Class<? extends Writable> valueClass;

    /** values of the ArrayWritable */
    private Writable[] values;

    /**
     * Construct with the class of the values.
     *
     * @param valueClass the class of the values
     */
    public ArrayWritable(Class<? extends Writable> valueClass) {
        if (valueClass == null) {
            throw new IllegalArgumentException("null valueClass");
        }
        this.valueClass = valueClass;
    }


    /**
     * Construct with the class of the values and the values.
     *
     * @param valueClass the class of the values
     * @param values     the values to set
     */
    public ArrayWritable(Class<? extends Writable> valueClass, Writable[] values) {
        this(valueClass);
        this.values = values;
    }

    /**
     * Returns the value class of the ArrayWritable.
     *
     * @return the value class of the ArrayWritable
     */
    public Class getValueClass() {
        return valueClass;
    }

    /**
     * Convert the values to a array of strings.
     *
     * @return a array of strings.
     */
    public String[] toStrings() {
        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = values[i].toString();
        }
        return strings;
    }

    /**
     * Convert the values to a array.
     *
     * @return a array of values
     */
    public Object toArray() {
        Object result = Array.newInstance(valueClass, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(result, i, values[i]);
        }
        return result;
    }

    /**
     * Set the values of ArrayWritable.
     *
     * @param values the values to set
     */
    public void set(Writable[] values) {
        this.values = values;
    }

    /**
     * Get the values of ArrayWritable.
     *
     * @return the values of ArrayWritable
     */
    public Writable[] get() {
        return values;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        values = new Writable[in.readInt()]; // construct values
        for (int i = 0; i < values.length; i++) {
            Writable value = WritableFactories.newInstance(valueClass);
            value.readFields(in); // read a value
            values[i] = value; // store it in values
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(values.length); // write values
        for (int i = 0; i < values.length; i++) {
            values[i].write(out);
        }
    }

    @Override
    public Object getValue() {
        return values;
    }

    @Override
    public void setValue(Object value) {
        this.values = (Writable[]) value;
    }
}
