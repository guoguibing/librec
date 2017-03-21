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

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.Date;
import java.util.List;

/**
 * <h3>WRMF: Weighted Regularized Matrix Factorization.</h3>
 * <p>
 * This implementation refers to the method proposed by Hu et al. at ICDM 2008.
 * <ul>
 * <li><strong>Binary ratings:</strong> Pan et al., One-class Collaborative Filtering, ICDM 2008.</li>
 * <li><strong>Real ratings:</strong> Hu et al., Collaborative filtering for implicit feedback datasets, ICDM 2008.</li>
 * </ul>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "wrmf", "userFactors", "itemFactors", "trainMatrix"})
public class WRMFRecommender extends MatrixFactorizationRecommender {
    /**
     * confidence weight coefficient
     */
    protected float weightCoefficient;

    /**
     * confindence Minus Identity Matrix{ui} = confidenceMatrix_{ui} - 1 =alpha * r_{ui} or log(1+10^alpha * r_{ui})
     */
    protected SparseMatrix confindenceMinusIdentityMatrix;

    /**
     * preferenceMatrix_{ui} = 1 if {@code r_{ui}>0 or preferenceMatrix_{ui} = 0}
     */
    protected SparseMatrix preferenceMatrix;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        weightCoefficient = conf.getFloat("rec.wrmf.weight.coefficient", 4.0f);

        confindenceMinusIdentityMatrix = new SparseMatrix(trainMatrix);
        preferenceMatrix = new SparseMatrix(trainMatrix);
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
//            confindenceMinusIdentityMatrix.set(userIdx, itemIdx, weightCoefficient * matrixEntry.get());
            confindenceMinusIdentityMatrix.set(userIdx, itemIdx, Math.log(1.0 + Math.pow(10, weightCoefficient) * matrixEntry.get())); //maybe better for poi recommender
            preferenceMatrix.set(userIdx, itemIdx, 1.0d);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {

        SparseMatrix userIdentityMatrix = DiagMatrix.eye(numFactors).scale(regUser);
        SparseMatrix itemIdentityMatrix = DiagMatrix.eye(numFactors).scale(regItem);

        // To be consistent with the symbols in the paper
        DenseMatrix X = userFactors, Y = itemFactors;
        // Updating by using alternative least square (ALS)
        // due to large amount of entries to be processed (SGD will be too slow)
        for (int iter = 1; iter <= numIterations; iter++) {
            // Step 1: update user factors;
            DenseMatrix Yt = Y.transpose();
            DenseMatrix YtY = Yt.mult(Y);
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {

                DenseMatrix YtCuI = new DenseMatrix(numFactors, numItems);//actually YtCuI is a sparse matrix
                //Yt * (Cu-itemIdx)
                List<Integer> itemList = trainMatrix.getColumns(userIdx);
                for (int itemIdx : itemList) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        YtCuI.set(factorIdx, itemIdx, Y.get(itemIdx, factorIdx) * confindenceMinusIdentityMatrix.get(userIdx, itemIdx));
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
                YtCuY.addEqual(YtY);
                // (YtCuY + lambda * itemIdx)^-1
                //lambda * itemIdx can be pre-difined because every time is the same.
                DenseMatrix Wu = (YtCuY.add(userIdentityMatrix)).inv();
                // Yt * (Cu - itemIdx) * Pu + Yt * Pu
                DenseVector YtCuPu = new DenseVector(numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int itemIdx : itemList) {
                        YtCuPu.add(factorIdx, preferenceMatrix.get(userIdx, itemIdx) * (YtCuI.get(factorIdx, itemIdx) + Yt.get(factorIdx, itemIdx)));
                    }
                }

                DenseVector xu = Wu.mult(YtCuPu);
                // udpate user factors
                X.setRow(userIdx, xu);
            }

            // Step 2: update item factors;
            DenseMatrix Xt = X.transpose();
            DenseMatrix XtX = Xt.mult(X);

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {


                DenseMatrix XtCiI = new DenseMatrix(numFactors, numUsers);
                //actually XtCiI is a sparse matrix
                //Xt * (Ci-itemIdx)
                List<Integer> userList = trainMatrix.getRows(itemIdx);
                for (int userIdx : userList) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        XtCiI.set(factorIdx, userIdx, X.get(userIdx, factorIdx) * confindenceMinusIdentityMatrix.get(userIdx, itemIdx));
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
                XtCiX.addEqual(XtX);

                // (XtCuX + lambda * itemIdx)^-1
                //lambda * itemIdx can be pre-difined because every time is the same.
                DenseMatrix Wi = (XtCiX.add(itemIdentityMatrix)).inv();
                // Xt * (Ci - itemIdx) * Pu + Xt * Pu
                DenseVector XtCiPu = new DenseVector(numFactors);
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int userIdx : userList) {
                        XtCiPu.add(factorIdx, preferenceMatrix.get(userIdx, itemIdx) * (XtCiI.get(factorIdx, userIdx) + Xt.get(factorIdx, userIdx)));
                    }
                }

                DenseVector yi = Wi.mult(XtCiPu);
                // udpate item factors
                Y.setRow(itemIdx, yi);
            }

            if (verbose) {
                LOG.info(getClass()+" runs at iteration = "+iter+" "+new Date());
            }
        }
    }
}
