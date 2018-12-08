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
package net.librec.data;

import net.librec.conf.Configuration;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SparseTensor;

import java.io.IOException;

/**
 * A <tt>DataConvertor</tt> is an interface to convert
 * a data file from one source format to a target format.
 *
 * @author WangYuFeng
 */
public interface DataConvertor {

    /**
     * Process the input data.
     *
     * @throws IOException if the path is not valid
     */
    void processData() throws IOException;

    /**
     * Returns a {@code SparseMatrix} object which stores rate data.
     *
     * @return a {@code SparseMatrix} object which stores rate data.
     */
    DataFrame getMatrix();
    SequentialAccessSparseMatrix getPreferenceMatrix();
    //    SequentialAccessSparseMatrix getPreferenceMatrix(String[] columns);
    SequentialAccessSparseMatrix getPreferenceMatrix(Configuration conf);
    SequentialAccessSparseMatrix getDatetimeMatrix();
    SparseTensor getSparseTensor();
    SparseTensor getSparseTensor(String[] indicesColumn, String valueColumn);
}
