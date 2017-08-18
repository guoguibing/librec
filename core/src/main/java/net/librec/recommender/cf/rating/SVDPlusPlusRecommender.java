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
import net.librec.math.structure.SparseMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * SVD++ Recommender
 *
 * @author GuoGuibing and Keqiang Wang
 */
@ModelData({"isRating", "svdplusplus", "userFactors", "itemFactors", "userBiases", "itemBiases", "impItemFactors", "trainMatrix"})
public class SVDPlusPlusRecommender extends BiasedMFRecommender {
    /**
     * item implicit feedback factors, "imp" string means implicit
     */
    protected DenseMatrix impItemFactors;

    /**
     * user items list
     */
    protected List<List<Integer>> userItemsList;

    /**
     * implicit item regularization
     */
    private double regImpItem;

    /*
     * (non-Javadoc)
	 *
	 * @see net.librec.recommender.AbstractRecommender#setup()
	 */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        regImpItem = conf.getDouble("rec.impItem.regularization", 0.015d);

        impItemFactors = new DenseMatrix(numItems, numFactors);
        impItemFactors.init(initMean, initStd);
        userItemsList = getUserItemsList(trainMatrix);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;
            for (MatrixEntry matrixEntry : trainMatrix) {

                int userIdx = matrixEntry.row(); // user
                int itemIdx = matrixEntry.column(); // item
                double realRating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = realRating - predictRating;

                loss += error * error;

                List<Integer> items = userItemsList.get(userIdx);

                // update user and item bias
                double userBiasValue = userBiases.get(userIdx);
                userBiases.add(userIdx, learnRate * (error - regBias * userBiasValue));

                loss += regBias * userBiasValue * userBiasValue;

                double itemBiasValue = itemBiases.get(itemIdx);
                itemBiases.add(itemIdx, learnRate * (error - regBias * itemBiasValue));

                loss += regBias * itemBiasValue * itemBiasValue;

                DenseVector sumImpItemsFactors = new DenseVector(numFactors);
                for (int impItemIdx : items) {
                    sumImpItemsFactors.addEqual(impItemFactors.row(impItemIdx, false));
                }

                double impNor = Math.sqrt(items.size());
                if (impNor > 0) {
                    sumImpItemsFactors.scaleEqual(1.0 / impNor);
                }

                //update user and item factors
                for (int factorIdx = 0; factorIdx < numFactors; ++factorIdx) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                    userFactors.add(userIdx, factorIdx, learnRate * (error * itemFactorValue - regUser * userFactorValue));
                    itemFactors.add(itemIdx, factorIdx, learnRate * (error * (userFactorValue + sumImpItemsFactors.get(factorIdx)) - regItem * itemFactorValue));

                    loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;

                    for (int impItemIdx : items) {
                        double impItemFactor = impItemFactors.get(impItemIdx, factorIdx);
                        impItemFactors.add(impItemIdx, factorIdx, learnRate * (error * itemFactorValue / impNor - regImpItem * impItemFactor));

                        loss += regImpItem * impItemFactor * impItemFactor;
                    }
                }
            }
            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double predictRating = userBiases.get(userIdx) + itemBiases.get(itemIdx) + globalMean;

        List<Integer> items = userItemsList.get(userIdx);
        DenseVector userImpFactor = new DenseVector(numFactors);

        // sum of implicit feedback factors of userIdx with weight Math.sqrt(1.0 / userItemsList.get(userIdx).size())
        for (int impItemIdx : items) {
            userImpFactor.addEqual(impItemFactors.row(impItemIdx, false));
        }

        double impNor = Math.sqrt(items.size());
        if (impNor > 0) {
            userImpFactor.scaleEqual(1.0 / impNor);
        }
        // sum with user factors
        userImpFactor.addEqual(userFactors.row(userIdx, false));

        return predictRating + userImpFactor.inner(itemFactors.row(itemIdx, false));
    }

    private List<List<Integer>> getUserItemsList(SparseMatrix sparseMatrix) {
        List<List<Integer>> userItemsList = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsList.add(sparseMatrix.getColumns(userIdx));
        }
        return userItemsList;
    }
}
