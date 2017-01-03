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
package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 *
 */
public class SymmMatrix {

    // matrix dimension
    protected int dim;
    // matrix data
    Table<Integer, Integer, Double> data;

    /**
     * Construct a symmetric matrix
     *
     * @param dim matrix dimension
     */
    public SymmMatrix(int dim) {
        this.dim = dim;
        data = HashBasedTable.create(); // do not specify the size here as a
        // sparse matrix
    }

    /**
     * Construct a symmetric matrix by deeply copying data from a given matrix
     *
     * @param mat a given matrix
     */
    public SymmMatrix(SymmMatrix mat) {
        dim = mat.dim;
        data = HashBasedTable.create(mat.data);
    }

    /**
     * Make a deep copy of current matrix
     */
    public SymmMatrix clone() {
        return new SymmMatrix(this);
    }

    /**
     * Get a value at entry (row, col)
     *
     * @param row row index
     * @param col column index
     * @return value at entry (row, col)
     */
    public double get(int row, int col) {

        if (data.contains(row, col))
            return data.get(row, col);
        else if (data.contains(col, row))
            return data.get(col, row);

        return 0.0;
    }

    /**
     * set a value to entry (row, col)
     *
     * @param row row index
     * @param col column index
     * @param val value to set
     */
    public void set(int row, int col, double val) {
        if (row >= col)
            data.put(row, col, val);
        else
            data.put(col, row, val);
    }

    /**
     * add a value to entry (row, col)
     *
     * @param row row index
     * @param col column index
     * @param val value to add
     */
    public void add(int row, int col, double val) {
        if (row >= col)
            data.put(row, col, val + get(row, col));
        else
            data.put(col, row, val + get(col, row));
    }

    /**
     * Retrieve a complete row of similar items
     *
     * @param row row index
     * @return a complete row of similar items
     */
    public SparseVector row(int row) {
        SparseVector res = new SparseVector(dim);
        for (int col = 0; col < dim; col++) {
            double val = get(row, col);
            if (val != 0)
                res.set(col, val);
        }

        return res;
    }

    /**
     * @return the dim
     */
    public int getDim() {
        return dim;
    }

    /**
     * @return the data
     */
    public Table<Integer, Integer, Double> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Dimension: " + dim + " x " + dim + "\n" + data.toString();
    }

}
