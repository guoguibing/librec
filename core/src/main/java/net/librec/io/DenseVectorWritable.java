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

import net.librec.math.structure.DenseVector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Dense Vector Writable
 *
 * @author WangYuFeng
 */
public class DenseVectorWritable implements Writable {

    /** the value of the DenseVectorWritable */
    private DenseVector value;

    /**
     * Empty constructor
     */
    public DenseVectorWritable() {
    }

    /**
     * Create a new object with the given value
     *
     * @param denseVector a given value
     */
    public DenseVectorWritable(DenseVector denseVector) {
        this.value = denseVector;
    }

    /*
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

    /*
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        int dataLength = in.readInt();
        if (dataLength > 0) {
            value = new DenseVector(dataLength);
            for (int i = 0; i < dataLength; i++) {
                value.add(in.readInt(), in.readDouble());
            }
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = (DenseVector) value;
    }

}
