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
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;
import net.librec.util.Lists;

import java.util.*;

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
@ModelData({"isRanking", "slim", "coefficientMatrix", "trainMatrix", "similarityMatrix", "knn"})
public class SLIMRecommender extends AbstractRecommender {
    /**
     * the number of iterations
     */
    protected int numIterations;

    /**
     * W in original paper, a sparse matrix of aggregation coefficients
     */
    private DenseMatrix coefficientMatrix;

    /**
     * item's nearest neighbors for kNN > 0
     */
    private Set<Integer>[] itemNNs;

    /**
     * regularization parameters for the L1 or L2 term
     */
    private float regL1Norm, regL2Norm;

    /**
     * number of nearest neighbors
     */
    protected static int knn;

    /**
     * item similarity matrix
     */
    private SymmMatrix similarityMatrix;

    /**
     * item's nearest neighbors for kNN <=0, i.e., all other items
     */
    private Set<Integer> allItems;

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        knn = conf.getInt("rec.neighbors.knn.number", 50);
        numIterations = conf.getInt("rec.iterator.maximum");
        regL1Norm = conf.getFloat("rec.slim.regularization.l1", 1.0f);
        regL2Norm = conf.getFloat("rec.slim.regularization.l2", 1.0f);

        coefficientMatrix = new DenseMatrix(numItems, numItems);
        // initial guesses: make smaller guesses (e.g., W.init(0.01)) to speed up training
        coefficientMatrix.init();
        similarityMatrix = context.getSimilarity().getSimilarityMatrix();

        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            coefficientMatrix.set(itemIdx, itemIdx, 0.0d);
        }

        createItemNNs();
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
                Set<Integer> nearestNeighborCollection = knn > 0 ? itemNNs[itemIdx] : allItems;

                double[] userRatingEntries = new double[numUsers];

                Iterator<VectorEntry> userItr = trainMatrix.rowIterator(itemIdx);
                while (userItr.hasNext()) {
                    VectorEntry userRatingEntry = userItr.next();
                    userRatingEntries[userRatingEntry.index()] = userRatingEntry.get();
                }

                // for each nearest neighbor nearestNeighborItemIdx, update coefficienMatrix by the coordinate
                // descent update rule
                for (Integer nearestNeighborItemIdx : nearestNeighborCollection) {
                    if (nearestNeighborItemIdx != itemIdx) {
                        double gradSum = 0.0d, rateSum = 0.0d, errors = 0.0d;

                        Iterator<VectorEntry> nnUserRatingItr = trainMatrix.rowIterator(nearestNeighborItemIdx);
                        if (!nnUserRatingItr.hasNext()) {
                            continue;
                        }

                        int nnCount = 0;

                        while (nnUserRatingItr.hasNext()) {
                            VectorEntry nnUserVectorEntry = nnUserRatingItr.next();
                            int nnUserIdx = nnUserVectorEntry.index();
                            double nnRating = nnUserVectorEntry.get();
                            double rating = userRatingEntries[nnUserIdx];
                            double error = rating - predict(nnUserIdx, itemIdx, nearestNeighborItemIdx);

                            gradSum += nnRating * error;
                            rateSum += nnRating * nnRating;

                            errors += error * error;
                            nnCount++;
                        }


                        gradSum /= nnCount;
                        rateSum /= nnCount;

                        errors /= nnCount;

                        double coefficient = coefficientMatrix.get(nearestNeighborItemIdx, itemIdx);
                        loss += errors + 0.5 * regL2Norm * coefficient * coefficient + regL1Norm * coefficient;


                        double update = 0.0d;
                        if (regL1Norm < Math.abs(gradSum)) {
                            if (gradSum > 0) {
                                update = (gradSum - regL1Norm) / (regL2Norm + rateSum);
                            } else {
                                // One doubt: in this case, wij<0, however, the
                                // paper says wij>=0. How to gaurantee that?
                                update = (gradSum + regL1Norm) / (regL2Norm + rateSum);
                            }
                        }

                        coefficientMatrix.set(nearestNeighborItemIdx, itemIdx, update);
                    }
                }
            }

            if (isConverged(iter) && earlyStop) {
                break;
            }
        }
    }


    /**
     * predict a specific ranking score for user userIdx on item itemIdx.
     *
     * @param userIdx         user index
     * @param itemIdx         item index
     * @param excludedItemIdx excluded item index
     * @return a prediction without the contribution of excluded item
     */
    protected double predict(int userIdx, int itemIdx, int excludedItemIdx) {
        double predictRating = 0;
        Iterator<VectorEntry> itemEntryIterator = trainMatrix.colIterator(userIdx);
        while (itemEntryIterator.hasNext()) {
            VectorEntry itemEntry = itemEntryIterator.next();
            int nearestNeighborItemIdx = itemEntry.index();
            double nearestNeighborPredictRating = itemEntry.get();
            if (itemNNs[itemIdx].contains(nearestNeighborItemIdx) && nearestNeighborItemIdx != excludedItemIdx) {
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
//        create item knn list if not exists,  for local offline model
        if (!(null != itemNNs && itemNNs.length > 0)) {
            createItemNNs();
        }
        return predict(userIdx, itemIdx, -1);
    }


    /**
     * Create item KNN list.
     */
    public void createItemNNs() {
        itemNNs = new HashSet[numItems];

        // find the nearest neighbors for each item based on item similarity
        List<Map.Entry<Integer, Double>> tempItemSimList;
        if (knn > 0) {
            for (int itemIdx = 0; itemIdx < numItems; ++itemIdx) {
                SparseVector similarityVector = similarityMatrix.row(itemIdx);
                if (knn < similarityVector.size()) {
                    tempItemSimList = new ArrayList<>(similarityVector.size() + 1);
                    Iterator<VectorEntry> simItr = similarityVector.iterator();
                    while (simItr.hasNext()) {
                        VectorEntry simVectorEntry = simItr.next();
                        tempItemSimList.add(new AbstractMap.SimpleImmutableEntry<>(simVectorEntry.index(), simVectorEntry.get()));
                    }
                    tempItemSimList = Lists.sortListTopK(tempItemSimList, true, knn);
                    itemNNs[itemIdx] = new HashSet<>((int) (tempItemSimList.size() / 0.5));
                    for (Map.Entry<Integer, Double> tempItemSimEntry : tempItemSimList) {
                        itemNNs[itemIdx].add(tempItemSimEntry.getKey());
                    }
                } else {
                    itemNNs[itemIdx] = similarityVector.getIndexSet();
                }
            }
        } else {
            allItems = new HashSet<>(trainMatrix.columns());
        }
    }
}
