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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.ProbabilisticGraphicalRecommender;

import java.util.Set;

import static net.librec.math.algorithm.Gamma.digamma;

/**
 * Latent Dirichlet Allocation for implicit feedback: Tom Griffiths, <strong>Gibbs sampling in the generative model of
 * Latent Dirichlet Allocation</strong>, 2002. <br>
 * <p>
 * <p>
 * <strong>Remarks:</strong> This implementation of LDA is for implicit feedback, where users are regarded as documents
 * and items as words. To directly apply LDA to explicit ratings, Ian Porteous et al. (AAAI 2008, Section Bi-LDA)
 * mentioned that, one way is to treat items as documents and ratings as words. We did not provide such an LDA
 * implementation for explicit ratings. Instead, we provide recommender {@code URP} as an alternative LDA model for
 * explicit ratings.
 * </p>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "lda", "userTopicProbs", "topicItemProbs","trainMatrix"})
public class LDARecommender extends ProbabilisticGraphicalRecommender {

    /**
     * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
     */
    protected float initAlpha;

    /**
     * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
     */
    protected float initBeta;
    /**
     * entry[k, i]: number of tokens assigned to topic k, given item i.
     */
    protected DenseMatrix topicItemNumbers;

    /**
     * entry[u, k]: number of tokens assigned to topic k, given user u.
     */
    protected DenseMatrix userTopicNumbers;

    /**
     * entry[u, i, k]: topic assignment as sparse structure
     */
    protected Table<Integer, Integer, Integer> topicAssignments;

    /**
     * entry[u]: number of tokens rated by user u.
     */
    protected DenseVector userTokenNumbers;

    /**
     * entry[k]: number of tokens assigned to topic t.
     */
    protected DenseVector topicTokenNumbers;

    /**
     * number of topics
     */
    protected int numTopics;

    /**
     * vector of hyperparameters for alpha and beta
     */
    protected DenseVector alpha, beta;

    /**
     * cumulative statistics of theta, phi
     */
    protected DenseMatrix userTopicProbsSum, topicItemProbsSum;

    /**
     * posterior probabilities of parameters
     */
    protected DenseMatrix userTopicProbs, topicItemProbs;

    /**
     * size of statistics
     */
    protected int numStats = 0;

    /**
     * setup
     * init member method
     *
     * @throws LibrecException
     */
    protected void setup() throws LibrecException {
        super.setup();
        numIterations = conf.getInt("rec.iterator.maximum");

        userTopicProbsSum = new DenseMatrix(numUsers, numTopics);
        topicItemProbsSum = new DenseMatrix(numTopics, numItems);

        // initialize count variables.
        userTopicProbs = new DenseMatrix(numUsers, numTopics);
        userTokenNumbers = new DenseVector(numUsers);

        topicItemProbs = new DenseMatrix(numTopics, numItems);
        topicTokenNumbers = new DenseVector(numTopics);

        initAlpha = conf.getFloat("-alpha", 1.0f / numTopics);
        initBeta = conf.getFloat("-beta", 1.0f / numTopics);

        alpha = new DenseVector(numTopics);
        alpha.setAll(initAlpha);

        beta = new DenseVector(numItems);
        beta.setAll(initBeta);

        // The z_u,i are initialized to values in [0, K-1] to determine the initial state of the Markov chain.
        topicAssignments = HashBasedTable.create();
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            int topicIdx = (int) (Math.random() * numTopics); // 0 ~ k-1

            // assign a topic t to pair (u, i)
            topicAssignments.put(userIdx, itemIdx, topicIdx);

            // number of items of user u assigned to topic t.
            userTopicNumbers.add(userIdx, topicIdx, 1);
            // total number of items of user u
            userTokenNumbers.add(userIdx, 1);
            // number of instances of item i assigned to topic t
            topicItemNumbers.add(topicIdx, itemIdx, 1);
            // total number of words assigned to topic t.
            topicTokenNumbers.add(topicIdx, 1);
        }
    }

    @Override
    protected void eStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();

        // Gibbs sampling from full conditional distribution
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            int topicIdx = topicAssignments.get(userIdx, itemIdx); // topic

            userTopicNumbers.add(userIdx, topicIdx, -1);
            userTokenNumbers.add(userIdx, -1);
            topicItemNumbers.add(topicIdx, itemIdx, -1);
            topicTokenNumbers.add(topicIdx, -1);

            // do multinomial sampling via cumulative method:
            double[] p = new double[numTopics];
            for (topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                p[topicIdx] = (userTopicNumbers.get(userIdx, topicIdx) + alpha.get(topicIdx)) / (userTokenNumbers.get(userIdx)
                        + sumAlpha) * (topicItemNumbers.get(topicIdx, itemIdx) + beta.get(itemIdx))
                        / (topicTokenNumbers.get(topicIdx) + sumBeta);
            }
            // cumulating multinomial parameters
            for (topicIdx = 1; topicIdx < p.length; topicIdx++) {
                p[topicIdx] += p[topicIdx - 1];
            }
            // scaled sample because of unnormalized p[], randomly sampled a new topic t
            double rand = Math.random() * p[numTopics - 1];
            for (topicIdx = 0; topicIdx < p.length; topicIdx++) {
                if (rand < p[topicIdx])
                    break;
            }

            // add newly estimated z_i to count variables
            userTopicNumbers.add(userIdx, topicIdx, 1);
            userTokenNumbers.add(userIdx, 1);
            topicItemNumbers.add(topicIdx, itemIdx, 1);
            topicTokenNumbers.add(topicIdx, 1);

            topicAssignments.put(userIdx, itemIdx, topicIdx);
        }
    }

    @Override
    protected void mStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double topicAlpha, itemBeta;

        // update alpha vector
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {

            topicAlpha = alpha.get(topicIdx);
            double numerator = 0, denominator = 0;
            for (int itemIdx = 0; itemIdx < numUsers; itemIdx++) {
                numerator += digamma(userTopicNumbers.get(itemIdx, topicIdx) + topicAlpha) - digamma(topicAlpha);
                denominator += digamma(userTokenNumbers.get(itemIdx) + sumAlpha) - digamma(sumAlpha);
            }
            if (numerator != 0)
                alpha.set(topicIdx, topicAlpha * (numerator / denominator));
        }

        // update beta_k
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {

            itemBeta = beta.get(itemIdx);
            double numerator = 0, denominator = 0;
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                numerator += digamma(topicItemNumbers.get(topicIdx, itemIdx) + itemBeta) - digamma(itemBeta);
                denominator += digamma(topicTokenNumbers.get(topicIdx) + sumBeta) - digamma(sumBeta);
            }
            if (numerator != 0)
                beta.set(itemIdx, itemBeta * (numerator / denominator));
        }
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    protected void readoutParams() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();

        double val;
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            for (int factorIdx = 0; factorIdx < numTopics; factorIdx++) {
                val = (userTopicNumbers.get(userIdx, factorIdx) + alpha.get(factorIdx)) / (userTokenNumbers.get(userIdx) + sumAlpha);
                userTopicProbsSum.add(userIdx, factorIdx, val);
            }
        }

        for (int factorIdx = 0; factorIdx < numTopics; factorIdx++) {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                val = (topicItemNumbers.get(factorIdx, itemIdx) + beta.get(itemIdx)) / (topicTokenNumbers.get(factorIdx) + sumBeta);
                topicItemProbsSum.add(factorIdx, itemIdx, val);
            }
        }
        numStats++;
    }

    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicProbsSum.scale(1.0 / numStats);
        topicItemProbs = topicItemProbsSum.scale(1.0 / numStats);
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.product(userTopicProbs, userIdx, topicItemProbs, itemIdx);
    }
}
