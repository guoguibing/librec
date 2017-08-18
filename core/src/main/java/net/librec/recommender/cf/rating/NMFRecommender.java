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
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.MatrixFactorizationRecommender;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative Matrix Factorization</strong>, NIPS 2001.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "nmf", "transUserFactors", "transItemFactors"})
public class NMFRecommender extends AbstractRecommender {
    /**
     * userFactors and itemFactors matrix transpose
     */
    DenseMatrix transUserFactors;
    DenseMatrix transItemFactors;

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
        numIterations = conf.getInt("rec.iterator.maximum",100);


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
                SparseVector itemRatingsVector = trainMatrix.row(userIdx);

                if (itemRatingsVector.getCount() > 0) {
                    SparseVector itemPredictsVector = new SparseVector(numItems, itemRatingsVector.size());

                    for (int itemIdx : itemRatingsVector.getIndex()) {
                        itemPredictsVector.append(itemIdx, predict(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        DenseVector factorItemsVector = transItemFactors.row(factorIdx, false);
                        double realValue = factorItemsVector.inner(itemRatingsVector);
                        double estmValue = factorItemsVector.inner(itemPredictsVector) + 1e-9;

                        transUserFactors.set(factorIdx, userIdx, transUserFactors.get(factorIdx, userIdx)
                                * (realValue / estmValue));
                    }
                }
            }

            // update itemFactors by fixing userFactors
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                SparseVector userRatingsVector = trainMatrix.column(itemIdx);

                if (userRatingsVector.getCount() > 0) {
                    SparseVector userPredictsVector = new SparseVector(numUsers, userRatingsVector.size());

                    for (int userIdx : userRatingsVector.getIndex()) {
                        userPredictsVector.append(userIdx, predict(userIdx, itemIdx));
                    }

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        DenseVector factorUsersVector = transUserFactors.row(factorIdx, false);
                        double realValue = factorUsersVector.inner(userRatingsVector);
                        double estmValue = factorUsersVector.inner(userPredictsVector) + 1e-9;

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
        return DenseMatrix.colMult(transUserFactors, userIdx, transItemFactors, itemIdx);
    }
}
