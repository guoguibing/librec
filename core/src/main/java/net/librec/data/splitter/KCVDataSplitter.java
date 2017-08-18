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

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.util.Lists;

import java.util.*;

/**
 * K-fold Cross Validation Data Splitter
 *
 * @author WangYuFeng and Liuxz
 */
public class KCVDataSplitter extends AbstractDataSplitter {

    /** The rate dataset for splitting */
    private SparseMatrix preferenceMatrix;

    /** The assign matrix for k-fold splitting */
    private SparseMatrix assignMatrix;

    /** The number of folds */
    private int cvNumber;

    /** The index of current fold */
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
     * @param dataConvertor
     *          the convertor for the splitter.
     * @param conf
     *          the configuration for the splitter.
     */
    public KCVDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /**
     * preserve the k-th validation as the test set and the rest as train set
     *
     * @param k the index of validation
     * @throws LibrecException if error occurs
     */
    public void splitData(int k) throws LibrecException {
        if (k > 0 || k <= cvNumber) {
            preferenceMatrix = dataConvertor.getPreferenceMatrix();

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
                SparseVector items = preferenceMatrix.row(u);
                for (int j : items.getIndex()) {
                    if (assignMatrix.get(u, j) == k)
                        trainMatrix.set(u, j, 0.0);
                    else
                        testMatrix.set(u, j, 0.0);
                }
            }
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
        }
    }

    /**
     * Assign the data into k folds.
     */
    public void splitFolds() {
        this.cvNumber = conf.getInt("data.splitter.cv.number", 5);
        if (null == assignMatrix){
        	splitFolds(this.cvNumber);
        }
    }

    /**
     * Split the data into k folds.
     *
     * @param kFold
     *          the number of folds.
     */
    public void splitFolds(int kFold) {
        this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
        if (kFold > 0) {
            assignMatrix = new SparseMatrix(preferenceMatrix);
            int numRates = preferenceMatrix.getData().length;
            int numFold = kFold > numRates ? numRates : kFold;

            // divide rating data into kfold sample of equal size
            List<Map.Entry<Integer,Double>> rdm = new ArrayList<>(numRates);
            double indvCount = (numRates + 0.0) / numFold;
            for (int i = 0; i < numRates; i++) {
                rdm.add(new AbstractMap.SimpleImmutableEntry<>((int) (i / indvCount) + 1, Randoms.uniform()));
            }

            int[] fold = new int[numRates];
            Lists.sortList(rdm,true);
            for(int index = 0; index < numRates; index ++){
                fold[index] = rdm.get(index).getKey();
            }

            int[] row_ptr = preferenceMatrix.getRowPointers();
            int[] col_idx = preferenceMatrix.getColumnIndices();

            int i = 0;
            for (int userIdx= 0, numUser = preferenceMatrix.numRows(); userIdx < numUser; userIdx++) {
                for (int idx = row_ptr[userIdx], end = row_ptr[userIdx + 1]; idx < end; idx++) {
                    int itemIdx = col_idx[idx];
                    assignMatrix.set(userIdx, itemIdx, fold[i++]);
                }
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
        this.cvIndex = conf.getInt("data.splitter.cv.index");
        splitData(this.cvIndex);
    }


}
