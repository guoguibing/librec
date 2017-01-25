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
package net.librec.data.splitter;

import net.librec.conf.Configured;
import net.librec.data.DataConvertor;
import net.librec.data.DataSplitter;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseTensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract Data Splitter
 *
 * @author WangYuFeng
 */
public abstract class AbstractDataSplitter extends Configured implements DataSplitter {
    /**
     * LOG
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());
    /**
     * dataConvertor
     */
    protected DataConvertor dataConvertor;
    /**
     * trainMatrix
     */
    protected SparseMatrix trainMatrix;
    /**
     * testMatrix
     */
    protected SparseMatrix testMatrix;
    /**
     * validationMatrix
     */
    protected SparseMatrix validationMatrix;

    /**
     * @param dataConvertor the dataConvertor to set
     */
    public void setDataConvertor(DataConvertor dataConvertor) {
        this.dataConvertor = dataConvertor;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getTrainDataSet()
     */
    public SparseMatrix getTrainData() {
        return trainMatrix;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getTestDataSet()
     */
    public SparseMatrix getTestData() {
        return testMatrix;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getValidDataSet()
     */
    public SparseMatrix getValidData() {
        return validationMatrix;
    }

}
