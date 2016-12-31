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
package net.librec.data.convertor;

import net.librec.data.DataConvertor;
import net.librec.job.progress.ProgressReporter;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseTensor;

/**
 * A <tt>AbstractDataConvertor</tt> is a class to convert
 * a data file from one source format to a target format.
 *
 * @author WangYuFeng
 */
public abstract class AbstractDataConvertor extends ProgressReporter implements DataConvertor {

    /** store rate data as {user, item, rate} matrix */
    protected SparseMatrix preferenceMatrix;

    /** store time data as {user, item, rate} matrix */
    protected SparseMatrix datetimeMatrix;

    /** store rate data as a sparse tensor */
    protected SparseTensor sparseTensor;

    /**
     * Return the rate matrix.
     *
     * @return  {@link #preferenceMatrix}
     */
    public SparseMatrix getPreferenceMatrix() {
        return preferenceMatrix;
    }

    /**
     * Return the date matrix.
     *
     * @return  {@link #datetimeMatrix}
     */
    public SparseMatrix getDatetimeMatrix() {
        return datetimeMatrix;
    }

    /**
     * Return the rate tensor.
     *
     * @return  {@link #sparseTensor}
     */
    public SparseTensor getSparseTensor() {
        return sparseTensor;
    }

}
