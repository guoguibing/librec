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
import net.librec.math.algorithm.Gaussian;
import net.librec.math.algorithm.Randoms;
import net.librec.math.algorithm.Stats;
import net.librec.math.structure.*;
import net.librec.recommender.ProbabilisticGraphicalRecommender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thomas Hofmann, <strong>Collaborative Filtering via Gaussian Probabilistic Latent Semantic Analysis</strong>, SIGIR
 * 2003. <br>
 * <p>
 * <strong>Tempered EM:</strong> Thomas Hofmann, <strong>Unsupervised Learning by Probabilistic Latent Semantic
 * Analysis</strong>, Machine Learning, 42, 177�C196, 2001.
 */
public class GPLSARecommender extends ProbabilisticGraphicalRecommender {
    /*
     * number of latent topics
     */
    protected int numTopics;
    /*
     * {user, item, {topic z, probability}}
     */
    protected Table<Integer, Integer, Map<Integer, Double>> Q;
    /*
     * Conditional Probability: P(z|u)
     */
    protected DenseMatrix userTopicProbs;
    /*
     * Conditional Probability: P(v|y,z)
     */
    protected DenseMatrix topicItemMu, topicItemSigma;
    /*
     * regularize ratings
     */
    protected DenseVector userMu, userSigma;
    /*
     * smoothing weight
     */
    protected float smoothWeight;
    /*
     * tempered EM parameter beta, suggested by Wu Bin
     */
    protected float b;
    /*
     * small value for initialization
     */
    protected static double smallValue = 0.01;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numTopics = conf.getInt("rec.topic.number", 10);

        // Initialize users' conditional probabilities
        userTopicProbs = new DenseMatrix(numUsers, numTopics);
        for (int u = 0; u < numUsers; u++) {
            double[] probs = Randoms.randProbs(numTopics);
            for (int z = 0; z < numTopics; z++) {
                userTopicProbs.set(u, z, probs[z]);
            }
        }

        double mean = trainMatrix.mean();
        double sd = Stats.sd(trainMatrix.getData(), mean);

        userMu = new DenseVector(numUsers);
        userSigma = new DenseVector(numUsers);
        smoothWeight = conf.getInt("rec.recommender.smoothWeight");
        for (int u = 0; u < numUsers; u++) {
            SparseVector userRow = trainMatrix.row(u);
            int numRatings = userRow.size();
            if (numRatings < 1) {
                continue;
            }

            // Compute mu_u
            double mu_u = (userRow.sum() + smoothWeight * mean) / (numRatings + smoothWeight);
            userMu.set(u, mu_u);

            // Compute sigma_u
            double sum = 0;
            for (VectorEntry ve : userRow) {
                sum += Math.pow(ve.get() - mu_u, 2);
            }
            sum += smoothWeight * Math.pow(sd, 2);
            double sigma_u = Math.sqrt(sum / (numRatings + smoothWeight));
            userSigma.set(u, sigma_u);
        }


        // Initialize Q
        Q = HashBasedTable.create();

        for (MatrixEntry trainMatrixEntry : trainMatrix) {
            int userIdx = trainMatrixEntry.row();
            int itemIdx = trainMatrixEntry.column();
            double rating = trainMatrixEntry.get();

            double r = (rating - userMu.get(userIdx)) / userSigma.get(userIdx);
            trainMatrix.set(userIdx,itemIdx,r);

            Q.put(userIdx, itemIdx, new HashMap<Integer, Double>());
        }

        // Initialize Mu, Sigma
        topicItemMu = new DenseMatrix(numItems, numTopics);
        topicItemSigma = new DenseMatrix(numItems, numTopics);
        for (int i = 0; i < numItems; i++) {
            SparseVector itemColumn = trainMatrix.column(i);
            int numRatings = itemColumn.size();
            if (numRatings < 1) {
                continue;
            }

            double mu_i = itemColumn.mean();

            double sum = 0;
            for (VectorEntry ve : itemColumn) {
                sum += Math.pow(ve.get() - mu_i, 2);
            }

            double sd_i = Math.sqrt(sum / numRatings);

            for (int z = 0; z < numTopics; z++) {
                topicItemMu.set(i, z, mu_i + smallValue * Randoms.uniform());
                topicItemSigma.set(i, z, sd_i + smallValue * Randoms.uniform());
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        super.trainModel();
    }


    @Override
    protected void eStep() {
        // variational inference to compute Q
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            double rating = me.get();

            double denominator = 0.0;
            double[] numerator = new double[numTopics];
            for (int z = 0; z < numTopics; z++) {
                double pdf = Gaussian.pdf(rating, topicItemMu.get(i, z), topicItemSigma.get(i, z));
                double val = Math.pow(userTopicProbs.get(u, z) * pdf, b); // Tempered EM

                numerator[z] = val;
                denominator += val;
            }
            Map<Integer, Double> factorProbs = Q.get(u, i);
            for (int z = 0; z < numTopics; z++) {
                double prob = (denominator > 0 ? numerator[z] / denominator : 0);
                factorProbs.put(z, prob);
            }
        }
    }

    @Override
    protected void mStep() {
        // theta_u,z
        for (int u = 0; u < numUsers; u++) {
            List<Integer> items = trainMatrix.getColumns(u);
            if (items.size() < 1) {
                continue;
            }

            double[] numerator = new double[numTopics];
            double denominator = 0.0;
            for (int z = 0; z < numTopics; z++) {
                for (int i : items) {
                    numerator[z] = Q.get(u, i).get(z);
                }

                denominator += numerator[z];
            }

            for (int z = 0; z < numTopics; z++) {
                userTopicProbs.set(u, z, numerator[z] / denominator);
            }
        }

        // topicItemMu, topicItemSigma
        for (int i = 0; i < numItems; i++) {
            List<Integer> users = trainMatrix.getRows(i);
            if (users.size() < 1)
                continue;

            for (int z = 0; z < numTopics; z++) {
                double numerator = 0, denominator = 0;

                for (int u : users) {
                    double rating = trainMatrix.get(u, i);
                    double prob = Q.get(u, i).get(z);

                    numerator += rating * prob;
                    denominator += prob;
                }

                double mu = denominator > 0 ? numerator / denominator : 0;
                topicItemMu.set(i, z, mu);

                numerator = 0;
                denominator = 0;
                for (int u : users) {
                    double rating = trainMatrix.get(u, i);
                    double prob = Q.get(u, i).get(z);

                    numerator += Math.pow(rating - mu, 2) * prob;
                    denominator += prob;
                }

                double sigma = denominator > 0 ? Math.sqrt(numerator / denominator) : 0;
                topicItemSigma.set(i, z, sigma);
            }
        }
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double sum = 0;
        for (int z = 0; z < numTopics; z++) {
            sum += userTopicProbs.get(userIdx, z) * topicItemMu.get(itemIdx, z);
        }
        double predictRating = userMu.get(userIdx) + userSigma.get(userIdx) * sum;

        return predictRating;
    }
}
