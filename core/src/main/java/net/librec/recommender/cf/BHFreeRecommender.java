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

/**
 * Barbieri et al., <strong>Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference
 * Data</strong>, SDM 2012. <br>
 * <p>
 * <strong>Remarks:</strong> this class implements the BH-free method.
 *
 * @author Guo Guibing and haidong zhang
 */
public class BHFreeRecommender extends ProbabilisticGraphicalRecommender {

    private float initGamma, initSigma, initAlpha, initBeta;

    /**
     * number of user communities
     */
    private int numUserTopics;   // K

    /**
     * number of item categories
     */
    private int numItemTopics;        // L

    /**
     * evaluation of the user u which have been assigned to the user topic k
     */
    private DenseMatrix userTopicNum;

    /**
     * observations for the user
     */
    private DenseVector userNum;

    /**
     * observations associated with community k
     */
    private DenseVector uTopicNum;

    /**
     * number of user communities * number of topics
     */
    private DenseMatrix userTopicItemTopicNum;   // Nkl

    /**
     * number of user communities * number of topics * number of ratings
     */
    private int[][][] userTopicItemTopicRatingNum, userTopicItemTopicItemNum;    // Nklr, Nkli;

    /**
     *
     */
    private Table<Integer, Integer, Integer> userTopics, itemTopics;

    /**
     *
     */
    private int numRatingLevels;

    // parameters
    private DenseMatrix userTopicProbs, userTopicItemTopicProbs;
    private DenseMatrix userTopicSumProbs, userTopicItemTopicSumProbs;
    private double[][][] userTopicItemTopicRatingProbs, userTopicItemTopicItemProbs;
    private double[][][] userTopicItemTopicRatingSumProbs, userTopicItemTopicItemSumProbs;

    protected void setup() throws LibrecException {
        super.setup();

        numUserTopics = conf.getInt("rec.bhfree.user.topic.number", 10);
        numItemTopics = conf.getInt("rec.bhfree.item.topic.number", 10);
        initAlpha = conf.getFloat("rec.bhfree.alpha", 1.0f / numUserTopics);
        initBeta = conf.getFloat("rec.bhfree.beta", 1.0f / numItemTopics);
        initGamma = conf.getFloat("rec.bhfree.gamma", 1.0f / numRatingLevels);
        initSigma = conf.getFloat("rec.sigma", 1.0f / numItems);
        numRatingLevels = trainMatrix.getValueSet().size();

        userTopicNum = new DenseMatrix(numUsers, numUserTopics);
        userNum = new DenseVector(numUsers);

        userTopicItemTopicNum = new DenseMatrix(numUserTopics, numItemTopics);
        uTopicNum = new DenseVector(numUserTopics);

        userTopicItemTopicRatingNum = new int[numUserTopics][numItemTopics][numRatingLevels];
        userTopicItemTopicItemNum = new int[numUserTopics][numItemTopics][numItems];

        userTopics = HashBasedTable.create();
        itemTopics = HashBasedTable.create();

        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rate = me.get();
            int r = ratingScale.indexOf(rate);

            int k = (int) (numUserTopics * Randoms.uniform()); // user's topic k
            int l = (int) (numItemTopics * Randoms.uniform()); // item's topic l

            userTopicNum.add(u, k, 1);
            userNum.add(u, 1);

            userTopicItemTopicNum.add(k, l, 1);
            uTopicNum.add(k, 1);

            userTopicItemTopicRatingNum[k][l][r]++;
            userTopicItemTopicItemNum[k][l][i]++;

            userTopics.put(u, i, k);
            itemTopics.put(u, i, l);
        }

        // parameters
        userTopicSumProbs = new DenseMatrix(numUsers, numUserTopics);
        userTopicItemTopicSumProbs = new DenseMatrix(numUserTopics, numItemTopics);
        userTopicItemTopicRatingSumProbs = new double[numUserTopics][numItemTopics][numRatingLevels];
        userTopicItemTopicRatingProbs = new double[numUserTopics][numItemTopics][numRatingLevels];
        userTopicItemTopicItemSumProbs = new double[numUserTopics][numItemTopics][numItems];
        userTopicItemTopicItemProbs = new double[numUserTopics][numItemTopics][numItems];
    }

    @Override
    protected void eStep() {
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rate = me.get();
            int r = ratingScale.indexOf(rate);

            int k = userTopics.get(u, i);
            int l = itemTopics.get(u, i);

            userTopicNum.add(u, k, -1);
            userNum.add(u, -1);
            userTopicItemTopicNum.add(k, l, -1);
            uTopicNum.add(k, -1);
            userTopicItemTopicRatingNum[k][l][r]--;
            userTopicItemTopicItemNum[k][l][i]--;

            DenseMatrix userTopicItemTopicProbs = new DenseMatrix(numUserTopics, numItemTopics);
            double sum = 0;
            for (int z = 0; z < numUserTopics; z++) {
                for (int w = 0; w < numItemTopics; w++) {
                    double v1 = (userTopicNum.get(u, k) + initAlpha) / (userNum.get(u) + numUserTopics * initAlpha);
                    double v2 = (userTopicItemTopicNum.get(k, l) + initBeta) / (uTopicNum.get(k) + numItemTopics * initBeta);
                    double v3 = (userTopicItemTopicRatingNum[k][l][r] + initGamma) / (userTopicItemTopicNum.get(k, l) + numRatingLevels * initGamma);
                    double v4 = (userTopicItemTopicItemNum[k][l][i] + initSigma) / (userTopicItemTopicNum.get(k, l) + numItems * initSigma);

                    double val = v1 * v2 * v3 * v4;
                    userTopicItemTopicProbs.set(z, w, val);
                    sum += val;
                }
            }

            // normalization
            userTopicItemTopicProbs = userTopicItemTopicProbs.scale(1.0 / sum);

            // resample k
            double[] userTopicProbs = new double[numUserTopics];
            for (int z = 0; z < numUserTopics; z++) {
                userTopicProbs[z] = userTopicItemTopicProbs.sumOfRow(z);
            }
            for (int z = 1; z < numUserTopics; z++) {
                userTopicProbs[z] += userTopicProbs[z - 1];
            }

            double rand = Randoms.uniform();
            for (k = 0; k < numUserTopics - 1; k++) {
                if (rand < userTopicProbs[k])
                    break;
            }

            // resample item topic
            double[] itemTopicProbs = new double[numItemTopics];
            for (int w = 0; w < numItemTopics; w++) {
                itemTopicProbs[w] = userTopicItemTopicProbs.sumOfColumn(w);
            }

            for (int w = 1; w < numItemTopics; w++) {
                itemTopicProbs[w] += itemTopicProbs[w - 1];
            }

            rand = Randoms.uniform();
            for (l = 0; l < numItemTopics - 1; l++) {
                if (rand < itemTopicProbs[l])
                    break;
            }

            // add statistic
            userTopicNum.add(u, k, 1);
            userNum.add(u, 1);
            userTopicItemTopicNum.add(k, l, 1);
            uTopicNum.add(k, 1);
            userTopicItemTopicRatingNum[k][l][r]++;
            userTopicItemTopicItemNum[k][l][i]++;

            userTopics.put(u, i, k);
            itemTopics.put(u, i, l);
        }

    }

    @Override
    protected void mStep() {

    }

    @Override
    protected void readoutParams() {
        for (int u = 0; u < numUsers; u++) {
            for (int k = 0; k < numUserTopics; k++) {
                userTopicSumProbs.add(u, k, (userTopicNum.get(u, k) + initAlpha) / (userNum.get(u) + numUserTopics * initAlpha));
            }
        }

        for (int k = 0; k < numUserTopics; k++) {
            for (int l = 0; l < numItemTopics; l++) {
                userTopicItemTopicSumProbs.add(k, l, (userTopicItemTopicNum.get(k, l) + initBeta) / (uTopicNum.get(k) + numItemTopics * initBeta));
            }
        }

        for (int k = 0; k < numUserTopics; k++) {
            for (int l = 0; l < numItemTopics; l++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    userTopicItemTopicRatingSumProbs[k][l][r] += (userTopicItemTopicRatingNum[k][l][r] + initGamma) / (userTopicItemTopicNum.get(k, l) + numRatingLevels * initGamma);
                }
            }
        }

        for (int k = 0; k < numUserTopics; k++) {
            for (int l = 0; l < numItemTopics; l++) {
                for (int i = 0; i < numItems; i++) {
                    userTopicItemTopicItemSumProbs[k][l][i] += (userTopicItemTopicItemNum[k][l][i] + initSigma) / (userTopicItemTopicNum.get(k, l) + numItems * initSigma);
                }
            }
        }

        numStats++;
    }

    @Override
    protected void estimateParams() {

        double scale = 1.0 / numStats;
        userTopicProbs = userTopicSumProbs.scale(scale);
        userTopicItemTopicProbs = userTopicItemTopicSumProbs.scale(scale);

        for (int k = 0; k < numUserTopics; k++) {
            for (int l = 0; l < numItemTopics; l++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    userTopicItemTopicRatingProbs[k][l][r] = userTopicItemTopicRatingSumProbs[k][l][r] * scale;
                }
            }
        }

        for (int k = 0; k < numUserTopics; k++) {
            for (int l = 0; l < numItemTopics; l++) {
                for (int i = 0; i < numItems; i++) {
                    userTopicItemTopicItemProbs[k][l][i] = userTopicItemTopicItemSumProbs[k][l][i] * scale;
                }
            }
        }
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
        double sum = 0, probs = 0;

        for (int r = 0; r < numRatingLevels; r++) {
            double rate = ratingScale.get(r);

            double prob = 0;
            for (int k = 0; k < numUserTopics; k++) {
                for (int l = 0; l < numItemTopics; l++) {
                    prob += userTopicProbs.get(userIdx, k) * userTopicItemTopicProbs.get(k, l) * userTopicItemTopicRatingProbs[k][l][r];
                }
            }

            sum += rate * prob;
            probs += prob;
        }

        return sum / probs;
    }

    protected double predictRanking(int userIdx, int itemIdx) {
        double rank = 0;

        for (int r = 0; r < numRatingLevels; r++) {
            double rate = ratingScale.get(r);

            double prob = 0;
            for (int k = 0; k < numUserTopics; k++) {
                for (int l = 0; l < numItemTopics; l++) {
                    prob += userTopicProbs.get(userIdx, k) * userTopicItemTopicProbs.get(k, l) * userTopicItemTopicItemSumProbs[k][l][itemIdx] * userTopicItemTopicRatingProbs[k][l][r];
                }
            }

            rank += rate * prob;
        }

        return rank;
    }
}
