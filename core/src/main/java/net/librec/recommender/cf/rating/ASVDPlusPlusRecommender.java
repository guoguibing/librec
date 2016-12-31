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

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Yehuda Koren, <strong>Factorization Meets the Neighborhood: a Multifaceted
 * Collaborative Filtering Model.</strong>, KDD 2008. Asymmetric SVD++
 * Recommender
 *
 * @author Bin Wu(wubin@gs.zzu.edu.cn)
 */
public class ASVDPlusPlusRecommender extends BiasedMFRecommender {

    protected DenseMatrix impItemFactors, neiItemFactors;
    /**
     * user items list
     */
    protected List<List<Integer>> userItemsList;

    protected void setup() throws LibrecException {
        super.setup();
        impItemFactors = new DenseMatrix(numItems, numFactors);
        impItemFactors.init(initMean, initStd);
        neiItemFactors = new DenseMatrix(numItems, numFactors);
        neiItemFactors.init(initMean, initStd);
        userItemsList = getUserItemsList(trainMatrix);
    }

    @Override
    protected void trainModel() throws LibrecException {

        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;

            for (MatrixEntry matrixEntry : trainMatrix) {

                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double realRating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = realRating - predictRating;
                List<Integer> items = userItemsList.get(userIdx);
                double impNor = Math.sqrt(items.size());

                // update factors
                double userBiasValue = userBiases.get(userIdx);
                userBiases.add(userIdx, learnRate * (error - regBias * userBiasValue));

                double itemBiasValue = itemBiases.get(itemIdx);
                itemBiases.add(itemIdx, learnRate * (error - regBias * itemBiasValue));

                double[] sumImpItemsFactors = new double[numFactors];
                double[] sumNeiItemsFactors = new double[numFactors];
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double sumImpItemsFactor = 0;
                    double sumNeiItemsFactor = 0;
                    for (int ItemIdx : items) {
                        sumImpItemsFactor += impItemFactors.get(ItemIdx, factorIdx);
                        sumNeiItemsFactor += neiItemFactors.get(ItemIdx, factorIdx)
                                * (realRating - globalMean - userBiases.get(userIdx) - itemBiases.get(ItemIdx));
                    }
                    sumImpItemsFactors[factorIdx] = impNor > 0 ? sumImpItemsFactor / impNor : sumImpItemsFactor;
                    sumNeiItemsFactors[factorIdx] = impNor > 0 ? sumNeiItemsFactor / impNor : sumNeiItemsFactor;
                }

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorIdx = userFactors.get(userIdx, factorIdx);
                    double itemFactorIdx = itemFactors.get(itemIdx, factorIdx);

                    double sgd_user = error * itemFactorIdx - regUser * userFactorIdx;
                    double sgd_item = error
                            * (userFactorIdx + sumImpItemsFactors[factorIdx] + sumNeiItemsFactors[factorIdx])
                            - regItem * itemFactorIdx;

                    userFactors.add(userIdx, factorIdx, learnRate * sgd_user);
                    itemFactors.add(itemIdx, factorIdx, learnRate * sgd_item);
                    for (int ImpitemIdx : items) {
                        double impItemFactorIdx = impItemFactors.get(ImpitemIdx, factorIdx);
                        double neiItemFactorIdx = neiItemFactors.get(ImpitemIdx, factorIdx);
                        double delta_impItem = error * itemFactorIdx / impNor - regUser * impItemFactorIdx;
                        double delta_neiItem = error * itemFactorIdx
                                * (realRating - globalMean - userBiases.get(userIdx) - itemBiases.get(ImpitemIdx)) / impNor
                                - regUser * neiItemFactorIdx;
                        impItemFactors.add(ImpitemIdx, factorIdx, learnRate * delta_impItem);
                        neiItemFactors.add(ImpitemIdx, factorIdx, learnRate * delta_neiItem);
                    }
                }

            }
        }
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double predictRating = globalMean + userBiases.get(userIdx) + itemBiases.get(itemIdx)
                + DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);

        List<Integer> items = userItemsList.get(userIdx);
        double w = Math.sqrt(items.size());
        for (int k : items) {
            predictRating += DenseMatrix.rowMult(impItemFactors, k, itemFactors, itemIdx) / w;
            predictRating += neiItemFactors.row(k)
                    .scale(trainMatrix.get(userIdx, k) - globalMean - userBiases.get(userIdx) - itemBiases.get(k))
                    .inner(itemFactors.row(itemIdx)) / w;
        }
        return predictRating;
    }

    private List<List<Integer>> getUserItemsList(SparseMatrix sparseMatrix) {
        List<List<Integer>> userItemsList = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsList.add(sparseMatrix.getColumns(userIdx));
        }
        return userItemsList;
    }
}