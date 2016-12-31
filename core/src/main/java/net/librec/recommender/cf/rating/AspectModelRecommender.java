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
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.ProbabilisticGraphicalRecommender;

import java.util.HashMap;
import java.util.Map;

/**
 * <h3> Latent class models for collaborative filtering</h3>
 * <p>
 * This implementation refers to the method proposed by Thomas et al. at IJCAI 1999.
 * <p>
 * <strong>Tempered EM:</strong> Thomas Hofmann, <strong>Latent class models for collaborative filtering
 * </strong>, IJCAI. 1999, 99: 688-693.
 *
 * @author guoguibin and Haidong Zhang
 */
public class AspectModelRecommender extends ProbabilisticGraphicalRecommender {
    /*
     * Conditional distribution: P(u|z)
     */
    protected DenseMatrix topicUserProbs, topicUserProbsSum;
    /*
     * Conditional distribution: P(i|z)
     */
    protected DenseMatrix topicItemProbs, topicItemProbsSum;
    /*
     * topic distribution: P(z)
     */
    protected DenseVector topicProbs, topicProbsSum;
    /*
     *
     */
    protected DenseVector topicProbsMean, topicProbsMeanSum;
    /*
     *
     */
    protected DenseVector topicProbsVariance, topicProbsVarianceSum;

    /*
     * number of topics
     */
    protected int numTopics;
    /*
     * small value
     */
    protected static double smallValue = 0.0000001;
    /*
     * {user, item, {topic z, probability}}
     */
    protected Table<Integer, Integer, Map<Integer, Double>> Q;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        numTopics = conf.getInt("rec.factory.number", 10);

        // Initialize topic distribution
        topicProbs = new DenseVector(numTopics);
        topicProbsSum = new DenseVector(numTopics);
        double[] probs = Randoms.randProbs(numTopics);
        for (int z = 0; z < numTopics; z++) {
            topicProbs.set(z, probs[z]);
        }

        // intialize conditional distribution P(u|z)
        topicUserProbs = new DenseMatrix(numTopics, numUsers);
        topicUserProbsSum = new DenseMatrix(numTopics, numUsers);
        for (int z = 0; z < numTopics; z++) {
            probs = Randoms.randProbs(numUsers);
            for (int u = 0; u < numUsers; u++) {
                topicUserProbs.set(z, u, probs[u]);
            }
        }

        // initialize conditional distribution P(i|z)
        topicItemProbs = new DenseMatrix(numTopics, numItems);
        topicItemProbsSum = new DenseMatrix(numTopics, numItems);
        for (int z = 0; z < numTopics; z++) {
            probs = Randoms.randProbs(numItems);
            for (int i = 0; i < numItems; i++) {
                topicItemProbs.set(z, i, probs[i]);
            }
        }

        // initialize Q
        Q = HashBasedTable.create();
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            Q.put(u, i, new HashMap<Integer, Double>());
        }

        // 
        double globalMean = trainMatrix.mean();
        topicProbsMean = new DenseVector(numTopics);
        topicProbsVariance = new DenseVector(numTopics);
        topicProbsMeanSum = new DenseVector(numTopics);
        topicProbsVarianceSum = new DenseVector(numTopics);
        for (int z = 0; z < numTopics; z++) {
            topicProbsMean.set(z, globalMean);
            topicProbsVariance.set(z, 2);
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
            double r = me.get();

            double denominator = 0;
            double[] numerator = new double[numTopics];
            for (int z = 0; z < numTopics; z++) {
                double val = topicProbs.get(z) * topicUserProbs.get(z, u) * topicItemProbs.get(z, i)
                        * Gaussian.pdf(r, topicProbsMean.get(z), topicProbsVariance.get(z));
                numerator[z] = val;
                denominator += val;
            }

            Map<Integer, Double> QTopicProbs = Q.get(u, i);
            for (int z = 0; z < numTopics; z++) {
                double prob = (denominator > 0 ? numerator[z] / denominator : 0.0d);
                QTopicProbs.put(z, prob);
            }
        }
    }

    @Override
    protected void mStep() {
        topicProbsSum.setAll(0.0);
        topicUserProbsSum.setAll(0.0);
        topicItemProbsSum.setAll(0.0);
        topicProbsMeanSum.setAll(0.0);
        topicProbsVarianceSum.setAll(0.0);

        for (int z = 0; z < numTopics; z++) {
            for (MatrixEntry me : trainMatrix) {
                int u = me.row();
                int i = me.column();
                double r = me.get();

                double val = Q.get(u, i).get(z);
                topicProbsSum.add(z, val);
                topicUserProbsSum.add(z, u, val);
                topicItemProbsSum.add(z, i, val);
                topicProbsMeanSum.add(z, r * val);
            }

            topicProbsSum.add(z, smallValue);
            topicProbs.set(z, topicProbsSum.get(z) / numRates);
            for (int u = 0; u < numUsers; u++) {
                topicUserProbs.set(z, u, topicUserProbsSum.get(z, u) / topicProbsSum.get(z));
            }
            for (int i = 0; i < numItems; i++) {
                topicItemProbs.set(z, i, topicItemProbsSum.get(z, i) / topicProbsSum.get(z));
            }
            double mean = topicProbsMeanSum.get(z) / topicProbsSum.get(z);
            for (MatrixEntry me : trainMatrix) {
                int u = me.row();
                int i = me.column();
                double r = me.get();
                double val = Q.get(u, i).get(z);
                topicProbsVarianceSum.add(z, (r - mean) * (r - mean) * val);
            }
            topicProbsMean.set(z, mean);
            topicProbsVariance.set(z, (topicProbsVarianceSum.get(z) + smallValue) / topicProbsSum.get(z));
        }
    }

    @Override
    protected void readoutParams() {

    }

    @Override
    protected void estimateParams() {

    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double predictRating = 0.0d;
        double denominator = 0.0d;
        for (int z = 0; z < numTopics; z++) {
            double weight = topicProbs.get(z) * topicUserProbs.get(z, userIdx) * topicItemProbs.get(z, itemIdx);
            denominator += weight;
            predictRating += weight * topicProbsMean.get(z);
        }
        predictRating = predictRating / denominator;

        return predictRating;
    }
}
