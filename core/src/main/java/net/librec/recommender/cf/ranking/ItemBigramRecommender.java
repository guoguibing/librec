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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.ProbabilisticGraphicalRecommender;
import net.librec.util.RatingContext;

import java.util.*;

import static net.librec.math.algorithm.Gamma.digamma;

/**
 * Hanna M. Wallach, <strong>Topic Modeling: Beyond Bag-of-Words</strong>, ICML 2006.
 *
 * @author Keqiang Wang
 **/
@ModelData({"isRanking", "itembigram", "userTopicProbs", "topicPreItemCurItemProbs"})
public class ItemBigramRecommender extends ProbabilisticGraphicalRecommender {

    private Map<Integer, List<Integer>> userItemsMap;

    /**
     * k: current topic; j: previously rated item; i: current item
     */
    private int[][][] topicPreItemCurItemNum;
    private DenseMatrix topicItemProbs;
    private double[][][] topicPreItemCurItemProbs, topicPreItemCurItemSumProbs;

    private DenseMatrix beta;

    /**
     * vector of hyperparameters for alpha
     */
    protected DenseVector alpha;

    /**
     * number of topics
     */
    protected int numTopics;

    /**
     * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
     */
    protected float initAlpha;

    /**
     * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
     */
    protected float initBeta;

    /**
     * cumulative statistics of theta, phi
     */
    protected DenseMatrix userTopicProbsSum;

    /**
     * entry[u, k]: number of tokens assigned to topic k, given user u.
     */
    protected DenseMatrix userTopicNumbers;

    /**
     * entry[u]: number of tokens rated by user u.
     */
    protected DenseVector userTokenNumbers;

    /**
     * posterior probabilities of parameters
     */
    protected DenseMatrix userTopicProbs;

    /**
     * entry[u, i, k]: topic assignment as sparse structure
     */
    protected Table<Integer, Integer, Integer> topicAssignments;

    /**
     * time sparse matrix
     */
    private SparseMatrix timeMatrix;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numTopics = conf.getInt("rec.topic.number", 10);

        initAlpha = conf.getFloat("rec.user.dirichlet.prior", 0.01f);
        initBeta = conf.getFloat("rec.topic.dirichlet.prior", 0.01f);

        timeMatrix =  (SparseMatrix) getDataModel().getDatetimeDataSet();

        // build the training data, sorting by date
        userItemsMap = new HashMap<>();
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            List<Integer> unsortedItems = trainMatrix.getColumns(userIdx);
            int size = unsortedItems.size();

            List<RatingContext> rcs = new ArrayList<>(size);
            for (Integer itemIdx : unsortedItems) {
                rcs.add(new RatingContext(userIdx, itemIdx, (long) timeMatrix.get(userIdx, itemIdx)));
            }
            Collections.sort(rcs);

            List<Integer> sortedItems = new ArrayList<>(size);
            for (RatingContext rc : rcs) {
                sortedItems.add(rc.getItem());
            }

            userItemsMap.put(userIdx, sortedItems);
        }

        // count variables
        // initialize count variables.
        userTopicNumbers = new DenseMatrix(numUsers, numTopics);
        userTokenNumbers = new DenseVector(numUsers);

        topicPreItemCurItemNum = new int[numTopics][numItems + 1][numItems];
        topicItemProbs = new DenseMatrix(numTopics, numItems + 1);

        // Logs.debug("topicPreItemCurItemNum consumes {} bytes", Strings.toString(Memory.bytes(topicPreItemCurItemNum)));

        // parameters
        userTopicProbsSum = new DenseMatrix(numUsers, numTopics);
        topicPreItemCurItemSumProbs = new double[numTopics][numItems + 1][numItems];
        topicPreItemCurItemProbs = new double[numTopics][numItems + 1][numItems];

        // hyper-parameters
        alpha = new DenseVector(numTopics);
        alpha.setAll(initAlpha);

        beta = new DenseMatrix(numTopics, numItems + 1);
        beta.setAll(initBeta);

        // initialization
        topicAssignments = HashBasedTable.create();
        for (Map.Entry<Integer, List<Integer>> userItemEntry : userItemsMap.entrySet()) {
            int userIdx = userItemEntry.getKey();
            List<Integer> itemIdxList = userItemEntry.getValue();

            for (int itemIdxIndex = 0; itemIdxIndex < itemIdxList.size(); itemIdxIndex++) {
                int itemIdx = itemIdxList.get(itemIdxIndex);

                int topicIdx = (int) (Math.random() * numTopics);
                topicAssignments.put(userIdx, itemIdx, topicIdx);

                userTopicNumbers.add(userIdx, topicIdx, 1.0);
                userTokenNumbers.add(userIdx, 1.0);

                int preItemIdx = itemIdxIndex > 0 ? itemIdxList.get(itemIdxIndex - 1) : numItems;
                topicPreItemCurItemNum[topicIdx][preItemIdx][itemIdx]++;
                topicItemProbs.add(topicIdx, preItemIdx, 1);
            }
        }
    }

    @Override
    protected void eStep() {
        double sumAlpha = alpha.sum();
        double tempValue1, tempValue2;

        for (Map.Entry<Integer, List<Integer>> userItemEntry : userItemsMap.entrySet()) {
            int userIdx = userItemEntry.getKey();
            List<Integer> items = userItemEntry.getValue();

            for (int itemIdxIndex = 0; itemIdxIndex < items.size(); itemIdxIndex++) {
                int itemIdx = items.get(itemIdxIndex);
                int topicIdx = topicAssignments.get(userIdx, itemIdx);

                userTopicNumbers.add(userIdx, topicIdx, -1.0);
                userTokenNumbers.add(userIdx, -1.0);

                int preItemIdx = itemIdxIndex > 0 ? items.get(itemIdxIndex - 1) : numItems;
                topicPreItemCurItemNum[topicIdx][preItemIdx][itemIdx]--;
                topicItemProbs.add(topicIdx, preItemIdx, -1);

                double[] tempUserProbs = new double[numTopics];
                for (int topicInIdx = 0; topicInIdx < numTopics; topicInIdx++) {
                    tempValue1 = (userTopicNumbers.get(userIdx, topicIdx) + alpha.get(topicInIdx)) / (userTokenNumbers.get(userIdx) + sumAlpha);
                    tempValue2 = (topicPreItemCurItemNum[topicInIdx][preItemIdx][itemIdx] + beta.get(topicInIdx, preItemIdx))
                            / (topicItemProbs.get(topicInIdx, preItemIdx) + beta.sumOfRow(topicInIdx));

                    tempUserProbs[topicInIdx] = tempValue1 * tempValue2;
                }

                for (int topicInIdx = 1; topicInIdx < numTopics; topicInIdx++) {
                    tempUserProbs[topicInIdx] += tempUserProbs[topicInIdx - 1];
                }

                double rand = Randoms.uniform() * tempUserProbs[numTopics - 1];
                for (topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                    if (rand < tempUserProbs[topicIdx])
                        break;
                }

                topicAssignments.put(userIdx, itemIdx, topicIdx);

                userTopicNumbers.add(userIdx, topicIdx, 1.0d);
                userTokenNumbers.add(userIdx, 1.0d);

                topicPreItemCurItemNum[topicIdx][preItemIdx][itemIdx]++;
                topicItemProbs.add(topicIdx, preItemIdx, 1.0d);
            }
        }
    }

    @Override
    protected void mStep() {
        double sumAlpha = alpha.sum();
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double alphaTopicValue = alpha.get(topicIdx);
            double numerator = 0, denominator = 0;
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                numerator += digamma(userTopicNumbers.get(userIdx, topicIdx) + alphaTopicValue) - digamma(alphaTopicValue);
                denominator += digamma(userTokenNumbers.get(userIdx) + sumAlpha) - digamma(sumAlpha);
            }

            if (numerator != 0)
                alpha.set(topicIdx, alphaTopicValue * (numerator / denominator));
        }

        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double betaTopicValue = beta.sumOfRow(topicIdx);
            for (int itemIdx = 0; itemIdx < numItems + 1; itemIdx++) {
                double betaTopicItemValue = beta.get(topicIdx, itemIdx);
                double numerator = 0.0d, denominator = 0.0d;
                for (int preItemIdx = 0; preItemIdx < numItems; preItemIdx++) {
                    numerator += digamma(topicPreItemCurItemNum[topicIdx][itemIdx][preItemIdx] + betaTopicItemValue) - digamma(betaTopicItemValue);
                    denominator += digamma(topicItemProbs.get(topicIdx, itemIdx) + betaTopicValue) - digamma(betaTopicValue);
                }

                if (numerator != 0)
                    beta.set(topicIdx, itemIdx, betaTopicItemValue * (numerator / denominator));
            }
        }
    }

    @Override
    protected void readoutParams() {
        double val;
        double sumAlpha = alpha.sum();

        for (int userIdx = 0; userIdx < numTopics; userIdx++) {
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                val = (userTopicNumbers.get(userIdx, topicIdx) + alpha.get(topicIdx)) / (userTokenNumbers.get(userIdx) + sumAlpha);
                userTopicProbsSum.add(userIdx, topicIdx, val);
            }
        }

        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double betaTopicValue = beta.sumOfRow(topicIdx);
            for (int itemIdx = 0; itemIdx < numItems + 1; itemIdx++) {
                for (int preItemIdx = 0; preItemIdx < numItems; preItemIdx++) {
                    val = (topicPreItemCurItemNum[topicIdx][itemIdx][preItemIdx] + beta.get(topicIdx, itemIdx)) / (topicItemProbs.get(topicIdx, itemIdx) + betaTopicValue);
                    topicPreItemCurItemSumProbs[topicIdx][itemIdx][preItemIdx] += val;
                }
            }
        }

        numStats++;
    }

    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicProbsSum.scale(1.0 / numStats);

        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            for (int itemIdx = 0; itemIdx < numItems + 1; itemIdx++) {
                for (int preItemIdx = 0; preItemIdx < numItems; preItemIdx++) {
                    topicPreItemCurItemProbs[topicIdx][itemIdx][preItemIdx] = topicPreItemCurItemSumProbs[topicIdx][itemIdx][preItemIdx] / numStats;
                }
            }
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        List<Integer> items = userItemsMap.get(userIdx);
        int preItemIdx = items.size() < 1 ? numItems : items.get(items.size() - 1); // last rated item

        double predictRating = 0;
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            predictRating += userTopicProbs.get(userIdx, topicIdx) * topicPreItemCurItemProbs[topicIdx][preItemIdx][itemIdx];
        }

        return predictRating;
    }
}
