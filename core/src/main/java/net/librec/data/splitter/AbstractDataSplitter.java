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
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;

/**
 * Abstract Data Splitter
 *
 * @author WangYuFeng and Keqiang Wang
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
    protected SequentialAccessSparseMatrix trainMatrix;
    /**
     * testMatrix
     */
    protected SequentialAccessSparseMatrix testMatrix;
    /**
     * validationMatrix
     */
    protected SequentialAccessSparseMatrix validationMatrix;
    /**
     * assign matrix
     */
    protected LinkedList<SequentialAccessSparseMatrix> assignMatrixList;

    protected SequentialAccessSparseMatrix preferenceMatrix = null;
    protected SequentialAccessSparseMatrix datetimeMatrix = null;

    @Override
    public void setDataConvertor(DataConvertor dataConvertor) {
        this.dataConvertor = dataConvertor;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getTrainDataSet()
     */
    @Override
    public SequentialAccessSparseMatrix getTrainData() {
        return trainMatrix;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getTestDataSet()
     */
    @Override
    public SequentialAccessSparseMatrix getTestData() {
        return testMatrix;
    }

    /**
     * (non-Javadoc)
     *
     * @see net.librec.data.DataModel#getValidDataSet()
     */
    @Override
    public SequentialAccessSparseMatrix getValidData() {
        return validationMatrix;
    }

    public void setPreferenceMatrix(SequentialAccessSparseMatrix preferenceMatrix) {
        this.preferenceMatrix = preferenceMatrix;
    }

    @Override
    public boolean nextFold() {
        if (assignMatrixList == null) {
            assignMatrixList = new LinkedList<>();
            return true;
        } else {
            if (assignMatrixList.size() > 0) {
                SequentialAccessSparseMatrix assign = assignMatrixList.poll();
                trainMatrix = preferenceMatrix.clone();
                testMatrix = preferenceMatrix.clone();

                for (MatrixEntry matrixEntry : preferenceMatrix) {
                    if (assign.get(matrixEntry.row(), matrixEntry.column()) == 1) {
                        trainMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                    } else {
                        testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                    }
                }

                trainMatrix.reshape();
                testMatrix.reshape();
                return true;
            } else {
                return false;
            }
        }
    }
}
