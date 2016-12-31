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
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rendle et al., <strong>BPR: Bayesian Personalized Ranking from Implicit Feedback</strong>, UAI 2009.
 *
 * @author GuoGuibing and Keqiang Wang
 */
@ModelData({"isRanking", "bpr", "userFactors", "itemFactors"})
public class BPRRecommender extends MatrixFactorizationRecommender {
    private List<Set<Integer>> userItemsSet;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
    }

    @Override
    protected void trainModel() throws LibrecException {

        userItemsSet = getUserItemsSet(trainMatrix);

        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;
            for (int sampleCount = 0, smax = numUsers * 100; sampleCount < smax; sampleCount++) {

                // randomly draw (userIdx, posItemIdx, negItemIdx)
                int userIdx, posItemIdx, negItemIdx;
                while (true) {
                    userIdx = Randoms.uniform(numUsers);
                    Set<Integer> itemSet = userItemsSet.get(userIdx);
                    if (itemSet.size() == 0 || itemSet.size() == numItems)
                        continue;

                    List<Integer> itemList = trainMatrix.getColumns(userIdx);
                    posItemIdx = itemList.get(Randoms.uniform(itemList.size()));
                    do {
                        negItemIdx = Randoms.uniform(numItems);
                    } while (itemSet.contains(negItemIdx));

                    break;
                }

                // update parameters
                double posPredictRating = predict(userIdx, posItemIdx);
                double negPredictRating = predict(userIdx, negItemIdx);
                double diffValue = posPredictRating - negPredictRating;

                double lossValue = -Math.log(Maths.logistic(diffValue));
                loss += lossValue;

                double deriValue = Maths.logistic(-diffValue);

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                    double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                    userFactors.add(userIdx, factorIdx, learnRate * (deriValue * (posItemFactorValue - negItemFactorValue) - regUser * userFactorValue));
                    itemFactors.add(posItemIdx, factorIdx, learnRate * (deriValue * userFactorValue - regItem * posItemFactorValue));
                    itemFactors.add(negItemIdx, factorIdx, learnRate * (deriValue * (-userFactorValue) - regItem * negItemFactorValue));

                    loss += regUser * userFactorValue * userFactorValue + regItem * posItemFactorValue * posItemFactorValue + regItem * negItemFactorValue * negItemFactorValue;
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
