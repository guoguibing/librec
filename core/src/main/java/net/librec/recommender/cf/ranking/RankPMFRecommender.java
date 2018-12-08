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
package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Date;

/**
 * The probabilistic matrix factorization (PMF) used in
 * <strong>Collaborative Deep Learning for Recommender Systems</strong>, KDD, 2015.
 * <strong>Collaborative Variational Autoencoder for Recommender Systems</strong>, KDD, 2017.
 *
 * @author Chen Ma
 */
public class RankPMFRecommender extends MatrixFactorizationRecommender {
    private double aMinusb;
    private SequentialAccessSparseMatrix preferenceMatrix;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        aMinusb = conf.getFloat("rec.confidence.a") -  conf.getFloat("rec.confidence.b");
        preferenceMatrix = new SequentialAccessSparseMatrix(trainMatrix);
        for (MatrixEntry matrixEntry : preferenceMatrix) {
            matrixEntry.set(1.0);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        DenseMatrix userIdentityMatrix = BuildEyeMatrix(numFactors).times(regUser);
        DenseMatrix itemIdentityMatrix = BuildEyeMatrix(numFactors).times(regItem);

        // To be consistent with the symbols in the paper
        DenseMatrix X = userFactors, Y = itemFactors;
        // Updating by using alternative least square (ALS)
        // due to large amount of entries to be processed (SGD will be too slow)
        for (int iter = 1; iter <= numIterations; iter++) {
            // Step 1: update user factors;
            DenseMatrix Yt = Y.transpose();
            DenseMatrix YtY = Yt.times(Y);
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {

                DenseMatrix YtCuI = new DenseMatrix(numFactors, numItems);//actually YtCuI is a sparse matrix
                //Yt * (Cu-itemIdx)
                SequentialSparseVector userRatingVec = trainMatrix.row(userIdx);
                int[] itemList = userRatingVec.getIndices();

                for (int itemIdx : itemList) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        YtCuI.set(factorIdx, itemIdx, Y.get(itemIdx, factorIdx) * aMinusb);
                    }
                }

                // YtY + Yt * (Cu - itemIdx) * Y
                DenseMatrix YtCuY = new DenseMatrix(numFactors, numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int factorIdxIn = 0; factorIdxIn < numFactors; factorIdxIn++) {
                        double value = 0.0;
                        for (int itemIdx : itemList) {
                            value += YtCuI.get(factorIdx, itemIdx) * Y.get(itemIdx, factorIdxIn);
                        }
                        YtCuY.set(factorIdx, factorIdxIn, value);
                    }
                }
                YtCuY = YtCuY.plus(YtY);
                // (YtCuY + lambda * itemIdx)^-1
                //lambda * itemIdx can be pre-difined because every time is the same.
                DenseMatrix Wu = (YtCuY.plus(userIdentityMatrix)).inverse();
                // Yt * (Cu - itemIdx) * Pu + Yt * Pu
                DenseVector YtCuPu = new VectorBasedDenseVector(numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int itemIdx : itemList) {
                        YtCuPu.plus(factorIdx, (YtCuI.get(factorIdx, itemIdx) + Yt.get(factorIdx, itemIdx)));
                    }
                }

                DenseVector xu = Wu.times(YtCuPu);
                // udpate user factors
                X.set(userIdx, xu);
            }

            // Step 2: update item factors;
            DenseMatrix Xt = X.transpose();
            DenseMatrix XtX = Xt.times(X);

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {


                DenseMatrix XtCiI = new DenseMatrix(numFactors, numUsers);
                //actually XtCiI is a sparse matrix
                //Xt * (Ci-itemIdx)
                int[] userList = trainMatrix.column(itemIdx).getIndices();
                for (int userIdx : userList) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        XtCiI.set(factorIdx, userIdx, X.get(userIdx, factorIdx) * aMinusb);
                    }
                }

                // XtX + Xt * (Ci - itemIdx) * X
                DenseMatrix XtCiX = new DenseMatrix(numFactors, numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int factorIdxIn = 0; factorIdxIn < numFactors; factorIdxIn++) {
                        double value = 0.0;
                        for (int userIdx : userList) {
                            value += XtCiI.get(factorIdx, userIdx) * X.get(userIdx, factorIdxIn);
                        }
                        XtCiX.set(factorIdx, factorIdxIn, value);
                    }
                }
                XtCiX = XtCiX.plus(XtX);

                // (XtCuX + lambda * itemIdx)^-1
                //lambda * itemIdx can be pre-difined because every time is the same.
                DenseMatrix Wi = (XtCiX.plus(itemIdentityMatrix)).inverse();
                // Xt * (Ci - itemIdx) * Pu + Xt * Pu
                DenseVector XtCiPu = new VectorBasedDenseVector(numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int userIdx : userList) {
                        XtCiPu.plus(factorIdx, (XtCiI.get(factorIdx, userIdx) + Xt.get(factorIdx, userIdx)));
                    }
                }

                DenseVector yi = Wi.times(XtCiPu);
                // udpate item factors
                Y.set(itemIdx, yi);
            }

            if (verbose) {
                LOG.info(getClass() + " runs at iteration = " + iter + " " + new Date());
            }
        }
    }

    protected DenseMatrix BuildEyeMatrix(int numDim) throws LibrecException {
        double[][] values = new double[numDim][numDim];
        for (int i=0; i<numDim; i++) {
            values[i][i] = 1.0;
        }
        return new DenseMatrix(values);
    }

}
