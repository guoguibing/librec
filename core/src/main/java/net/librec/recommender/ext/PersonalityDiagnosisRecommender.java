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
package net.librec.recommender.ext;

import net.librec.common.LibrecException;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.AbstractRecommender;

/**
 * Related Work:
 * <ul>
 * <li><a href= "http://www.cs.carleton.edu/cs_comps/0607/recommend/recommender/pd.html">A brief introduction to Personality
 * Diagnosis</a></li>
 * </ul>
 *
 * @author guoguibing and Keqiang Wang
 */
public class PersonalityDiagnosisRecommender extends AbstractRecommender {
    /**
     * Gaussian noise: 2.5 suggested in the paper
     */
    private float sigma;

    /**
     * prior probability
     */
    private double prior;

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        prior = 1.0 / numUsers;
        sigma = conf.getFloat("rec.PersonalityDiagnosis.sigma");
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {

    }


    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double[] scaleProbs = new double[ratingScale.size()];

        SparseVector itemRatingsVector = trainMatrix.row(userIdx);
        SparseVector userRatingsVector = trainMatrix.column(itemIdx);

        int index = 0;
        for (double ratingValue : ratingScale) {

            double prob = 0.0;
            for (VectorEntry vectorEntry : userRatingsVector) {
                // other users who rated item j
                int ratedUserIdx = vectorEntry.index();
                double userRatingValue = vectorEntry.get();

                SparseVector ratedItemRatingsVector = trainMatrix.row(ratedUserIdx);
                double prod = 1.0;
                for (VectorEntry itemRatingEntry : itemRatingsVector) {
                    int ratedItemIdx = itemRatingEntry.index();
                    double itemRatingValue = itemRatingEntry.get();
                    double ratedItemRatingValue = ratedItemRatingsVector.get(ratedItemIdx);
                    if (ratedItemRatingValue > 0)
                        prod *= gaussian(itemRatingValue, ratedItemRatingValue, sigma);
                }
                prob += gaussian(ratingValue, userRatingValue, sigma) * prod;
            }

            prob *= prior;
            scaleProbs[index++] = prob;
        }

        int maxIdx = 0;
        double max = Integer.MIN_VALUE;
        for (int ratingIdx = 0; ratingIdx < scaleProbs.length; ratingIdx++) {
            if (scaleProbs[ratingIdx] > max) {
                max = scaleProbs[ratingIdx];
                maxIdx = ratingIdx;
            }
        }
        return ratingScale.get(maxIdx);
    }

    /**
     * @param x     input value
     * @param mu    mean of normal distribution
     * @param sigma standard deviation of normation distribution
     * @return a gaussian value with mean {@code mu} and standard deviation {@code sigma};
     */
    protected double gaussian(double x, double mu, double sigma) {
        return Math.exp(-0.5 * Math.pow(x - mu, 2) / (sigma * sigma));
    }
}
