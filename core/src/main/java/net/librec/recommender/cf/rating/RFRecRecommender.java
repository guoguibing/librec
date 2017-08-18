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

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

/**
 * Gedikli et al., <strong>RF-Rec: Fast and Accurate Computation of
 * Recommendations based on Rating Frequencies</strong>, IEEE (CEC) 2011,
 * Luxembourg, 2011, pp. 50-57. <br>
 * <p>
 * <strong>Remark:</strong> This implementation does not support half-star
 * ratings.
 * @author bin wu(Email:wubin@gs.zzu.edu.cn)
 */
public class RFRecRecommender extends MatrixFactorizationRecommender {
    /**
     * The average ratings of users
     */
    private DenseVector userAverages;

    /**
     * The average ratings of items
     */
    private DenseVector itemAverages;

    /**
     * The number of ratings per rating value per user
     */
    private DenseMatrix userRatingFrequencies;

    /**
     * The number of ratings per rating value per item
     */
    private DenseMatrix itemRatingFrequencies;

    /**
     * User weights learned by the gradient solver
     */
    private DenseVector userWeights;

    /**
     * Item weights learned by the gradient solver.
     */
    private DenseVector itemWeights;

    protected void setup() throws LibrecException {
        super.setup();
        // Calculate the average ratings
        userAverages = new DenseVector(numUsers);
        itemAverages = new DenseVector(numItems);
        userWeights = new DenseVector(numUsers);
        itemWeights = new DenseVector(numItems);

        for (int u = 0; u < numUsers; u++) {
            userAverages.set(u, trainMatrix.row(u).mean());
            userWeights.set(u, 0.6 + Randoms.uniform() * 0.01);
        }
        for (int j = 0; j < numItems; j++) {
            itemAverages.set(j, trainMatrix.column(j).mean());
            itemWeights.set(j, 0.4 + Randoms.uniform() * 0.01);
        }
        // Calculate the frequencies.
        // Users,items
        userRatingFrequencies = new DenseMatrix(numUsers, numRates);
        itemRatingFrequencies = new DenseMatrix(numItems, numRates);
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            int realRating = (int) matrixEntry.get();
            userRatingFrequencies.add(userIdx, realRating, 1);
            itemRatingFrequencies.add(itemIdx, realRating, 1);
        }
        userWeights = new DenseVector(numUsers);
        itemWeights = new DenseVector(numItems);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double realRating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = realRating - predictRating;

                double userWeight = userWeights.get(userIdx) + learnRate * (error - regUser * userWeights.get(userIdx));
                userWeights.set(userIdx, userWeight);

                // Gradient-Step on item weights.
                double itemWeight = itemWeights.get(itemIdx) + learnRate * (error - regItem * itemWeights.get(itemIdx));
                itemWeights.set(itemIdx, itemWeight);
            }
        }
    }

    /**
     * Returns 1 if the rating is similar to the rounded average value
     *
     * @param avg    the average
     * @param rating the rating
     * @return 1 when the values are equal
     */
    private int isAvgRating(double avg, int rating) {
        return Math.round(avg) == rating ? 1 : 0;
    }

    public double predict(int userIdx, int itemIdx) {

        double estimate = globalMean;
        float enumeratorUser = 0;
        float denominatorUser = 0;
        float enumeratorItem = 0;
        float denominatorItem = 0;
        if (userRatingFrequencies.row(userIdx).sum() > 0 && itemRatingFrequencies.row(itemIdx).sum() > 0
                && userAverages.get(userIdx) > 0 && itemAverages.get(itemIdx) > 0) {
            // Go through all the possible rating values
            for (int r = 0; r < ratingScale.size(); ++r) {
                int ratingValue = (int) Math.round(ratingScale.get(r));
                // user component
                int tmpUser = 0;
                double frequencyInt = userRatingFrequencies.get(userIdx, ratingValue);
                int frequency = (int) frequencyInt;
                tmpUser = frequency + 1 + isAvgRating(userAverages.get(userIdx), ratingValue);
                enumeratorUser += tmpUser * ratingValue;
                denominatorUser += tmpUser;

                // item component
                int tmpItem = 0;
//                frequency = 0;
                frequencyInt = itemRatingFrequencies.get(itemIdx, ratingValue);
                frequency = (int) frequencyInt;
                tmpItem = frequency + 1 + isAvgRating(itemAverages.get(itemIdx), ratingValue);
                enumeratorItem += tmpItem * ratingValue;
                denominatorItem += tmpItem;
            }

            double w_u = userWeights.get(userIdx);
            double w_i = itemWeights.get(itemIdx);
            float pred_ui_user = enumeratorUser / denominatorUser;
            float pred_ui_item = enumeratorItem / denominatorItem;
            estimate = (float) w_u * pred_ui_user + (float) w_i * pred_ui_item;

        } else {
            // if the user or item weren't known in the training phase...
            if (userRatingFrequencies.row(userIdx).sum() == 0 || userAverages.get(userIdx) == 0) {
                double iavg = itemAverages.get(itemIdx);
                if (iavg != 0) {
                    return iavg;
                } else {

                    return globalMean;
                }
            }
            if (itemRatingFrequencies.row(itemIdx).sum() == 0 || itemAverages.get(itemIdx) == 0) {
                double uavg = userAverages.get(userIdx);
                if (uavg != 0) {
                    return uavg;
                } else {
                    // Some heuristic -> a bit above the average rating
                    return globalMean;
                }
            }
        }
        return estimate;
    }
}
