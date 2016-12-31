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
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.MatrixFactorizationRecommender;
import net.librec.util.Lists;

import java.util.*;

/**
 * Jahrer and Toscher, Collaborative Filtering Ensemble for Ranking, JMLR, 2012 (KDD Cup 2011 Track 2).
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "ranksgd", "userFactors", "itemFactors", "trainMatrix"})
public class RankSGDRecommender extends MatrixFactorizationRecommender {
    // item sampling probabilities sorted ascendingly
    protected List<Map.Entry<Integer, Double>> itemProbs;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        // compute item sampling probability
        Map<Integer, Double> itemProbsMap = new HashMap<>();
        for (int j = 0; j < numItems; j++) {
            int users = trainMatrix.columnSize(j);

            // sample items based on popularity
            double prob = (users + 0.0) / numRates;
            if (prob > 0)
                itemProbsMap.put(j, prob);
        }
        itemProbs = Lists.sortMap(itemProbsMap);
    }

    @Override
    protected void trainModel() throws LibrecException {
        List<Set<Integer>> userItemsSet = getUserItemsSet(trainMatrix);
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            // for each rated user-item (u,i) pair
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int posItemIdx = matrixEntry.column();
                double posRating = matrixEntry.get();
                int negItemIdx = -1;

                while (true) {
                    // draw an item j with probability proportional to popularity
                    double sum = 0, rand = Randoms.random();
                    for (Map.Entry<Integer, Double> itemProb : itemProbs) {
                        int itemIdx = itemProb.getKey();
                        double prob = itemProb.getValue();

                        sum += prob;
                        if (sum >= rand) {
                            negItemIdx = itemIdx;
                            break;
                        }
                    }
                    // ensure that it is unrated by user u
                    if (!userItemsSet.get(userIdx).contains(negItemIdx))
                        break;
                }

                double negRating = 0;
                // compute predictions
                double posPredictRating = predict(userIdx, posItemIdx), negPredictRating = predict(userIdx, negItemIdx);
                double error = (posPredictRating - negPredictRating) - (posRating - negRating);

                loss += error * error;

                // update vectors
                double sgd = learnRate * error;
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                    double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                    userFactors.add(userIdx, factorIdx, -sgd * (posItemFactorValue - negItemFactorValue));
                    itemFactors.add(posItemIdx, factorIdx, -sgd * userFactorValue);
                    itemFactors.add(negItemIdx, factorIdx, sgd * userFactorValue);
                }

            }

            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }


    private List<Set<Integer>> getUserItemsSet(SparseMatrix sparseMatrix) {
        List<Set<Integer>> userItemsSet = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            userItemsSet.add(new HashSet<>(sparseMatrix.getColumns(userIdx)));
        }
        return userItemsSet;
    }
}
