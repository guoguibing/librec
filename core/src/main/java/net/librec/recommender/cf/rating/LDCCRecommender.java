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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Guo Guibing and zhanghaidong
 */
public class LDCCRecommender extends ProbabilisticGraphicalRecommender {

    private Table<Integer, Integer, Integer> userTopics, itemTopics; // Zu, Zv

    private DenseMatrix numEachUserTopics, numEachItemTopics;  // Nui, Nvj
    private DenseVector numEachUserRatings, numEachItemRatings;                    // Nv

    private DenseMatrix numUserItemTopics;
    private int[][][] numUserItemRatingTopics;

    private int numUserTopics, numItemTopics;

    private double userAlpha, itemAlpha, ratingBeta;

    private DenseMatrix userTopicProbs, itemTopicProbs;
    private DenseMatrix userTopicProbsSum, itemTopicProbsSum;
    private double[][][] userItemRatingTopicProbs, userItemRatingTopicProbsSum;

    private int numRatingLevels;
    private List<Double> ratingScale;

    private int numStats;  //size of statistics
    private double loss;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numStats = 0;

        numUserTopics = conf.getInt("rec.pgm.number.users", 10);
        numItemTopics = conf.getInt("rec.pgm.number.items", 10);
        burnIn = conf.getInt("rec.pgm.burn-in", 100);

        Set<Double> ratingScaleSet = trainMatrix.getValueSet();
        ratingScale = new ArrayList<>(ratingScaleSet);
        numRatingLevels = ratingScale.size();

        userAlpha = conf.getDouble("rec.pgm.user.alpha", 1.0 / numUserTopics);
        itemAlpha = conf.getDouble("rec.pgm.item.alpha", 1.0 / numItemTopics);
        ratingBeta = conf.getDouble("rec.pgm.rating.beta", 1.0 / numRatingLevels);

        numEachUserTopics = new DenseMatrix(numUsers, numUserTopics);
        numEachItemTopics = new DenseMatrix(numItems, numItemTopics);
        numEachUserRatings = new DenseVector(numUsers);
        numEachItemRatings = new DenseVector(numItems);

        numUserItemRatingTopics = new int[numUserTopics][numItemTopics][numRatingLevels];
        numUserItemTopics = new DenseMatrix(numUserTopics, numItemTopics);

        userTopics = HashBasedTable.create();
        itemTopics = HashBasedTable.create();

        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int v = me.column();
            double rating = me.get();
            int r = ratingScale.indexOf(rating);

            int i = (int) (numUserTopics * Randoms.uniform());
            int j = (int) (numItemTopics * Randoms.uniform());

            numEachUserTopics.add(u, i, 1);
            numEachUserRatings.add(u, 1);

            numEachItemTopics.add(v, j, 1);
            numEachItemRatings.add(v, 1);

            numUserItemRatingTopics[i][j][r]++;
            numUserItemTopics.add(i, j, 1);

            userTopics.put(u, v, i);
            itemTopics.put(u, v, j);
        }

        // parameters
        userTopicProbsSum = new DenseMatrix(numUsers, numUserTopics);
        itemTopicProbsSum = new DenseMatrix(numItems, numItemTopics);
        userItemRatingTopicProbs = new double[numUserTopics][numItemTopics][numRatingLevels];
        userItemRatingTopicProbsSum = new double[numUserTopics][numItemTopics][numRatingLevels];

    }

    @Override
    protected void eStep() {
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int v = me.column();
            double rating = me.get();
            int r = ratingScale.indexOf(rating);

            // user and item's factors
            int i = userTopics.get(u, v);
            int j = itemTopics.get(u, v);

            // remove this observation
            numEachUserTopics.add(u, i, -1);
            numEachUserRatings.add(u, -1);

            numEachItemTopics.add(v, j, -1);
            numEachItemRatings.add(v, -1);

            numUserItemRatingTopics[i][j][r]--;
            numUserItemTopics.add(i, j, -1);

            // Compute P(i, j)
            DenseMatrix probs = new DenseMatrix(numUserTopics, numItemTopics);
            double sum = 0;
            for (int m = 0; m < numUserTopics; m++) {
                for (int n = 0; n < numItemTopics; n++) {
                    // Compute Pmn
                    double v1 = (numEachUserTopics.get(u, m) + userAlpha) / (numEachUserRatings.get(u) + numUserTopics * userAlpha);
                    double v2 = (numEachUserTopics.get(i, n) + itemAlpha) / (numEachItemRatings.get(v) + numItemTopics * itemAlpha);
                    double v3 = (numUserItemRatingTopics[m][n][r] + ratingBeta) / (numUserItemTopics.get(m, n) + numRatingLevels * ratingBeta);

                    double prob = v1 * v2 * v3;
                    probs.set(m, n, prob);
                    sum += prob;
                }
            }

            probs = probs.scale(1.0 / sum);

            // Re-sample user factor
            double[] Pu = new double[numUserTopics];
            for (int m = 0; m < numUserTopics; m++) {
                Pu[m] = probs.sumOfRow(m);
            }
            for (int m = 1; m < numUserTopics; m++) {
                Pu[m] += Pu[m - 1];
            }

            double rand = Randoms.uniform();
            for (i = 0; i < numUserTopics; i++) {
                if (rand < Pu[i]) {
                    break;
                }
            }

            // Re-sample item factor
            double[] Pv = new double[numItemTopics];
            for (int n = 0; n < numItemTopics; n++) {
                Pv[n] = probs.sumOfColumn(n);
            }
            for (int n = 1; n < numItemTopics; n++) {
                Pv[n] += Pv[n - 1];
            }

            rand = Randoms.uniform();
            for (j = 0; j < numItemTopics; j++) {
                if (rand < Pv[j]) {
                    break;
                }
            }

            // Add statistics
            numEachUserTopics.add(u, i, 1);
            numEachUserRatings.add(u, 1);

            numEachItemTopics.add(v, j, 1);
            numEachItemRatings.add(v, 1);

            numUserItemRatingTopics[i][j][r]++;
            numUserItemTopics.add(i, j, 1);

            userTopics.put(u, v, i);
            itemTopics.put(u, v, j);
        }
    }

    @Override
    protected void mStep() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readoutParams() {
        for (int u = 0; u < numUsers; u++) {
            for (int i = 0; i < numUserTopics; i++) {
                userTopicProbsSum.add(u, i, (numEachUserTopics.get(u, i) + userAlpha) / (numEachUserRatings.get(u) + numUserTopics * userAlpha));
            }
        }

        for (int v = 0; v < numItems; v++) {
            for (int j = 0; j < numItemTopics; j++) {
                itemTopicProbsSum.add(v, j, (numEachItemTopics.get(v, j) + itemAlpha) / (numEachItemRatings.get(v) + numItemTopics * itemAlpha));
            }
        }

        for (int i = 0; i < numUserTopics; i++) {
            for (int j = 0; j < numItemTopics; j++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    userItemRatingTopicProbsSum[i][j][r] += (numUserItemRatingTopics[i][j][r] + ratingBeta) / (numUserItemTopics.get(i, j) + numRatingLevels * ratingBeta);
                }
            }
        }
        numStats++;
    }

    /**
     * estimate the model parameters
     */
    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicProbsSum.scale(1.0 / numStats);
        itemTopicProbs = itemTopicProbsSum.scale(1.0 / numStats);

        for (int i = 0; i < numUserTopics; i++) {
            for (int j = 0; j < numItemTopics; j++) {
                for (int r = 0; r < numRatingLevels; r++) {
                    userItemRatingTopicProbs[i][j][r] = userItemRatingTopicProbsSum[i][j][r] / numStats;
                }
            }
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double pred = 0;

        for (int l = 0; l < numRatingLevels; l++) {
            double rate = ratingScale.get(l);

            double prob = 0; // P(r|u,v)=\sum_{i,j} P(r|i,j)P(i|u)P(j|v)
            for (int i = 0; i < numUserTopics; i++) {
                for (int j = 0; j < numItemTopics; j++) {
                    prob += userItemRatingTopicProbs[i][j][l] * userTopicProbs.get(userIdx, i) * itemTopicProbs.get(itemIdx, j);
                }
            }

            pred += rate * prob;
        }
        return pred;
    }

    @Override
    protected boolean isConverged(int iter) {
        // Get the parameters
        estimateParams();

        // Compute the perplexity
        int N = 0;
        double sum = 0.0;
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int v = me.column();
            double rating = me.get();

            sum += perplexity(u, v, rating);
            N++;
        }

        double perp = Math.exp(sum / N);
        double delta = perp - loss;

        if (numStats > 1 && delta > 0)
            return true;

        loss = perp;
        return false;
    }

    protected double perplexity(int user, int item, double rating) {
        int r = (int) (rating / minRate - 1);

        // Compute P(r | u, v)
        double prob = 0;
        for (int i = 0; i < numUserTopics; i++) {
            for (int j = 0; j < numItemTopics; j++) {
                prob += userItemRatingTopicProbs[i][j][r] * userTopicProbs.get(user, i) * itemTopicProbs.get(item, j);
            }
        }
        return -Math.log(prob);
    }
}
