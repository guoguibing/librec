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

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseMatrix;

/**
 * A <tt>DataSplitter</tt> is an interface to split
 * input data.
 *
 * @author WangYuFeng
 */
public interface DataSplitter {

    /**
     * The types of the splitter.
     */
    enum SplitterType {
        GENERIC, GIEVNN, RATIO, VALIDATION
    }

    /**
     * Split the data.
     *
     * @throws LibrecException
     *         if error occurs during splitting
     */
    public void splitData() throws LibrecException;

    /**
     * Set the data convertor of this splitter.
     *
     * @param dataConvertor
     *        a data convertor for this splitter.
     */
    public void setDataConvertor(DataConvertor dataConvertor);

    /**
     * Get train data.
     *
     * @return  a {@code SparseMatrix} object built by the train set.
     */
    public SparseMatrix getTrainData();

    /**
     * Get test data.
     *
     * @return  a {@code SparseMatrix} object built by the test set.
     */
    public SparseMatrix getTestData();

    /**
     * Get valid data.
     *
     * @return  a {@code SparseMatrix} object built by the valid set.
     */
    public SparseMatrix getValidData();
}
