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
package net.librec.recommender.cf;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.ProbabilisticGraphicalRecommender;

import static net.librec.math.algorithm.Gamma.digamma;

/**
 * Bayesian UCM: Nicola Barbieri et al., <strong>Modeling Item Selection and Relevance for Accurate Recommendations: a
 * Bayesian Approach</strong>, RecSys 2011.
 * <p>
 * Thank the paper authors for providing source code and for having valuable discussion.
 *
 * @author Guo Guibing and Haidong Zhang
 */
public class BUCMRecommender extends ProbabilisticGraphicalRecommender {
    /**
     * number of occurrences of entry (t, i, r)
     */
    private int[][][] topicItemRatingNum;

    /**
     * number of occurrentces of entry (user, topic)
     */
    private DenseMatrix userTopicNum;

    /**
     * number of occurences of users
     */
    private DenseVector userNum;

    /**
     * number of occurrences of entry (topic, item)
     */
    private DenseMatrix topicItemNum;

    /**
     * number of occurrences of items
     */
    private DenseVector topicNum;

    /**
     * cumulative statistics of probabilities of (t, i, r)
     */
    private double[][][] topicItemRatingSumProbs;

    /**
     * posterior probabilities of parameters epsilon_{k, i, r}
     */
    private double[][][] topicItemRatingProbs;

    /**
     * P(k | u)
     */
    private DenseMatrix userTopicProbs, userTopicSumProbs;

    /**
     * P(i | k)
     */
    private DenseMatrix topicItemProbs, topicItemSumProbs;

    /**
     *
     */
    private DenseVector alpha;

    /**
     *
     */
    private DenseVector beta;

    /**
     *
     */
    private DenseVector gamma;

    /**
     *
     */
    protected Table<Integer, Integer, Integer> topics;

    /**
     * number of topics
     */
    protected int numTopics;

    /**
     *
     */
    protected int numRatingLevels;


    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numTopics = conf.getInt("rec.pgm.topic.number", 10);
        numRatingLevels = trainMatrix.getValueSet().size();

        // cumulative parameters
        userTopicSumProbs = new DenseMatrix(numUsers, numTopics);
        topicItemSumProbs = new DenseMatrix(numTopics, numItems);
        topicItemRatingSumProbs = new double[numTopics][numItems][numRatingLevels];

        // initialize count varialbes
        userTopicNum = new DenseMatrix(numUsers, numTopics);
        userNum = new DenseVector(numUsers);

        topicItemNum = new DenseMatrix(numTopics, numItems);
        topicNum = new DenseVector(numTopics);

        topicItemRatingNum = new int[numTopics][numItems][numRatingLevels];

        double initAlpha = conf.getDouble("rec.bucm.alpha", 1.0 / numTopics);
        alpha = new DenseVector(numTopics);
        alpha.setAll(initAlpha);

        double initBeta = conf.getDouble("re.bucm.beta", 1.0 / numItems);
        beta = new DenseVector(numItems);
        beta.setAll(initBeta);

        double initGamma = conf.getDouble("rec.bucm.gamma", 1.0 / numTopics);
        gamma = new DenseVector(numRatingLevels);
        gamma.setAll(initGamma);

        // initialize topics
        topics = HashBasedTable.create();
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rating = me.get();

            int r = ratingScale.indexOf(rating);  // rating level 0 ~ numLevels
            int t = (int) (Randoms.uniform() * numTopics);    // 0 ~ k-1

            // Assign a topic t to pair (u, i)
            topics.put(u, i, t);
            // for users
            userTopicNum.add(u, t, 1);
            userNum.add(u, 1);

            // for items
            topicItemNum.add(t, i, 1);
            topicNum.add(t, 1);

            // for ratings
            topicItemRatingNum[t][i][r]++;
        }
    }

    @Override
    protected void eStep() {

        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double sumGamma = gamma.sum();

        // collapse Gibbs sampling
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rating = me.get();

            int r = ratingScale.indexOf(rating); // rating level 0 ~ numLevels
            int t = topics.get(u, i);

            // for user
            userTopicNum.add(u, t, -1);
            userNum.add(u, -1);

            // for item
            topicItemNum.add(t, i, -1);
            topicNum.add(t, -1);

            // for rating
            topicItemRatingNum[t][i][r]--;

            // do multinomial sampling via cumulative method:
            double[] p = new double[numTopics];

            double v1, v2, v3;
            for (int k = 0; k < numTopics; k++) {

                v1 = (userTopicNum.get(u, k) + alpha.get(k)) / (userNum.get(u) + sumAlpha);
                v2 = (topicItemNum.get(k, i) + beta.get(i)) / (topicNum.get(k) + sumBeta);
                v3 = (topicItemRatingNum[k][i][r] + gamma.get(r)) / (topicItemNum.get(k, i) + sumGamma);

                p[k] = v1 * v2 * v3;
            }

            // cumulate multinomial parameters
            for (int k = 1; k < numTopics; k++) {
                p[k] += p[k - 1];
            }

            // scaled sample because of unnormalized p[], randomly sampled a new topic t
            double rand = Randoms.uniform() * p[numTopics - 1];
            for (t = 0; t < numTopics; t++) {
                if (rand < p[t])
                    break;
            }

            // new topic t
            topics.put(u, i, t);

            // add newly estimated z_i to count variables
            userTopicNum.add(u, t, 1);
            userNum.add(u, 1);

            topicItemNum.add(t, i, 1);
            topicNum.add(t, 1);

            topicItemRatingNum[t][i][r]++;
        }
    }

    /**
     * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
     */
    @Override
    protected void mStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double sumGamma = gamma.sum();
        double ak, bi, gr;

        // update alpha
        for (int k = 0; k < numTopics; k++) {

            ak = alpha.get(k);
            double numerator = 0, denominator = 0;
            for (int u = 0; u < numUsers; u++) {
                numerator += digamma(userTopicNum.get(u, k) + ak) - digamma(ak);
                denominator += digamma(userNum.get(u) + sumAlpha) - digamma(sumAlpha);
            }

            if (numerator != 0)
                alpha.set(k, ak * (numerator / denominator));
        }

        // update beta
        for (int i = 0; i < numItems; i++) {

            bi = beta.get(i);
            double numerator = 0, denominator = 0;
            for (int k = 0; k < numTopics; k++) {
                numerator += digamma(topicItemNum.get(k, i) + bi) - digamma(bi);
                denominator += digamma(topicNum.get(k) + sumBeta) - digamma(sumBeta);
            }
            if (numerator != 0)
                beta.set(i, bi * (numerator / denominator));
        }

        // update gamma
        for (int r = 0; r < numRatingLevels; r++) {
            gr = gamma.get(r);
            double numerator = 0, denominator = 0;
            for (int i = 0; i < numItems; i++) {
                for (int k = 0; k < numTopics; k++) {
                    numerator += digamma(topicItemRatingNum[k][i][r] + gr) - digamma(gr);
                    denominator += digamma(topicItemNum.get(k, i) + sumGamma) - digamma(sumGamma);
                }
            }
            if (numerator != 0)
                gamma.set(r, gr * (numerator / denominator));
        }
    }

    @Override
    protected boolean isConverged(int iter) {
        double loss = 0;

        // get params
        estimateParams();

        // compute likelihood
        int count = 0;
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rui = me.get();
            int r = ratingScale.indexOf(rui);

            double prob = 0;
            for (int k = 0; k < numTopics; k++) {
                prob += userTopicProbs.get(u, k) * topicItemProbs.get(k, i) * topicItemRatingProbs[k][i][r];
            }

            loss += -Math.log(prob);
            count++;
        }
        loss /= count;

        double delta = loss - lastLoss; // loss gets smaller, delta <= 0

        if (numStats > 1 && delta > 0) {
            return true;
        }

        lastLoss = loss;
        return false;
    }


    protected void readoutParams() {
        double val;
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double sumGamma = gamma.sum();

        for (int u = 0; u < numUsers; u++) {
            for (int k = 0; k < numTopics; k++) {
                val = (userTopicNum.get(u, k) + alpha.get(k)) / (userNum.get(u) + sumAlpha);
                userTopicSumProbs.add(u, k, val);
            }
        }

        for (int k = 0; k < numTopics; k++) {
            for (int i = 0; i < numItems; i++) {
                val = (topicItemNum.get(k, i) + beta.get(i)) / (topicNum.get(k) + sumBeta);
                topicItemSumProbs.add(k, i, val);
            }
        }

        for (int k = 0; k < numTopics; k++) {
            for (int i = 0; i < numItems; i++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    val = (topicItemRatingNum[k][i][r] + gamma.get(r)) / (topicItemNum.get(k, i) + sumGamma);
                    topicItemRatingSumProbs[k][i][r] += val;
                }
            }
        }
        numStats++;
    }

    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicSumProbs.scale(1.0 / numStats);
        topicItemProbs = topicItemSumProbs.scale(1.0 / numStats);

        topicItemRatingProbs = new double[numTopics][numItems][numRatingLevels];
        for (int k = 0; k < numTopics; k++) {
            for (int i = 0; i < numItems; i++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    topicItemRatingProbs[k][i][r] = topicItemRatingSumProbs[k][i][r] / numStats;
                }
            }
        }
    }

    protected double perplexity(int u, int j, double ruj) throws Exception {
        int r = (int) (ruj / minRate) - 1;

        double prob = 0;
        for (int k = 0; k < numTopics; k++) {
            prob += userTopicProbs.get(u, k) * topicItemProbs.get(k, j) * topicItemRatingProbs[k][j][r];
        }

        return -Math.log(prob);
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        if (isRanking) {
            return predictRanking(userIdx, itemIdx);
        } else {
            return predictRating(userIdx, itemIdx);
        }
    }

    protected double predictRating(int userIdx, int itemIdx) {
        double pred = 0, probs = 0;

        for (int r = 0; r < numRatingLevels; r++) {
            double rate = ratingScale.get(r);

            double prob = 0;
            for (int k = 0; k < numTopics; k++) {
                prob += userTopicProbs.get(userIdx, k) * topicItemProbs.get(k, itemIdx) * topicItemRatingProbs[k][itemIdx][r];
            }

            pred += prob * rate;
            probs += prob;
        }

        return pred / probs;
    }

    protected double predictRanking(int userIdx, int itemIdx) {
        double rankScore = 0;

        for (int topicIdx = 0; topicIdx < numTopics; ++topicIdx) {

            double sum = 0;
            for (int rateIdx = 0; rateIdx < numRatingLevels; ++rateIdx) {
                double rate = ratingScale.get(rateIdx);
                if (rate > globalMean) {
                    sum += topicItemRatingProbs[topicIdx][itemIdx][rateIdx];
                }
            }

            rankScore += userTopicProbs.get(userIdx, topicIdx) * topicItemProbs.get(topicIdx, itemIdx) * sum;
        }

        return rankScore;
    }
}
