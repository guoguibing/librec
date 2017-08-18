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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.recommender.MatrixFactorizationRecommender;
import net.librec.util.Lists;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Gantner et al., <strong>Bayesian Personalized Ranking for Non-Uniformly Sampled Items</strong>, JMLR, 2012.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "wbpr", "userFactors", "itemFactors", "itemBiases", "trainMatrix"})
public class WBPRRecommender extends MatrixFactorizationRecommender {
    /**
     * user items Set
     */
    private LoadingCache<Integer, Set<Integer>> userItemsSet;

    /**
     * pre-compute and sort by item's popularity
     */
    private List<Map.Entry<Integer, Double>> sortedItemPops;

    /**
     * cache each user's candidate items with probabilities
     */
    private LoadingCache<Integer, List<Map.Entry<Integer, Double>>> cacheItemProbs;

    /**
     * user-items cache, item-users cache
     */
    protected LoadingCache<Integer, List<Integer>> userItemsCache;

    /**
     * items biases
     */
    private DenseVector itemBiases;

    /**
     * bias regularization
     */
    protected float regBias;

    /**
     * Guava cache configuration
     */
    protected static String cacheSpec;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        regBias = conf.getFloat("rec.bias.regularization", 0.01f);

        itemBiases = new DenseVector(numItems);
        itemBiases.init(0.01);

        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");
        userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
        userItemsSet = trainMatrix.rowColumnsSetCache(cacheSpec);

        // pre-compute and sort by item's popularity
        sortedItemPops = new ArrayList<>();
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            sortedItemPops.add(new AbstractMap.SimpleEntry<>(itemIdx, Double.valueOf(trainMatrix.columnSize(itemIdx))));
        }
        Lists.sortList(sortedItemPops, true);

        // cache each user's candidate items with probabilities
        cacheItemProbs = getCacheItemProbs();
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;
            for (int sampleCount = 0, smax = numUsers * 100; sampleCount < smax; sampleCount++) {
                // randomly draw (userIdx, posItemIdx, negItemIdx)
                int userIdx = 0, posItemIdx = 0, negItemIdx = 0;
                List<Integer> ratedItems = null;
                List<Map.Entry<Integer, Double>> itemProbs = null;

                while (true) {
                    userIdx = Randoms.uniform(numUsers);
                    try {
                        ratedItems = userItemsCache.get(userIdx);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    if (ratedItems.size() == 0)
                        continue;

                    posItemIdx = Randoms.random(ratedItems);

                    // sample j by popularity (probability)
                    try {
                        itemProbs = cacheItemProbs.get(userIdx);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    double rand = Randoms.random();
                    double sum = 0;
                    for (Map.Entry<Integer, Double> itemProb : itemProbs) {
                        sum += itemProb.getValue();
                        if (sum >= rand) {
                            negItemIdx = itemProb.getKey();
                            break;
                        }
                    }

                    break;
                }

                // update parameters
                double posPredictRating = predict(userIdx, posItemIdx);
                double negPredictRating = predict(userIdx, negItemIdx);
                double diffValue = posPredictRating - negPredictRating;

                double lossValue = -Math.log(Maths.logistic(diffValue));
                loss += lossValue;
                double deriValue = Maths.logistic(-diffValue);

                // update bias
                double posItemBiasValue = itemBiases.get(posItemIdx), negItemBiasValue = itemBiases.get(negItemIdx);
                itemBiases.add(posItemIdx, learnRate * (deriValue - regBias * posItemBiasValue));
                itemBiases.add(negItemIdx, learnRate * (-deriValue - regBias * negItemBiasValue));
                loss += regBias * (posItemBiasValue * posItemBiasValue + negItemBiasValue * negItemBiasValue);

                // update user/item vectors
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

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException if error occurs
     */
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return itemBiases.get(itemIdx) + DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
    }

    /**
     * cache each user's candidate items with probabilities
     *
     * @return cache each user's candidate items with probabilities
     */
    private LoadingCache<Integer, List<Map.Entry<Integer, Double>>> getCacheItemProbs() {
        LoadingCache<Integer, List<Map.Entry<Integer, Double>>> cache = CacheBuilder.from(cacheSpec).build(new CacheLoader<Integer, List<Map.Entry<Integer, Double>>>() {

            @Override
            public List<Map.Entry<Integer, Double>> load(Integer u) throws Exception {
                List<Map.Entry<Integer, Double>> itemProbs = new ArrayList<>();

                Set<Integer> ratedItemsSet = userItemsSet.get(u);

                // filter candidate items
                double sum = 0;
                for (Map.Entry<Integer, Double> itemPop : sortedItemPops) {
                    Integer itemIdx = itemPop.getKey();
                    double popularity = itemPop.getValue();

                    if (!ratedItemsSet.contains(itemIdx) && popularity > 0) {
                        // make a clone to prevent bugs from normalization
                        itemProbs.add(itemPop);
                        sum += popularity;
                    }
                }

                // normalization
                for (Map.Entry<Integer, Double> itemProb : itemProbs) {
                    itemProb.setValue(itemProb.getValue() / sum);
                }

                return itemProbs;
            }
        });
        return cache;
    }
}
