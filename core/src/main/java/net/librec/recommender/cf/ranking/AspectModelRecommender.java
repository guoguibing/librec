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
 * <h3> Latent class models for collaborative filtering</h3>
 * <p>
 * This implementation refers to the method proposed by Thomas et al. at IJCAI 1999.
 * <p>
 * <strong>Tempered EM:</strong> Thomas Hofmann, <strong>Latent class models for collaborative filtering
 * </strong>, IJCAI. 1999, 99: 688-693.
 *
 * @author Haidong Zhang and Keqiang Wang
 */

public class AspectModelRecommender extends ProbabilisticGraphicalRecommender {

    /**
     * number of topics
     */
    protected int numTopics;

    /**
     * Conditional distribution: P(u|z)
     */
    protected DenseMatrix topicUserProbs, topicUserProbsSum;

    /**
     * Conditional distribution: P(i|z)
     */
    protected DenseMatrix topicItemProbs, topicItemProbsSum;

    /**
     * topic distribution: P(z)
     */
    protected DenseVector topicProbs, topicProbsSum;

    /**
     * {user, item, {topic z, probability}}
     */
    protected Table<Integer, Integer, double[]> entryTopicDistribution;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numTopics = conf.getInt("rec.topic.number", 10);
        isRanking = true;

        // Initialize topic distribution
        topicProbs = new DenseVector(numTopics);
        topicProbsSum = new DenseVector(numTopics);
        double[] probs = Randoms.randProbs(numTopics);
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            topicProbs.set(topicIdx, probs[topicIdx]);
        }

        topicUserProbs = new DenseMatrix(numTopics, numUsers);
        topicUserProbsSum = new DenseMatrix(numTopics, numUsers);
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            probs = Randoms.randProbs(numUsers);
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                topicUserProbs.set(topicIdx, userIdx, probs[userIdx]);
            }
        }

        topicItemProbs = new DenseMatrix(numTopics, numItems);
        topicItemProbsSum = new DenseMatrix(numTopics, numItems);
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            probs = Randoms.randProbs(numItems);
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                topicItemProbs.set(topicIdx, itemIdx, probs[itemIdx]);
            }
        }

        // initialize every entry topic distribution
        entryTopicDistribution = HashBasedTable.create();
        for (MatrixEntry trainMatrixEntry : trainMatrix) {
            int userIdx = trainMatrixEntry.row();
            int itemIdx = trainMatrixEntry.column();
            entryTopicDistribution.put(userIdx, itemIdx, new double[numTopics]);
        }
    }

    /*
     *
     */
    @Override
    protected void eStep() {

        for (MatrixEntry trainMatrixEntry : trainMatrix) {
            int userIdx = trainMatrixEntry.row();
            int itemIdx = trainMatrixEntry.column();

            double[] entryTopicProbs = entryTopicDistribution.get(userIdx, itemIdx);
            double sum = 0.0d;
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                double prob = topicUserProbs.get(topicIdx, userIdx) * topicItemProbs.get(topicIdx, itemIdx) * topicProbs.get(topicIdx);
                entryTopicProbs[topicIdx] = prob;
                sum += prob;
            }

            // Normalize along with the latent states
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                entryTopicProbs[topicIdx] /= sum;
            }
        }
    }

    @Override
    protected void mStep() {
        topicProbsSum.setAll(0.0);
        topicUserProbsSum.setAll(0.0);
        topicItemProbsSum.setAll(0.0);

        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            for (MatrixEntry trainMatrixEntry : trainMatrix) {
                int userIdx = trainMatrixEntry.row();
                int itemIdx = trainMatrixEntry.column();
                double num = trainMatrixEntry.get();

                double val = entryTopicDistribution.get(userIdx, itemIdx)[topicIdx] * num;
                topicProbsSum.add(topicIdx, val);
                topicUserProbsSum.add(topicIdx, userIdx, val);
                topicItemProbsSum.add(topicIdx, itemIdx, val);
            }
        }
        topicProbs = topicProbsSum.scale(1.0 / topicProbsSum.sum());
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            double userProbsSum = topicUserProbs.sumOfColumn(topicIdx);
            // avoid Nan
            userProbsSum = userProbsSum > 0.0d ? userProbsSum : 1.0d;
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                topicUserProbs.set(topicIdx, userIdx, topicUserProbsSum.get(topicIdx, userIdx) / userProbsSum);
            }
            double itemProbsSum = topicItemProbs.sumOfColumn(topicIdx);
            // avoid Nan
            itemProbsSum = itemProbsSum > 0.0d ? itemProbsSum : 1.0d;
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                topicItemProbs.set(topicIdx, itemIdx, topicItemProbsSum.get(topicIdx, itemIdx) / itemProbsSum);
            }
        }
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double predictRating = 0.0;
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            predictRating += topicUserProbs.get(topicIdx, userIdx) * topicItemProbs.get(topicIdx, itemIdx) * topicProbs.get(topicIdx);
        }
        return predictRating;
    }
}
