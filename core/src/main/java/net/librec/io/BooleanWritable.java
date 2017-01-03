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
public class BooleanWritable implements WritableComparable {

    /**
     * the value of BooleanWritable.
     */
    private boolean value;

    /**
     * Empty constructor.
     */
    public BooleanWritable() {
    }

    /**
     * Construct with the value of BooleanWritable
     *
     * @param value the value of BooleanWritable
     */
    public BooleanWritable(boolean value) {
        set(value);
    }

    /**
     * Set the value of BooleanWritable
     *
     * @param value the value to set
     */
    public void set(boolean value) {
        this.value = value;
    }

    /**
     * Return the value of BooleanWritable.
     *
     * @return the value of BooleanWritable
     */
    public boolean get() {
        return value;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        value = in.readBoolean();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BooleanWritable)) {
            return false;
        }
        BooleanWritable other = (BooleanWritable) o;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return value ? 0 : 1;
    }

    @Override
    public int compareTo(Object o) {
        boolean a = this.value;
        boolean b = ((BooleanWritable) o).value;
        return ((a == b) ? 0 : (a == false) ? -1 : 1);
    }

    @Override
    public String toString() {
        return Boolean.toString(get());
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (boolean) value;
    }
}
