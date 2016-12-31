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
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.ProbabilisticGraphicalRecommender;

/**
 * Thomas Hofmann, <strong>Latent semantic models for collaborative filtering</strong>,
 * ACM Transactions on Information Systems.
 * 2004. <br>
 *
 * @author Haidong Zhang and Keqiang Wang
 */

public class PLSARecommender extends ProbabilisticGraphicalRecommender {

    /**
     * number of latent topics
     */
    protected int numTopics;

    /**
     * {user, item, {topic z, probability}}
     */
    protected Table<Integer, Integer, double[]> Q;

    /**
     * Conditional Probability: P(z|u)
     */
    protected DenseMatrix userTopicProbs, userTopicProbsSum;

    /**
     * Conditional Probability: P(i|z)
     */
    protected DenseMatrix topicItemProbs, topicItemProbsSum;

    /**
     * topic probability sum value
     */
    private DenseVector topicProbsSum;

    /**
     * entry[u]: number of tokens rated by user u.
     */
    protected DenseVector numItemsRateByUser;


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numTopics = conf.getInt("rec.topic.number", 10);
        isRanking = true;

        userTopicProbs = new DenseMatrix(numUsers, numTopics);
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            double[] probs = Randoms.randProbs(numTopics);
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                userTopicProbs.set(userIdx, topicIdx, probs[topicIdx]);
            }
        }

        topicItemProbs = new DenseMatrix(numTopics, numItems);
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double[] probs = Randoms.randProbs(numItems);
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                topicItemProbs.set(topicIdx, itemIdx, probs[itemIdx]);
            }
        }

        // initialize Q

        // initialize Q
        Q = HashBasedTable.create();
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            Q.put(userIdx, itemIdx, new double[numTopics]);
        }

        numItemsRateByUser = new DenseVector(numUsers);
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            numItemsRateByUser.add(userIdx, matrixEntry.get());
        }
    }

    @Override
    protected void eStep() {
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();

            double[] topicDistr = Q.get(userIdx, itemIdx);
            double sum = 0.0;
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                double value = userTopicProbs.get(userIdx, topicIdx) * topicItemProbs.get(topicIdx, itemIdx);
                topicDistr[topicIdx] = value;
                sum += value;
            }

            sum = sum > 0.0 ? sum : 1.0d;
            // Normalize along with the latent states
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                topicDistr[topicIdx] /= sum;
            }
        }
    }

    @Override
    protected void mStep() {
        userTopicProbsSum = new DenseMatrix(numUsers, numTopics);
        topicItemProbsSum = new DenseMatrix(numTopics, numItems);
        topicProbsSum = new DenseVector(numTopics);

        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            double num = matrixEntry.get();
            double[] topicDistr = Q.get(userIdx, itemIdx);
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                double val = topicDistr[topicIdx] * num;
                userTopicProbsSum.add(userIdx, topicIdx, val);
                topicItemProbsSum.add(topicIdx, itemIdx, val);
                topicProbsSum.add(topicIdx, val);
            }
        }

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            double deno = numItemsRateByUser.get(userIdx);
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                double value = deno > 0.0d ? userTopicProbsSum.get(userIdx, topicIdx) / numItemsRateByUser.get(userIdx) : 0.0d;
                userTopicProbs.set(userIdx, topicIdx, value);
            }
        }

        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double itemTopicProbsSum = topicProbsSum.get(topicIdx);
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                double value = itemTopicProbsSum > 0.0d ? topicItemProbsSum.get(topicIdx, itemIdx) / itemTopicProbsSum : 0.0d;
                topicItemProbs.set(topicIdx, itemIdx, value);
            }
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.product(userTopicProbs, userIdx, topicItemProbs, itemIdx);
    }
}
