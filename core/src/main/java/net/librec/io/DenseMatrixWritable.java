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

import net.librec.math.structure.DenseMatrix;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Dense Matrix Writable
 *
 * @author WangYuFeng
 */
public class DenseMatrixWritable implements Writable {

    private DenseMatrix value;

    public DenseMatrixWritable() {
    }

    public DenseMatrixWritable(DenseMatrix denseMatrix) {
        this.value = denseMatrix;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        if (value != null && value.data.length > 0) {
            out.writeInt(value.numRows);
            out.writeInt(value.numColumns);
            for (int i = 0; i < value.data.length; i++) {
                for (int j = 0; j < value.data[i].length; j++) {
                    out.writeInt(i);
                    out.writeInt(j);
                    out.writeDouble(value.data[i][j]);
                }
            }
        }
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int numRows = in.readInt();
        if (numRows > 0) {
            int numColumns = in.readInt();
            if (numColumns > 0) {
                value = new DenseMatrix(numRows, numColumns);
                for (int i = 0; i < numRows; i++) {
                    for (int j = 0; j < numColumns; j++) {
                        int rowIdx = in.readInt();
                        int columnIdx = in.readInt();
                        value.data[rowIdx][columnIdx] = in.readDouble();
                    }
                }
            }
        }
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = (DenseMatrix) value;
    }

}
