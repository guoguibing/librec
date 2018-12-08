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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.util.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * K-fold Cross Validation Data Splitter
 *
 * @author WangYuFeng, Liuxz and Keqiang Wang
 */
public class KCVDataSplitter extends AbstractDataSplitter {

    /**
     * The rate dataset for splitting
     */
//    private SparseMatrix preferenceMatrix;

    /**
     * The assign matrix for k-fold splitting
     */
    private SequentialAccessSparseMatrix assignMatrix;

    /**
     * The number of folds
     */
    private int cvNumber;

    /**
     * The index of current fold
     */
    private int cvIndex;

    /**
     * Empty constructor.
     */
    public KCVDataSplitter() {
    }

    /**
     * Initializes a newly created {@code KCVDataSplitter} object
     * with convertor and configuration.
     *
     * @param dataConvertor the convertor for the splitter.
     * @param conf          the configuration for the splitter.
     */
    public KCVDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /**
     * Split the data into k folds.
     *
     * @param kFold the number of folds.
     */
    public void splitData(int kFold) {
        //  prepare preferenceMatrix
        if (null == this.preferenceMatrix) {
//            if (Objects.equals(conf.get("data.convert.columns"), null)) {
//                this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
//            } else {
//                this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf.get("data.convert.columns").split(","));
//            }
            this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf);
//            this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
            if ((!(dataConvertor instanceof ArffDataConvertor))
                    &&(StringUtils.equals(conf.get("data.column.format"), "UIRT"))) {
                this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
            }
        }

        if (kFold > 0) {
            assignMatrix = preferenceMatrix.clone();
            int numRates = preferenceMatrix.getNumEntries();
            int numFold = kFold > numRates ? numRates : kFold;

            // divide rating data into kfold sample of equal size
            List<Map.Entry<Integer, Double>> rdm = new ArrayList<>(numRates);
            double indvCount = (numRates + 0.0) / numFold;
            for (int index = 0; index < numRates; index++) {
                rdm.add(new AbstractMap.SimpleImmutableEntry<>((int) (index / indvCount) + 1, Randoms.uniform()));
            }

            int[] fold = new int[numRates];
            Lists.sortList(rdm, true);
            for (int index = 0; index < numRates; index++) {
                fold[index] = rdm.get(index).getKey();
            }

            int i = 0;
            for (MatrixEntry matrixEntry : assignMatrix) {
                assignMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), fold[i++]);
            }
        }

        if (null == assignMatrixList) {
            List<Table<Integer, Integer, Integer>> tableList = new ArrayList<>(kFold + 1);
            for (int i = 0; i < kFold + 1; i++) {
                tableList.add(HashBasedTable.create());
            }
            for (MatrixEntry me : assignMatrix) {
                if (me.get() != 0) {
                    tableList.get((int) me.get()).put(me.row(), me.column(), 1);
                }
            }

            this.assignMatrixList = new LinkedList<>();
            for (int i = 1; i < kFold + 1; i++) {
                this.assignMatrixList.add(new SequentialAccessSparseMatrix(assignMatrix.rowSize(), assignMatrix.columnSize(), tableList.get(i)));
            }
        }
    }

    /**
     * Split the data.
     *
     * @throws LibrecException if error occurs
     */
    @Override
    public void splitData() throws LibrecException {
        this.cvNumber = conf.getInt("data.splitter.cv.number", 5);
        if (null == assignMatrix) {
            splitData(this.cvNumber);
        }
    }

    public List<SequentialAccessSparseMatrix> getAssignMatrixList() {
        return this.assignMatrixList;
    }
}
