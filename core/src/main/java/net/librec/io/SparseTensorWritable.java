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

import net.librec.math.structure.SparseTensor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sparse Tensor Writable
 *
 * @author WangYuFeng
 */
public class SparseTensorWritable implements Writable {
    /**
     * The value of this SparseTensorWritable
     */
    private SparseTensor value;

    /**
     * Construct from a <code>SparseTensor</code> object.
     *
     * @param sparseTensor  an object for construction
     */
    public SparseTensorWritable(SparseTensor sparseTensor) {
        this.value = sparseTensor;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        if (value != null && value.numDimensions > 0) {
            out.writeInt(value.numDimensions);
            // output value.dimensions
            out.writeInt(value.dimensions.length);
            if (value.dimensions != null && value.dimensions.length > 0) {
                for (int i = 0; i < value.dimensions.length; i++) {
                    out.writeInt(value.dimensions[i]);
                }
            }
            // output value.ndKeys
            out.writeInt(value.ndKeys.length);
            if (value.ndKeys != null && value.ndKeys.length > 0) {
                for (List<Integer> ndKeyList : value.ndKeys) {
                    out.writeInt(ndKeyList.size());
                    for (Integer ndKey : ndKeyList) {
                        out.writeInt(ndKey);
                    }
                }
            }
            // output value.values
            out.writeInt(value.values.size());
            if (value.values != null && value.values.size() > 0) {
                for (Double val : value.values) {
                    out.writeDouble(val);
                }
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
        int numDimensions = in.readInt();
        if (numDimensions > 0) {
            // read value.dimensions
            int dimensionsLength = in.readInt();
            if (dimensionsLength > 0) {
                value.dimensions = new int[dimensionsLength];
                for (int i = 0; i < dimensionsLength; i++) {
                    value.dimensions[i] = in.readInt();
                }
            }
            // read value.ndKeys
            int ndKeysLength = in.readInt();
            if (ndKeysLength > 0) {
                value.ndKeys = (List<Integer>[]) new ArrayList<?>[ndKeysLength];
                for (int i = 0; i < ndKeysLength; i++) {
                    value.ndKeys[i] = new ArrayList<Integer>();
                    int ndKeysSize = in.readInt();
                    if (ndKeysSize > 0) {
                        for (int j = 0; j < ndKeysSize; j++) {
                            value.ndKeys[i].add(in.readInt());
                        }
                    }
                }
            }
            // read value.values
            int valuesLength = in.readInt();
            if (valuesLength > 0) {
                value.values = new ArrayList<Double>();
                for (int i = 0; i < valuesLength; i++) {
                    value.values.add(in.readDouble());
                }
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
    @Override
    public void setValue(Object value) {
        this.value = (SparseTensor) value;
    }

}
