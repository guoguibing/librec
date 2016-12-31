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
package net.librec.recommender.baseline;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.ProbabilisticGraphicalRecommender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Set;

/**
 * It is a graphical model that clusters users into K groups for recommendation, see reference: Barbieri et al.,
 * <strong>Probabilistic Approaches to Recommendations</strong> (Section 2.2), Synthesis Lectures on Data Mining and
 * Knowledge Discovery, 2014.
 *
 * @author Guo Guibing and Zhang Haidong
 */
public class UserClusterRecommender extends ProbabilisticGraphicalRecommender {
    private DenseMatrix topicRatingProbs;   // Pkr
    private DenseVector topicInitialProbs;  // Pi

    private DenseMatrix userTopicProbs;  // Gamma_(u,k)
    private DenseMatrix userNumEachRating;    // Nur
    private DenseVector userNumRatings;     // Nu

    private int numTopics;
    private int numRatingLevels;
    private double lastLoss;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        Set<Double> ratingScaleSet = trainMatrix.getValueSet();
        ratingScale = new ArrayList<>(ratingScaleSet);
        numRatingLevels = ratingScale.size();
        numTopics = conf.getInt("rec.factory.number", 10);

        topicRatingProbs = new DenseMatrix(numTopics, numRatingLevels);
        for (int k = 0; k < numTopics; k++) {
            double[] probs = Randoms.randProbs(numRatingLevels);
            for (int r = 0; r < numRatingLevels; r++) {
                topicRatingProbs.set(k, r, probs[r]);
            }
        }

        topicInitialProbs = new DenseVector(Randoms.randProbs(numTopics));

        userTopicProbs = new DenseMatrix(numUsers, numTopics);

        userNumEachRating = new DenseMatrix(numUsers, numRatingLevels);
        userNumRatings = new DenseVector(numUsers);

        for (int u = 0; u < numUsers; u++) {
            SparseVector ru = trainMatrix.row(u);

            for (VectorEntry ve : ru) {
                double rui = ve.get();
                int r = ratingScale.indexOf(rui);
                userNumEachRating.add(u, r, 1);
            }

            userNumRatings.set(u, ru.size());
        }
        lastLoss = Double.MIN_VALUE;

    }

    @Override
    protected void eStep() {
        for (int u = 0; u < numUsers; u++) {
            BigDecimal sum_u = BigDecimal.ZERO;
            SparseVector ru = trainMatrix.row(u);

            BigDecimal[] sum_uk = new BigDecimal[numTopics];
            for (int k = 0; k < numTopics; k++) {
                BigDecimal userTopicProb = new BigDecimal(topicInitialProbs.get(k));

                for (VectorEntry ve : ru) {
                    double rui = ve.get();
                    int r = ratingScale.indexOf(rui);
                    BigDecimal topicRatingProb = new BigDecimal(topicRatingProbs.get(k, r));

                    userTopicProb = userTopicProb.multiply(topicRatingProb);
                }

                sum_uk[k] = userTopicProb;
                sum_u = sum_u.add(userTopicProb);
            }

            for (int k = 0; k < numTopics; k++) {
                double zuk = sum_uk[k].divide(sum_u, 6, RoundingMode.HALF_UP).doubleValue();
                userTopicProbs.set(u, k, zuk);
            }
        }

    }

    @Override
    protected void mStep() {
        double[] sum_uk = new double[numTopics];
        double sum = 0;

        for (int k = 0; k < numTopics; k++) {
            for (int r = 0; r < numRatingLevels; r++) {
                double numerator = 0.0, denorminator = 0.0;

                for (int u = 0; u < numUsers; u++) {
                    double ruk = userTopicProbs.get(u, k);
                    numerator += ruk * userNumEachRating.get(u, r);
                    denorminator += ruk * userNumRatings.get(u);
                }
                topicRatingProbs.set(k, r, numerator / denorminator);
            }

            double sum_u = 0;
            for (int u = 0; u < numUsers; u++) {
                double ruk = userTopicProbs.get(u, k);
                sum_u += ruk;
            }

            sum_uk[k] = sum_u;
            sum += sum_u;
        }

        for (int k = 0; k < numTopics; k++) {
            topicInitialProbs.set(k, sum_uk[k] / sum);
        }

    }

    @Override
    protected boolean isConverged(int iter) {
        double loss = 0.0;

        for (int u = 0; u < numUsers; u++) {
            for (int k = 0; k < numTopics; k++) {
                double ruk = userTopicProbs.get(u, k);
                double pi_k = topicInitialProbs.get(k);

                double sum_nl = 0;
                for (int r = 0; r < numRatingLevels; r++) {
                    double nur = userNumEachRating.get(u, r);
                    double pkr = topicRatingProbs.get(k, r);

                    sum_nl += nur * Math.log(pkr);
                }

                loss += ruk * (Math.log(pi_k) + sum_nl);
            }
        }

        float deltaLoss = (float) (loss - lastLoss);

        if (iter > 1 && (deltaLoss > 0 || Double.isNaN(deltaLoss))) {
            return true;
        }

        lastLoss = loss;
        return false;
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double pred = 0;

        for (int k = 0; k < numTopics; k++) {
            double pu_k = userTopicProbs.get(userIdx, k); // probability that user u belongs to cluster k
            double pred_k = 0;

            for (int r = 0; r < numRatingLevels; r++) {
                double ruj = ratingScale.get(r);
                double pkr = topicRatingProbs.get(k, r);

                pred_k += ruj * pkr;
            }

            pred += pu_k * pred_k;
        }

        return pred;
    }

}
