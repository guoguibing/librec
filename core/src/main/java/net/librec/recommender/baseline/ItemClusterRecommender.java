// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//
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
import java.util.Collections;
import java.util.Set;

/**
 * It is a graphical model that clusters items into K groups for recommendation, as opposite to
 * the {@code UserCluster} recommender.
 *
 * @author Guo Guibing and zhanghaidong
 */
public class ItemClusterRecommender extends ProbabilisticGraphicalRecommender {
    private DenseMatrix topicRatingProbs;   // Pkr
    private DenseVector topicInitialProbs;  // Pi

    private DenseMatrix itemTopicProbs;  // Gamma_(u,k)
    private DenseMatrix itemNumEachRating;    // Nur
    private DenseVector itemNumRatings;     // Nu

    private int numTopics;
    private int numRatingLevels;
    private double lastLoss;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        isRanking = false;
        Set<Double> ratingScaleSet = trainMatrix.getValueSet();
        ratingScale = new ArrayList<>(ratingScaleSet);
        numRatingLevels = ratingScale.size();
        Collections.sort(ratingScale);
        numTopics = conf.getInt("rec.pgm.number", 10);

        topicRatingProbs = new DenseMatrix(numTopics, numRatingLevels);
        for (int k = 0; k < numTopics; k++) {
            double[] probs = Randoms.randProbs(numRatingLevels);
            for (int r = 0; r < numRatingLevels; r++) {
                topicRatingProbs.set(k, r, probs[r]);
            }
        }

        topicInitialProbs = new DenseVector(Randoms.randProbs(numTopics));

        itemTopicProbs = new DenseMatrix(numItems, numTopics);

        itemNumEachRating = new DenseMatrix(numItems, numRatingLevels);
        itemNumRatings = new DenseVector(numItems);

        for (int i = 0; i < numItems; i++) {
            SparseVector ri = trainMatrix.column(i);

            for (VectorEntry vi : ri) {
                double rui = vi.get();
                int r = ratingScale.indexOf(rui);
                itemNumEachRating.add(i, r, 1);
            }
            itemNumRatings.set(i, ri.size());
        }
        lastLoss = Double.MIN_VALUE;

    }

    @Override
    protected void eStep() {
        for (int i = 0; i < numItems; i++) {
            BigDecimal sum_i = BigDecimal.ZERO;
            SparseVector ri = trainMatrix.column(i);

            BigDecimal[] sum_ik = new BigDecimal[numTopics];
            for (int k = 0; k < numTopics; k++) {
                BigDecimal itemTopicProb = new BigDecimal(topicInitialProbs.get(k));

                for (VectorEntry vi : ri) {
                    double rui = vi.get();
                    int r = ratingScale.indexOf(rui);
                    BigDecimal topicRatingProb = new BigDecimal(topicRatingProbs.get(k, r));

                    itemTopicProb = itemTopicProb.multiply(topicRatingProb);
                }
                sum_ik[k] = itemTopicProb;
                sum_i = sum_i.add(itemTopicProb);
            }
            for (int k = 0; k < numTopics; k++) {
                double zik = sum_ik[k].divide(sum_i, 6, RoundingMode.HALF_UP).doubleValue();
                itemTopicProbs.set(i, k, zik);
            }
        }
    }

    @Override
    protected void mStep() {
        double[] sum_ik = new double[numTopics];
        double sum = 0;

        for (int k = 0; k < numTopics; k++) {
            for (int r = 0; r < numRatingLevels; r++) {
                double numerator = 0.0, denorminator = 0.0;

                for (int i = 0; i < numItems; i++) {
                    double ruk = itemTopicProbs.get(i, k);
                    numerator += ruk * itemNumEachRating.get(i, r);
                    denorminator += ruk * itemNumRatings.get(i);
                }
                topicRatingProbs.set(k, r, numerator / denorminator);
            }

            double sum_i = 0;
            for (int i = 0; i < numItems; i++) {
                double ruk = itemTopicProbs.get(i, k);
                sum_i += ruk;
            }

            sum_ik[k] = sum_i;
            sum += sum_i;
        }

        for (int k = 0; k < numTopics; k++) {
            topicInitialProbs.set(k, sum_ik[k] / sum);
        }

    }

    @Override
    protected boolean isConverged(int iter) {
        double loss = 0.0;

        for (int i = 0; i < numItems; i++) {
            for (int k = 0; k < numTopics; k++) {
                double rik = itemTopicProbs.get(i, k);
                double pi_k = topicInitialProbs.get(k);

                double sum_nl = 0;
                for (int r = 0; r < numRatingLevels; r++) {
                    double nir = itemNumEachRating.get(i, r);
                    double pkr = topicRatingProbs.get(k, r);

                    sum_nl += nir * Math.log(pkr);
                }

                loss += rik * (Math.log(pi_k) + sum_nl);
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
            double pi_k = itemTopicProbs.get(itemIdx, k); // probability that user u belongs to cluster k
            double pred_k = 0;

            for (int r = 0; r < numRatingLevels; r++) {
                double rij = ratingScale.get(r);
                double pkr = topicRatingProbs.get(k, r);

                pred_k += rij * pkr;
            }

            pred += pi_k * pred_k;
        }

        return pred;
    }
}
