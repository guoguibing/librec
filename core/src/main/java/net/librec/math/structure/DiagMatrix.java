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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public class DiagMatrix extends SparseMatrix {
    private static final long serialVersionUID = -9186836460633909994L;

    public DiagMatrix(int rows, int cols, Table<Integer, Integer, Double> dataTable, Multimap<Integer, Integer> colMap) {
        super(rows, cols, dataTable, colMap);
    }

    public DiagMatrix(DiagMatrix mat) {
        super(mat);
    }

    public DiagMatrix clone() {
        return new DiagMatrix(this);
    }

    /**
     * Return a new diagonal matrix by scaling the current diagonal matrix.
     *
     * @param val a given value for scaling
     * @return a new diagonal matrix by scaling the current diagonal matrix
     */
    public DiagMatrix scale(double val) {
        DiagMatrix res = this.clone();
        for (int i = 0; i < res.numRows; i++)
            res.set(i, i, this.get(i, i) * val);

        return res;
    }

    /**
     * Return this diagonal matrix by scaling the current diagonal matrix.
     *
     * @param val a given value for scaling
     * @return this diagonal matrix by scaling the current diagonal matrix
     */
    public DiagMatrix scaleEqual(double val) {
        for (int i = 0; i < numRows; i++)
            set(i, i, get(i, i) * val);

        return this;
    }

    /**
     * do {@code B + C} diagonal matrix operation
     *
     * @param that a value to be added
     * @return a new diagonal matrix {@code A = B + C}
     */
    public DiagMatrix add(DiagMatrix that) {
        assert this.size() == that.size();

        DiagMatrix res = this.clone();
        for (int i = 0; i < res.numRows; i++)
            res.set(i, i, this.get(i, i) + that.get(i, i));

        return res;
    }

    /**
     * do {@code B + C} diagonal matrix operation
     *
     * @param that a value to be added
     * @return this diagonal matrix {@code B = B + C}
     */
    public DiagMatrix addEqual(DiagMatrix that) {
        assert this.size() == that.size();

        for (int i = 0; i < numRows; i++)
            set(i, i, get(i, i) + that.get(i, i));

        return this;
    }

    /**
     * Each diagonal entry addes {@code val}
     *
     * @param val a value to be added
     * @return a new diagonal matrix {@code A = B + c}
     */
    public DiagMatrix add(double val) {

        DiagMatrix res = this.clone();
        for (int i = 0; i < res.numRows; i++)
            res.set(i, i, this.get(i, i) + val);

        return res;
    }

    /**
     * Each diagonal entry addes {@code val}
     *
     * @param val a value to be added
     * @return this diagonal matrix {@code B = B + c}
     */
    public DiagMatrix addEqual(double val) {
        for (int i = 0; i < numRows; i++)
            set(i, i, get(i, i) + val);

        return this;
    }

    /**
     * Do {@code B - C} diagonal matrix operation
     *
     * @param that a value to be abstracted
     * @return a new diagonal matrix {@code A = B - C}
     */
    public DiagMatrix minus(DiagMatrix that) {
        assert this.size() == that.size();

        DiagMatrix res = this.clone();
        for (int i = 0; i < res.numRows; i++)
            res.set(i, i, this.get(i, i) - that.get(i, i));

        return res;
    }

    /**
     * Do {@code B - C} diagonal matrix operation
     *
     * @param that a value to be abstracted
     * @return this diagonal matrix {@code B = B - C}
     */
    public DiagMatrix minusEqual(DiagMatrix that) {
        assert this.size() == that.size();

        for (int i = 0; i < numRows; i++)
            set(i, i, get(i, i) - that.get(i, i));

        return this;
    }

    /**
     * Each diagonal entry abstracts {@code val}
     *
     * @param val a value to be abstracted
     * @return a new diagonal matrix {@code A = B - c}
     */
    public DiagMatrix minus(double val) {
        DiagMatrix res = this.clone();
        for (int i = 0; i < res.numRows; i++)
            res.set(i, i, this.get(i, i) - val);

        return res;
    }

    /**
     * Each diagonal entry abstracts {@code val}
     *
     * @param val a value to be abstracted
     * @return this diagonal matrix {@code B = B - c}
     */
    public DiagMatrix minusEqual(double val) {
        for (int i = 0; i < numRows; i++)
            set(i, i, get(i, i) - val);

        return this;
    }

    public static DiagMatrix eye(int n) {
        Table<Integer, Integer, Double> vals = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();
        for (int i = 0; i < n; i++) {
            vals.put(i, i, 1.0);
            colMap.put(i, i);
        }

        return new DiagMatrix(n, n, vals, colMap);
    }
}
