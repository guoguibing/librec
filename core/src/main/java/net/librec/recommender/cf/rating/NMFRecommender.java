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
package net.librec.recommender.cf.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.VectorBasedDenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.recommender.MatrixFactorizationRecommender;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative Matrix Factorization</strong>, NIPS 2001.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "nmf", "transUserFactors", "transItemFactors"})
public class NMFRecommender extends MatrixFactorizationRecommender {
    /**
     * userFactors and itemFactors matrix transpose
     */
    private DenseMatrix transUserFactors;
    private DenseMatrix transItemFactors;

    /**
     * the number of latent factors;
     */
    protected int numFactors;

    /**
     * the number of iterations
     */
    protected int numIterations;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numFactors = conf.getInt("rec.factor.number", 10);
        numIterations = conf.getInt("rec.iterator.maximum", 100);


        transUserFactors = new DenseMatrix(numFactors, numUsers);
        transItemFactors = new DenseMatrix(numFactors, numItems);
        transUserFactors.init(0.01);
        transItemFactors.init(0.01);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 0; iter <= numIterations; ++iter) {
            // update userFactors by fixing itemFactors
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                SequentialSparseVector itemRatingsVector = trainMatrix.row(userIdx);

                if (itemRatingsVector.getNumEntries() > 0) {
                    VectorBasedDenseVector itemPredictsVector = new VectorBasedDenseVector(numItems);

                    for (int i = 0; i < itemRatingsVector.getNumEntries(); i++) {
                        int itemIdx = itemRatingsVector.getIndexAtPosition(i);
                        itemPredictsVector.set(itemIdx, predict(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        VectorBasedDenseVector factorItemsVector = (VectorBasedDenseVector) transItemFactors.row(factorIdx);
                        double realValue = factorItemsVector.dot(itemRatingsVector);
                        double estmValue = factorItemsVector.dot(itemPredictsVector) + 1e-9;

                        transUserFactors.set(factorIdx, userIdx, transUserFactors.get(factorIdx, userIdx)
                                * (realValue / estmValue));
                    }
                }
            }

            // update itemFactors by fixing userFactors
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                SequentialSparseVector userRatingsVector = trainMatrix.column(itemIdx);

                if (userRatingsVector.getNumEntries() > 0) {
                    VectorBasedDenseVector userPredictsVector = new VectorBasedDenseVector(numUsers);

                    for (int i = 0; i < userRatingsVector.getNumEntries(); i++) {
                        int userIdx = userRatingsVector.getIndexAtPosition(i);
                        userPredictsVector.set(userIdx, predict(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        VectorBasedDenseVector factorUsersVector = (VectorBasedDenseVector) transUserFactors.row(factorIdx);
                        double realValue = factorUsersVector.dot(userRatingsVector);
                        double estmValue = factorUsersVector.dot(userPredictsVector) + 1e-9;

                        transItemFactors.set(factorIdx, itemIdx, transItemFactors.get(factorIdx, itemIdx)
                                * (realValue / estmValue));
                    }
                }
            }

            // compute errors
            loss = 0.0d;
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                if (rating > 0) {
                    double ratingError = predict(userIdx, itemIdx) - rating;

                    loss += ratingError * ratingError;
                }
            }

            loss *= 0.5d;
            if (isConverged(iter) && earlyStop) {
                break;
            }
            lastLoss = loss;
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return transUserFactors.column(userIdx).dot(transItemFactors.column(itemIdx));
    }
}
