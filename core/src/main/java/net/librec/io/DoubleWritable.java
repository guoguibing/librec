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

@SuppressWarnings("rawtypes")
public class DoubleWritable implements WritableComparable {

    /** value of the DoubleWritable */
    private double value = 0.0;

    /**
     * Empty constructor
     */
    public DoubleWritable() {

    }

    /**
     * Create a new object with the given value
     *
     * @param value a given value
     */
    public DoubleWritable(double value) {
        set(value);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        value = in.readDouble();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(value);
    }

    /**
     * Set the value of DoubleWritable
     *
     * @param value the value to set
     */
    public void set(double value) {
        this.value = value;
    }

    /**
     * Get the value of DoubleWritable
     *
     * @return the value of DoubleWritable
     */
    public double get() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DoubleWritable)) {
            return false;
        }
        DoubleWritable other = (DoubleWritable) o;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return (int) Double.doubleToLongBits(value);
    }

    @Override
    public int compareTo(Object o) {
        DoubleWritable other = (DoubleWritable) o;
        return (value < other.value ? -1 : (value == other.value ? 0 : 1));
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (double) value;
    }


}
