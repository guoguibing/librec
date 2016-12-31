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

import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;
import net.librec.util.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Xia Ning and George Karypis, <strong>SLIM: Sparse Linear Methods for Top-N Recommender Systems</strong>, ICDM 2011. <br>
 * <p>
 * Related Work:
 * <ul>
 * <li>Levy and Jack, Efficient Top-N Recommendation by Linear Regression, ISRS 2013. This paper reports experimental
 * results on the MovieLens (100K, 10M) and Epinions datasets in terms of precision, MRR and HR@N (i.e., Recall@N).</li>
 * <li>Friedman et al., Regularization Paths for Generalized Linear Models via Coordinate Descent, Journal of
 * Statistical Software, 2010.</li>
 * </ul>
 *
 * @author guoguibing and Keqiang Wang
 */
public class SLIMRecommender extends AbstractRecommender {
    /**
     * the number of iterations
     */
    protected int numIterations;

    private DenseMatrix coefficientMatrix;

    /**
     * item's nearest neighbors for kNN > 0
     */
    private Multimap<Integer, Integer> itemNNs;

    /**
     * item's nearest neighbors for kNN <=0, i.e., all other items
     */
    private List<Integer> allItems;

    /**
     * regularization parameters for the L1 or L2 term
     */
    private float regL1Norm, regL2Norm;

    /**
     * user-vector cache, item-vector cache
     */
    protected LoadingCache<Integer, SparseVector> userCache;

    /**
     * number of nearest neighbors
     */
    protected static int numberNearestNeighbors;

    /**
     * Guava cache configuration
     */
    protected String cacheSpec;

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        regL1Norm = conf.getFloat("rec.slim.regularization.l1",0.01f);
        regL2Norm = conf.getFloat("rec.slim.regularization.l2",0.01f);

        coefficientMatrix = new DenseMatrix(numItems, numItems);
        coefficientMatrix.init(); // initial guesses: make smaller guesses (e.g., W.init(0.01)) to speed up training
        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");

        userCache = trainMatrix.rowCache(cacheSpec);

        if (numberNearestNeighbors > 0) {
            // find the nearest neighbors for each item based on item similarity
            SymmMatrix itemCorrs = context.getSimilarity().getSimilarityMatrix();

            itemNNs = HashMultimap.create();
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // set diagonal entries to 0
                coefficientMatrix.set(itemIdx, itemIdx, 0);
                // find the k-nearest neighbors for each item
                Map<Integer, Double> nearestNeighborMap = itemCorrs.row(itemIdx).toMap();

                // sort by values to retriev topN similar items
                if (numberNearestNeighbors > 0 && numberNearestNeighbors < nearestNeighborMap.size()) {
                    List<Map.Entry<Integer, Double>> sorted = Lists.sortMap(nearestNeighborMap, true);
                    List<Map.Entry<Integer, Double>> subset = sorted.subList(0, numberNearestNeighbors);
                    nearestNeighborMap.clear();
                    for (Map.Entry<Integer, Double> nearestNeighborEntry : subset)
                        nearestNeighborMap.put(nearestNeighborEntry.getKey(), nearestNeighborEntry.getValue());
                }

                // put into the nns multimap
                for (Map.Entry<Integer, Double> multimapEntry : nearestNeighborMap.entrySet())
                    itemNNs.put(itemIdx, multimapEntry.getKey());
            }
        } else {
            // all items are used
            allItems = trainMatrix.columns();

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++)
                coefficientMatrix.set(itemIdx, itemIdx, 0.0);
        }
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {
        // number of iteration cycles
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            // each cycle iterates through one coordinate direction
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // find k-nearest neighbors
                Collection<Integer> nearestNeighborCollection = numberNearestNeighbors > 0 ? itemNNs.get(itemIdx) : allItems;

                // for each nearest neighbor nearestNeighborItemIdx, update coefficienMatrix by the coordinate
                // descent update rule
                // it is OK if nearestNeighborItemIdx==itemIdx, since coefficienMatrix  = 0;
                for (Integer nearestNeighborItemIdx : nearestNeighborCollection) {
                    if (nearestNeighborItemIdx != itemIdx) {
                        double gradSum = 0.0d, rateSum = 0.0d, errors = 0.0d;

                        SparseVector nnUserRatingsVector = trainMatrix.column(nearestNeighborItemIdx);
                        int nnCount = nnUserRatingsVector.getCount();
                        for (VectorEntry nnUserVectorEntry : nnUserRatingsVector) {
                            int nnUserIdx = nnUserVectorEntry.index();
                            double nnRating = nnUserVectorEntry.get();
                            double rating = trainMatrix.get(nnUserIdx, itemIdx);
                            double error = rating - predict(nnUserIdx, itemIdx, nearestNeighborItemIdx);

                            gradSum += nnRating * error;
                            rateSum += nnRating * nnRating;

                            errors += error * error;
                        }
                        gradSum /= nnCount;
                        rateSum /= nnCount;

                        errors /= nnCount;
                        double coefficient = coefficientMatrix.get(nearestNeighborItemIdx, itemIdx);
                        loss += errors + 0.5 * regL2Norm * coefficient * coefficient + regL1Norm * coefficient;


                        if (regL1Norm < Math.abs(gradSum)) {
                            if (gradSum > 0) {
                                double update = (gradSum - regL1Norm) / (regL2Norm + rateSum);
                                coefficientMatrix.set(nearestNeighborItemIdx, itemIdx, update);
                            } else {
                                // One doubt: in this case, wij<0, however, the
                                // paper says wij>=0. How to gaurantee that?
                                double update = (gradSum + regL1Norm) / (regL2Norm + rateSum);
                                coefficientMatrix.set(nearestNeighborItemIdx, itemIdx, update);
                            }
                        } else {
                            coefficientMatrix.set(nearestNeighborItemIdx, itemIdx, 0.0);
                        }
                    }
                }
            }
            if (isConverged(iter) && earlyStop)
                break;
        }
    }


    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @param excludedItemIdx excluded item index
     * @return a prediction without the contribution of excluded item
     */
    protected double predict(int userIdx, int itemIdx, int excludedItemIdx) {
        SparseVector itemRatingsVector = null;
        try {
            itemRatingsVector = userCache.get(userIdx);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        Collection<Integer> nearestNeighborCollection = numberNearestNeighbors > 0 ? itemNNs.get(itemIdx) : itemRatingsVector.getIndexList();

        double predictRating = 0;
        for (int nearestNeighborItemIdx : nearestNeighborCollection) {
            if (itemRatingsVector.contains(nearestNeighborItemIdx) && nearestNeighborItemIdx != excludedItemIdx) {
                double nearestNeighborPredictRating = itemRatingsVector.get(nearestNeighborItemIdx);
                predictRating += nearestNeighborPredictRating * coefficientMatrix.get(nearestNeighborItemIdx, itemIdx);
            }
        }

        return predictRating;
    }

    @Override
    protected boolean isConverged(int iter) {
        double delta_loss = lastLoss - loss;
        lastLoss = loss;

        // print out debug info
        if (verbose) {
            String recName = getClass().getSimpleName().toString();
            String info = recName + " iter " + iter + ": loss = " + loss + ", delta_loss = " + delta_loss;
            LOG.info(info);
        }

        return iter > 1 ? delta_loss < 1e-5 : false;
    }

    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive ranking score for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return predict(userIdx, itemIdx, -1);
    }
}
