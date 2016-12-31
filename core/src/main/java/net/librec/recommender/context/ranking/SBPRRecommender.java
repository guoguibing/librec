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
package net.librec.recommender.context.ranking;

import com.google.common.cache.LoadingCache;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.SocialRecommender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Social Bayesian Personalized Ranking (SBPR)
 * <p>
 * Zhao et al., <strong>Leveraging Social Connections to Improve Personalized Ranking for Collaborative
 * Filtering</strong>, CIKM 2014.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "sbpr", "userFactors", "itemFactors", "itemBiases"})
public class SBPRRecommender extends SocialRecommender {
    /**
     * items biases vector
     */
    private DenseVector itemBiases;

    /**
     * bias regularization
     */
    protected float regBias;

    /**
     * user-items cache, item-users cache
     */
    protected LoadingCache<Integer, List<Integer>> userItemsCache;

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;

    /**
     * find items rated by trusted neighbors only
     */
    private List<List<Integer>> userSocialItemsSetList;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        regBias = conf.getFloat("rec.bias.regularization", 0.01f);
        cacheSpec = conf.get("guava.cache.spec", "maximumSize=5000,expireAfterAccess=50m");

        itemBiases = new DenseVector(numItems);
        itemBiases.init();

        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);

        // find items rated by trusted neighbors only
        userSocialItemsSetList = new ArrayList<>(numUsers);
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            userSocialItemsSetList.add(new ArrayList<Integer>());
        }

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {

            List<Integer> uRatedItems = null;
            try {
                uRatedItems = userItemsCache.get(userIdx);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (uRatedItems.size() == 0)
                continue; // no rated items

            // find items rated by trusted neighbors only
            List<Integer> trustedUsers = socialMatrix.getColumns(userIdx);
            List<Integer> items = new ArrayList<>();
            for (int trustedUserIdx : trustedUsers) {

                List<Integer> trustedRatedItems = null;
                try {
                    trustedRatedItems = userItemsCache.get(trustedUserIdx);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                for (int trustedRatedItemIdx : trustedRatedItems) {
                    // v's rated items
                    if (!uRatedItems.contains(trustedRatedItemIdx) && !items.contains(trustedRatedItemIdx)) // if not rated by user u and not already added to item list
                        items.add(trustedRatedItemIdx);
                }
            }
            userSocialItemsSetList.set(userIdx, items);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            for (int sample = 0, smax = numUsers * 100; sample < smax; sample++) {
                // uniformly draw (userIdx, posItemIdx, k, negItemIdx)
                int userIdx, posItemIdx, negItemIdx;
                // userIdx
                List<Integer> ratedItems = null;
                do {
                    userIdx = Randoms.uniform(trainMatrix.numRows());
                    try {
                        ratedItems = userItemsCache.get(userIdx);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } while (ratedItems.size() == 0);

                // positive item index
                posItemIdx = Randoms.random(ratedItems);

                double posPredictRating = predict(userIdx, posItemIdx);

                // social Items List
                List<Integer> socialItemsList = userSocialItemsSetList.get(userIdx);

                // negative item index
                do {
                    negItemIdx = Randoms.uniform(numItems);
                } while (ratedItems.contains(negItemIdx) || socialItemsList.contains(negItemIdx));

                double negPredictRating = predict(userIdx, negItemIdx);

                if (socialItemsList.size() > 0) {
                    // if having social neighbors
                    int socialItemIdx = Randoms.random(socialItemsList);
                    double socialPredictRating = predict(userIdx, socialItemIdx);

                    SparseVector trustedUsersVector = socialMatrix.row(userIdx);
                    double socialWeight = 0;
                    for (VectorEntry trustedVectorEntry : trustedUsersVector) {
                        int trustedUserIdx = trustedVectorEntry.index();
                        if (trustedUserIdx < trainMatrix.numRows()) {
                            double socialRating = trainMatrix.get(trustedUserIdx, socialItemIdx);
                            if (socialRating > 0)
                                socialWeight += 1;
                        }
                    }

                    double posSocialDiffValue = (posPredictRating - socialPredictRating) / (1 + socialWeight);
                    double socialNegDiffValue = socialPredictRating - negPredictRating;

                    double error = -Math.log(Maths.logistic(posSocialDiffValue)) - Math.log(Maths.logistic(socialNegDiffValue));
                    loss += error;

                    double posSocialGradient = Maths.logistic(-posSocialDiffValue), socialNegGradient = Maths.logistic(-socialNegDiffValue);

                    // update bi, bk, bj
                    double posItemBiasValue = itemBiases.get(posItemIdx);
                    itemBiases.add(posItemIdx, learnRate * (posSocialGradient / (1 + socialWeight) - regBias * posItemBiasValue));
                    loss += regBias * posItemBiasValue * posItemBiasValue;

                    double socialItemBiasValue = itemBiases.get(socialItemIdx);
                    itemBiases.add(socialItemIdx, learnRate * (-posSocialGradient / (1 + socialWeight) + socialNegGradient - regBias * socialItemBiasValue));
                    loss += regBias * socialItemBiasValue * socialItemBiasValue;

                    double negItemBiasValue = itemBiases.get(negItemIdx);
                    itemBiases.add(negItemIdx, learnRate * (-socialNegGradient - regBias * negItemBiasValue));
                    loss += regBias * negItemBiasValue * negItemBiasValue;

                    // update P, Q
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                        double socialItemFactorValue = itemFactors.get(socialItemIdx, factorIdx);
                        double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                        double delta_puf = posSocialGradient * (posItemFactorValue - socialItemFactorValue) / (1 + socialWeight)
                                + socialNegGradient * (socialItemFactorValue - negItemFactorValue);
                        userFactors.add(userIdx, factorIdx, learnRate * (delta_puf - regUser * userFactorValue));

                        itemFactors.add(posItemIdx, factorIdx, learnRate * (posSocialGradient * userFactorValue / (1 + socialWeight)
                                - regItem * posItemFactorValue));

                        double delta_qkf = posSocialGradient * (-userFactorValue / (1 + socialWeight)) + socialNegGradient * userFactorValue;
                        itemFactors.add(socialItemIdx, factorIdx, learnRate * (delta_qkf - regItem * socialItemFactorValue));

                        itemFactors.add(negItemIdx, factorIdx, learnRate * (socialNegGradient * (-userFactorValue) -
                                regItem * negItemFactorValue));

                        loss += regUser * userFactorValue * userFactorValue + regItem * posItemFactorValue * posItemFactorValue +
                                regItem * negItemFactorValue * negItemFactorValue + regItem * socialItemFactorValue * socialItemFactorValue;
                    }
                } else {
                    // if no social neighbors, the same as BPR
                    double posNegDiffValue = posPredictRating - negPredictRating;
                    loss += posNegDiffValue;

                    double posNegGradient = Maths.logistic(-posNegDiffValue);

                    // update bi, bj
                    double posItemBiasValue = itemBiases.get(posItemIdx);
                    itemBiases.add(posItemIdx, learnRate * (posNegGradient - regBias * posItemBiasValue));
                    loss += regBias * posItemBiasValue * posItemBiasValue;

                    double negItemBiasValue = itemBiases.get(negItemIdx);
                    itemBiases.add(negItemIdx, learnRate * (-posNegGradient - regBias * negItemBiasValue));
                    loss += regBias * negItemBiasValue * negItemBiasValue;

                    // update user factors, item factors
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double posItemFactorValue = itemFactors.get(posItemIdx, factorIdx);
                        double negItemFactorValue = itemFactors.get(negItemIdx, factorIdx);

                        userFactors.add(userIdx, factorIdx, learnRate * (posNegGradient * (posItemFactorValue - negItemFactorValue) - regUser * userFactorValue));
                        itemFactors.add(posItemIdx, factorIdx, learnRate * (posNegGradient * userFactorValue - regItem * posItemFactorValue));
                        itemFactors.add(negItemIdx, factorIdx, learnRate * (posNegGradient * (-userFactorValue) - regItem * negItemFactorValue));

                        loss += regUser * userFactorValue * userFactorValue + regItem * posItemFactorValue * posItemFactorValue +
                                regItem * negItemFactorValue * negItemFactorValue;
                    }
                }
            }

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }


    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive ranking score for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double predictRating = itemBiases.get(itemIdx) + DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);

        return predictRating;
    }
}
