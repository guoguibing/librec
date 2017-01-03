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
public class NullWritable implements WritableComparable {

    /** the single instance of this class. */
    private static final NullWritable THIS = new NullWritable();

    /**
     * Empty constructor
     */
    private NullWritable() {
    } // no public ctor

    /**
     * Returns the single instance of this class.
     *
     * @return the single instance of this class.
     */
    public static NullWritable get() {
        return THIS;
    }

    @Override
    public String toString() {
        return "(null)";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public int compareTo(Object other) {
        if (!(other instanceof NullWritable)) {
            throw new ClassCastException("can't compare "
                    + other.getClass().getName() + " to NullWritable");
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NullWritable;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
    }

    @Override
    public void write(DataOutput out) throws IOException {
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {
    }
}
