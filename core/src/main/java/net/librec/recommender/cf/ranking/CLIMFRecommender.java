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
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.*;

/**
 * Shi et al., <strong>Climf: learning to maximize reciprocal rank with collaborative less-is-more filtering.</strong>,
 * RecSys 2012.
 *
 * @author Guibing Guo, Chen Ma and Keqiang Wang
 */
@ModelData({"isRanking", "climf", "userFactors", "itemFactors"})
public class CLIMFRecommender extends MatrixFactorizationRecommender {

    private List<Set<Integer>> userItemsSet;


    @Override
    protected void setup() throws LibrecException {
        super.setup();
    }

    @Override
    protected void trainModel() throws LibrecException {
        userItemsSet = getUserItemsSet(trainMatrix);

        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0f;
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {

                Set<Integer> itemSet = userItemsSet.get(userIdx);
                double[] sgds = new double[numFactors];

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {

                    double sgd = -regUser * userFactors.get(userIdx, factorIdx);

                    for (int itemIdx : itemSet) {
                        double predictValue = predict(userIdx, itemIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        sgd += Maths.logistic(-predictValue) * itemFactorValue;

                        for (int compareItemIdx : itemSet) {
                            if (compareItemIdx == itemIdx) {
                                continue;
                            }

                            double compPredictValue = predict(userIdx, compareItemIdx);
                            double compItemFactorValue = itemFactors.get(compareItemIdx, factorIdx);

                            double diffValue = compPredictValue - predictValue;

                            sgd += Maths.logisticGradientValue(diffValue) / (1 - Maths.logistic(diffValue)) * (itemFactorValue - compItemFactorValue);
                        }
                    }

                    sgds[factorIdx] = sgd;
                }

                Map<Integer, List<Double>> itemsSgds = new HashMap<>();
                for (int itemIdx : itemSet) {
                    double predictValue = predict(userIdx, itemIdx);
                    List<Double> itemSgds = new ArrayList<>();
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        double judgeValue = 1.0d;
                        double sgd = judgeValue * Maths.logistic(-predictValue) * userFactorValue - regItem * itemFactorValue;

                        for (int compItemIdx : itemSet) {
                            if (compItemIdx == itemIdx) {
                                continue;
                            }

                            double compPredictValue = predict(userIdx, compItemIdx);
                            double diffValue = compPredictValue - predictValue;

                            sgd += Maths.logisticGradientValue(-diffValue) * (1.0d / (1 - Maths.logistic(diffValue)) -
                                    1.0d / (1 - Maths.logistic(-diffValue))) * userFactorValue;
                        }

                        itemSgds.add(sgd);
                    }

                    itemsSgds.put(itemIdx, itemSgds);
                }

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    userFactors.add(userIdx, factorIdx, learnRate * sgds[factorIdx]);
                }

                for (int itemIdx : itemSet) {
                    List<Double> itemSgds = itemsSgds.get(itemIdx);
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        itemFactors.add(itemIdx, factorIdx, learnRate * itemSgds.get(factorIdx));
                    }
                }

                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    if (itemSet.contains(itemIdx)) {
                        double predictValue = predict(userIdx, itemIdx);

                        loss += Math.log(Maths.logistic(predictValue));

                        for (int compItemIdx : itemSet) {
                            double compPredictValue = predict(userIdx, compItemIdx);
                            loss += Math.log(1 - Maths.logistic(compPredictValue - predictValue));
                        }
                    }

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        loss += -0.5 * (regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue);
                    }
                }
            }

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    private List<Set<Integer>> getUserItemsSet(SparseMatrix sparseMatrix) {
        List<Set<Integer>> userItemsSet = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsSet.add(new HashSet(sparseMatrix.getColumns(userIdx)));
        }
        return userItemsSet;
    }
}
