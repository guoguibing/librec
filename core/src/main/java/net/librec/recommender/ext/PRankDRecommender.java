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
package net.librec.recommender.ext;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.cf.ranking.RankSGDRecommender;
import net.librec.util.Lists;

import java.util.HashMap;
import java.util.Map;

/**
 * Neil Hurley, <strong>Personalised ranking with diversity</strong>, RecSys 2013.
 * <p>
 * Related Work:
 * <ul>
 * <li>Jahrer and Toscher, Collaborative Filtering Ensemble for Ranking, JMLR, 2012 (KDD Cup 2011 Track 2).</li>
 * </ul>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "prankd", "userFactors", "itemFactors", "trainMatrix"})
public class PRankDRecommender extends RankSGDRecommender {
    /**
     * item importance
     */
    private DenseVector itemWeights;

    /**
     * item correlations
     */
    private SymmMatrix itemCorrs;

    /**
     * similarity filter
     */
    private float simFilter;

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        simFilter = conf.getFloat("rec.sim.filter", 4.0f);
        // compute item sampling probability
        Map<Integer, Double> itemProbsMap = new HashMap<>();
        double maxUsersCount = 0;

        itemWeights = new DenseVector(numItems);
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            int usersCount = trainMatrix.columnSize(itemIdx);

            maxUsersCount = maxUsersCount < usersCount ? usersCount : maxUsersCount;
            itemWeights.set(itemIdx, usersCount);
            // sample items based on popularity
            double prob = (usersCount + 0.0) / numRates;
            if (prob > 0)
                itemProbsMap.put(itemIdx, prob);
        }
        itemProbs = Lists.sortMap(itemProbsMap);

        // compute item relative importance
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            itemWeights.set(itemIdx, itemWeights.get(itemIdx) / maxUsersCount);
        }

        // compute item correlations by cosine similarity
        itemCorrs = context.getSimilarity().getSimilarityMatrix();
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;
            // for each rated user-item (u,i) pair
            for (int userIdx : trainMatrix.rows()) {
                SparseVector itemRatingsVector = trainMatrix.row(userIdx);
                for (VectorEntry itemRatingEntry : itemRatingsVector) {
                    // each rated item i
                    int posItemIdx = itemRatingEntry.index();
                    double posRating = itemRatingEntry.get();

                    int negItemIdx = -1;
                    while (true) {
                        // draw an item j with probability proportional to popularity
                        double sum = 0, randValue = Randoms.random();
                        for (Map.Entry<Integer, Double> mapEntry : itemProbs) {
                            int tempNegItemIdx = mapEntry.getKey();
                            double prob = mapEntry.getValue();

                            sum += prob;
                            if (sum >= randValue) {
                                negItemIdx = tempNegItemIdx;
                                break;
                            }
                        }

                        // ensure that it is unrated by user u
                        if (!itemRatingsVector.contains(negItemIdx))
                            break;
                    }
                    double negRating = 0;

                    // compute predictions
                    double posPredictRating = predict(userIdx, posItemIdx), negPredictRating = predict(userIdx, negItemIdx);

                    double distance = Math.sqrt(1 - Math.tanh(itemCorrs.get(posItemIdx, negItemIdx) * simFilter));
                    double itemWeightValue = itemWeights.get(negItemIdx);

                    double error = itemWeightValue * (posPredictRating - negPredictRating - distance * (posRating - negRating));
                    loss += error * error;

                    // update vectors
                    double learnFactor = learnRate * error;
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                        double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                        userFactors.add(userIdx, factorIdx, -learnFactor * (posItemFactorValue - negItemFactorValue));
                        itemFactors.add(posItemIdx, factorIdx, -learnFactor * userFactorValue);
                        itemFactors.add(negItemIdx, factorIdx, learnFactor * userFactorValue);
                    }
                }
            }
            loss *= 0.5;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }
}

