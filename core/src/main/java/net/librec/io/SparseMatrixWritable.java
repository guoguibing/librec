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

import net.librec.math.structure.SparseMatrix;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Dense Matrix Writable
 *
 * @author WangYuFeng
 */
public class SparseMatrixWritable implements Writable {

    /** The value of this <code>SparseMatrixWritable</code> */
    private SparseMatrix value;

    /**
     * Empty constructor.
     */
    public SparseMatrixWritable() {
    }

    /**
     * Construct from a <code>SparseMatrix</code> object.
     *
     * @param sparseMatrix  an object for construction
     */
    public SparseMatrixWritable(SparseMatrix sparseMatrix) {
        this.value = sparseMatrix;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        if (value != null && value.rowData.length > 0 && value.colData.length > 0) {
            out.writeInt(value.numRows);
            out.writeInt(value.numColumns);
            // output value.rowData
            out.writeInt(value.rowData.length);
            for (int i = 0; i < value.rowData.length; i++) {
                out.writeDouble(value.rowData[i]);
            }
            // output value.rowPtr
            out.writeInt(value.rowPtr.length);
            for (int i = 0; i < value.rowPtr.length; i++) {
                out.writeInt(value.rowPtr[i]);
            }
            // output value.colInd
            out.writeInt(value.colInd.length);
            for (int i = 0; i < value.colInd.length; i++) {
                out.writeInt(value.colInd[i]);
            }
            // output value.colData
            out.writeInt(value.colData.length);
            for (int i = 0; i < value.colData.length; i++) {
                out.writeDouble(value.colData[i]);
            }
            // output value.colPtr
            out.writeInt(value.colPtr.length);
            for (int i = 0; i < value.colPtr.length; i++) {
                out.writeInt(value.colPtr[i]);
            }
            // output value.rowInd
            out.writeInt(value.rowInd.length);
            for (int i = 0; i < value.rowInd.length; i++) {
                out.writeInt(value.rowInd[i]);
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
        int numRows = in.readInt();
        if (numRows > 0) {
            int numColumns = in.readInt();
            if (numColumns > 0) {
                value = new SparseMatrix(numRows, numColumns);
                // read rowData
                int rowDataLength = in.readInt();
                if (rowDataLength > 0) {
                    value.rowData = new double[rowDataLength];
                    for (int i = 0; i < rowDataLength; i++) {
                        value.rowData[i] = in.readDouble();
                    }
                }
                // read rowPtr
                int rowPtrLength = in.readInt();
                if (rowPtrLength > 0) {
                    value.rowPtr = new int[rowPtrLength];
                    for (int i = 0; i < rowPtrLength; i++) {
                        value.rowPtr[i] = in.readInt();
                    }
                }
                // read colInd
                int colIndLength = in.readInt();
                if (colIndLength > 0) {
                    value.colInd = new int[colIndLength];
                    for (int i = 0; i < colIndLength; i++) {
                        value.colInd[i] = in.readInt();
                    }
                }
                // read colData
                int colDataLength = in.readInt();
                if (colDataLength > 0) {
                    value.colData = new double[colDataLength];
                    for (int i = 0; i < colDataLength; i++) {
                        value.colData[i] = in.readDouble();
                    }
                }
                // read colPtr
                int colPtrLength = in.readInt();
                if (colPtrLength > 0) {
                    value.colPtr = new int[colPtrLength];
                    for (int i = 0; i < colPtrLength; i++) {
                        value.colPtr[i] = in.readInt();
                    }
                }
                // read rowInd
                int rowIndLength = in.readInt();
                if (rowIndLength > 0) {
                    value.rowInd = new int[rowIndLength];
                    for (int i = 0; i < rowIndLength; i++) {
                        value.rowInd[i] = in.readInt();
                    }
                }
            }
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#getValue()
     */
    public Object getValue() {
        return value;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.io.Writable#setValue(java.lang.Object)
     */
    public void setValue(Object value) {
        this.value = (SparseMatrix) value;
    }

}
