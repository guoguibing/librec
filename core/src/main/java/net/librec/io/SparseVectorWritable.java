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

import net.librec.math.structure.SparseVector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Dense Vector Writable
 *
 * @author WangYuFeng
 */
public class SparseVectorWritable implements Writable {

    /** The value of this <code>SparseVectorWritable</code> */
    private SparseVector value;

    /**
     * Empty constructor.
     */
    public SparseVectorWritable() {
    }

    /**
     * Construct from a <code>SparseVector</code> object.
     *
     * @param sparseVector  an object for construction
     */
    public SparseVectorWritable(SparseVector sparseVector) {
        this.value = sparseVector;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        if (value != null && value.getData().length > 0) {
            out.writeInt(value.getData().length);
            for (int i = 0; i < value.getData().length; i++) {
                out.writeInt(i);
                out.writeDouble(value.getData()[i]);
            }
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        int dataLength = in.readInt();
        if (dataLength > 0) {
            value = new SparseVector(dataLength);
            for (int i = 0; i < dataLength; i++) {
                value.add(in.readInt(), in.readDouble());
            }
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#getValue()
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#setValue(java.lang.Object)
     */
    public void setValue(Object value) {
        this.value = (SparseVector) value;
    }

}
