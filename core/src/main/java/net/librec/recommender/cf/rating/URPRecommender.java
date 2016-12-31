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
package net.librec.recommender.cf.rating;

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
 * User Rating Profile: a LDA model for rating prediction. <br>
 * <p>
 * Benjamin Marlin, <strong>Modeling user rating profiles for collaborative filtering</strong>, NIPS 2003.<br>
 * <p>
 * Nicola Barbieri, <strong>Regularized gibbs sampling for user profiling with soft constraints</strong>, ASONAM 2011.
 *
 * @author Guo Guibing and Haidong Zhang
 */
public class URPRecommender extends ProbabilisticGraphicalRecommender {
    private double preRMSE;

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
     * P(k | u)
     */
    private DenseMatrix userTopicProbs, userTopicSumProbs;

    /**
     * user parameters
     */
    private DenseVector alpha;

    /**
     * item parameters
     */
    private DenseVector beta;

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
    /**
     * number of occurrences of entry (t, i, r)
     */
    private int[][][] topicItemRatingNum;  // Nkir

    /**
     * cumulative statistics of probabilities of (t, i, r)
     */
    private double[][][] topicItemRatingSumProbs;    //PkirSum;

    /**
     * posterior probabilities of parameters phi_{k, i, r}
     */
    protected double[][][] topicItemRatingProbs;    //Pkir;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numTopics = conf.getInt("rec.pgm.number", 10);
        numRatingLevels = trainMatrix.getValueSet().size();

        // cumulative parameters
        userTopicSumProbs = new DenseMatrix(numUsers, numTopics);
        topicItemRatingSumProbs = new double[numTopics][numItems][numRatingLevels];

        // initialize count variables
        userTopicNum = new DenseMatrix(numUsers, numTopics);
        userNum = new DenseVector(numUsers);

        topicItemRatingNum = new int[numTopics][numItems][numRatingLevels];
        topicItemNum = new DenseMatrix(numTopics, numItems);

        alpha = new DenseVector(numTopics);
        double initAlpha = conf.getDouble("rec.pgm.bucm.alpha", 1.0 / numTopics);
        alpha.setAll(initAlpha);

        beta = new DenseVector(numRatingLevels);
        double initBeta = conf.getDouble("rec.pgm.bucm.beta", 1.0 / numTopics);
        beta.setAll(initBeta);

        // initialize topics
        topics = HashBasedTable.create();
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rui = me.get();

            int r = ratingScale.indexOf(rui);  // rating level 0 ~ numLevels
            int t = (int) (Randoms.uniform() * numTopics); // 0 ~ k-1

            // Assign a topic t to pair (u, i)
            topics.put(u, i, t);
            // number of pairs (u, t) in (u, i, t)
            userTopicNum.add(u, t, 1);
            // total number of items of user u
            userNum.add(u, 1);

            // number of pairs (t, i, r)
            topicItemRatingNum[t][i][r]++;
            // total number of words assigned to topic t
            topicItemNum.add(t, i, 1);
        }
    }

    @Override
    protected void eStep() {

        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();

        // collapse Gibbs sampling
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rui = me.get();

            int r = ratingScale.indexOf(rui);  // rating level 0 ~ numLevels
            int t = topics.get(u, i);

            userTopicNum.add(u, t, -1);
            userNum.add(u, -1);
            topicItemRatingNum[t][i][r]--;
            topicItemNum.add(t, i, -1);

            // do multinomial sampling via cumulative method:
            double[] p = new double[numTopics];
            for (int k = 0; k < numTopics; k++) {
                p[k] = (userTopicNum.get(u, k) + alpha.get(k)) / (userNum.get(u) + sumAlpha) * (topicItemRatingNum[k][i][r] + beta.get(r))
                        / (topicItemNum.get(k, i) + sumBeta);
            }
            // cumulate multinomial parameters
            for (int k = 1; k < p.length; k++) {
                p[k] += p[k - 1];
            }
            // scaled sample because of unnormalized p[], randomly sampled a new topic t
            double rand = Randoms.uniform() * p[numTopics - 1];
            for (t = 0; t < p.length; t++) {
                if (rand < p[t])
                    break;
            }

            // new topic t
            topics.put(u, i, t);

            // add newly estimated z_i to count variables
            userTopicNum.add(u, t, 1);
            userNum.add(u, 1);
            topicItemRatingNum[t][i][r]++;
            topicItemNum.add(t, i, 1);
        }

    }

    /**
     * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
     */
    @Override
    protected void mStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double ak, br;

        // update alpha vector
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

        // update beta_k
        for (int r = 0; r < numRatingLevels; r++) {
            br = beta.get(r);
            double numerator = 0, denominator = 0;
            for (int i = 0; i < numItems; i++) {
                for (int k = 0; k < numTopics; k++) {
                    numerator += digamma(topicItemRatingNum[k][i][r] + br) - digamma(br);
                    denominator += digamma(topicItemNum.get(k, i) + sumBeta) - digamma(sumBeta);
                }
            }
            if (numerator != 0)
                beta.set(r, br * (numerator / denominator));
        }
    }

    protected void readoutParams() {
        double val = 0;
        double sumAlpha = alpha.sum();

        for (int u = 0; u < numUsers; u++) {
            for (int k = 0; k < numTopics; k++) {
                val = (userTopicNum.get(u, k) + alpha.get(k)) / (userNum.get(u) + sumAlpha);
                userTopicSumProbs.add(u, k, val);
            }
        }

        double sumBeta = beta.sum();
        for (int k = 0; k < numTopics; k++) {
            for (int i = 0; i < numItems; i++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    val = (topicItemRatingNum[k][i][r] + beta.get(r)) / (topicItemNum.get(k, i) + sumBeta);
                    topicItemRatingSumProbs[k][i][r] += val;
                }
            }
        }
        numStats++;
    }

    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicSumProbs.scale(1.0 / numStats);

        topicItemRatingProbs = new double[numTopics][numItems][numRatingLevels];
        for (int k = 0; k < numTopics; k++) {
            for (int i = 0; i < numItems; i++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    topicItemRatingProbs[k][i][r] = topicItemRatingSumProbs[k][i][r] / numStats;
                }
            }
        }
    }

    @Override
    protected boolean isConverged(int iter) {

        if (validMatrix == null)
            return false;

        // get posterior probability distribution first
        estimateParams();

        // compute current RMSE
        int numCount = 0;
        double sum = 0;
        for (MatrixEntry me : validMatrix) {
            double rate = me.get();

            int u = me.row();
            int j = me.column();

            double pred = 0;
            try {
                pred = predict(u, j, true);
            } catch (LibrecException e) {
                e.printStackTrace();
            }
            if (Double.isNaN(pred))
                continue;

            double err = rate - pred;

            sum += err * err;
            numCount++;
        }

        double RMSE = Math.sqrt(sum / numCount);
        double delta = RMSE - preRMSE;

        if (numStats > 1 && delta > 0)
            return true;

        preRMSE = RMSE;
        return false;
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double pred = 0;

        for (int r = 0; r < numRatingLevels; r++) {
            double rate = ratingScale.get(r);

            double prob = 0;
            for (int k = 0; k < numTopics; k++) {
                prob += userTopicProbs.get(userIdx, k) * topicItemRatingProbs[k][itemIdx][r];
            }

            pred += prob * rate;
        }

        return pred;
    }
}
