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
package net.librec.recommender.rec.cf.ranking;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Sims;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.item.RecommendedList;
import net.librec.util.Lists;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Xia Ning and George Karypis, <strong>SLIM: Sparse Linear Methods for Top-N Recommender Systems</strong>, ICDM 2011. <br>
 * <p>
 * <p>
 * Related Work:
 * <ul>
 * <li>Levy and Jack, Efficient Top-N Recommendation by Linear Regression, ISRS 2013. This paper reports experimental
 * results on the MovieLens (100K, 10M) and Epinions datasets in terms of precision, MRR and HR@N (i.e., Recall@N).</li>
 * <li>Friedman et al., Regularization Paths for Generalized Linear Models via Coordinate Descent, Journal of
 * Statistical Software, 2010.</li>
 * </ul>
 * </p>
 * @author guoguibing and Keqiang Wang
 */
public class SLIMRecommender extends AbstractRecommender {
    /**
     * the number of iterations
     */
    protected int numIterations;

    private DenseMatrix coefficienMatrix;

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
     * similarity measure
     */
    protected static String similarityMeasure;

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
    protected static String cacheSpec;

    /**
     * initialization
     *
     * @throws LibrecException
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        regL1Norm = conf.getFloat("rec.slim.regularization.l1");
        regL2Norm = conf.getFloat("rec.slim.regularization.l2");

        coefficienMatrix = new DenseMatrix(numItems, numItems);
        coefficienMatrix.init(); // initial guesses: make smaller guesses (e.g., W.init(0.01)) to speed up training
        cacheSpec = conf.get("guava.cache.spec", "maximumSize=200,expireAfterAccess=2m");

        similarityMeasure = conf.get("similarity", "PCC");


        userCache = trainMatrix.rowCache(cacheSpec);

        if (numberNearestNeighbors > 0) {
            // find the nearest neighbors for each item based on item similarity
            SymmMatrix itemCorrs = buildCorrs(false);
            itemNNs = HashMultimap.create();

            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // set diagonal entries to 0
                coefficienMatrix.set(itemIdx, itemIdx, 0);
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
                coefficienMatrix.set(itemIdx, itemIdx, 0.0);
        }
    }

    /**
     * train model
     *
     * @throws LibrecException
     * @throws ExecutionException
     */
    @Override
    protected void trainModel() throws LibrecException {
        // number of iteration cycles
        for (int iter = 1; iter <= numIterations; iter++) {
            // each cycle iterates through one coordinate direction
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // find k-nearest neighbors
                Collection<Integer> nearestNeighborCollection = numberNearestNeighbors > 0 ? itemNNs.get(itemIdx) : allItems;

                // for each nearest neighbor nearestNeighborItemIdx, update coefficienMatrix by the coordinate
                // descent update rule
                // it is OK if nearestNeighborItemIdx==itemIdx, since coefficienMatrix  = 0;
                for (Integer nearestNeighborItemIdx : nearestNeighborCollection) {
                	if (nearestNeighborItemIdx != itemIdx) {
	                    double gradSum = 0, rateSum = 0;
	
	                    SparseVector nnUserRatingsVector = trainMatrix.column(nearestNeighborItemIdx);
	                    int nnCount = nnUserRatingsVector.getCount();
	                    for (VectorEntry nnUserVectorEntry : nnUserRatingsVector) {
	                        int nnUserIdx = nnUserVectorEntry.index();
	                        double nnRating = nnUserVectorEntry.get();
	                        double rating = trainMatrix.get(nnUserIdx, itemIdx);
	                        double error = rating - predict(nnUserIdx, itemIdx, nearestNeighborItemIdx);
	
	                        gradSum += nnRating * error;
	                        rateSum += nnRating * nnRating;
	                    }
	                    gradSum /= nnCount;
	                    rateSum /= nnCount;
	                    if (regL1Norm < Math.abs(gradSum)) {
	                        if (gradSum > 0) {
	                            double update = (gradSum - regL1Norm) / (regL2Norm + rateSum);
	                            coefficienMatrix.set(nearestNeighborItemIdx, itemIdx, update);
	                        } else {
	                            // One doubt: in this case, wij<0, however, the
	                            // paper says wij>=0. How to gaurantee that?
	                            double update = (gradSum + regL1Norm) / (regL2Norm + rateSum);
	                            coefficienMatrix.set(nearestNeighborItemIdx, itemIdx, update);
	                        }
	                    } else {
	                        coefficienMatrix.set(nearestNeighborItemIdx, itemIdx, 0.0);
	                    }
	                }
				}
            }
        }
    }


    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
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
                predictRating += nearestNeighborPredictRating * coefficienMatrix.get(nearestNeighborItemIdx, itemIdx);
            }
        }

        return predictRating;
    }

    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive ranking score for user userIdx on item itemIdx
     * @throws LibrecException
     */
    @Override
    protected double predict(int userIdx, int itemIdx) {
        return predict(userIdx, itemIdx, -1);
    }


    /**
     * build user-user or item-item correlation matrix from training data
     *
     * @param isUser whether it is user-user correlation matrix
     * @return a upper symmetric matrix with user-user or item-item coefficients
     */
    protected SymmMatrix buildCorrs(boolean isUser) {
        int count = isUser ? numUsers : numItems;
        SymmMatrix corrs = new SymmMatrix(count);

        for (int index = 0; index < count; index++) {
            SparseVector vector = isUser ? trainMatrix.row(index) : trainMatrix.column(index);
            if (vector.getCount() == 0)
                continue;
            // user/item itself exclusive
            for (int comparedIndex = index + 1; comparedIndex < count; comparedIndex++) {
                SparseVector comparedVector = isUser ? trainMatrix.row(comparedIndex) : trainMatrix.column(comparedIndex);

                double similarity = correlation(vector, comparedVector);

                if (!Double.isNaN(similarity))
                    corrs.set(index, comparedIndex, similarity);
            }
        }
        return corrs;
    }

    /**
     * Compute the correlation between two vectors using method specified by configuration key "similarity"
     *
     * @param vector         vector
     * @param comparedVector vector
     * @return the correlation between vectors vector and comparedVector
     */
    protected double correlation(SparseVector vector, SparseVector comparedVector) {
        return correlation(vector, comparedVector, similarityMeasure);
    }

    /**
     * Compute the correlation between two vectors for a specific method
     *
     * @param vector         vector
     * @param comparedVector vector
     * @param method         similarity method
     * @return the correlation between vectors vector and comparedVector; return NaN if the correlation is not computable.
     */
    protected double correlation(SparseVector vector, SparseVector comparedVector, String method) {

        // compute similarity
        List<Double> vectorList = new ArrayList<>();
        List<Double> comparedVectorList = new ArrayList<>();

        for (Integer idx : comparedVector.getIndex()) {
            if (vector.contains(idx)) {
                vectorList.add(vector.get(idx));
                comparedVectorList.add(comparedVector.get(idx));
            }
        }

        double similarity = 0;
        switch (method.toLowerCase()) {
            case "cos":
                // for ratings along the overlappings
                similarity = Sims.cos(vectorList, comparedVectorList);
                break;
            case "cos-binary":
                // for ratings along all the vectors (including one-sided 0s)
                similarity = vector.inner(comparedVector) / (Math.sqrt(vector.inner(vector)) * Math.sqrt(comparedVector.inner(comparedVector)));
                break;
            case "msd":
                similarity = Sims.msd(vectorList, comparedVectorList);
                break;
            case "cpc":
                similarity = Sims.cpc(vectorList, comparedVectorList, (minRate + maxRate) / 2.0);
                break;
            case "exjaccard":
                similarity = Sims.exJaccard(vectorList, comparedVectorList);
                break;
            case "pcc":
            default:
                similarity = Sims.pcc(vectorList, comparedVectorList);
                break;
        }

        // shrink to account for vector size
        if (!Double.isNaN(similarity)) {
            int size = vectorList.size();
            int shrinkage = conf.getInt("num.shrinkage");
            if (shrinkage > 0)
                similarity *= size / (size + shrinkage + 0.0);
        }

        return similarity;
    }
}
