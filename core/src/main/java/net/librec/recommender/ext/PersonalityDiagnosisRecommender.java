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

import com.clearspring.analytics.util.Lists;
import com.google.common.collect.Maps;
import net.librec.common.LibrecException;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.recommender.MatrixRecommender;

import java.util.List;
import java.util.Map;

/**
 * Related Work:
 * <ul>
 * <li><a href= "http://www.cs.carleton.edu/cs_comps/0607/recommend/recommender/pd.html">A brief introduction to Personality
 * Diagnosis</a></li>
 * </ul>
 *
 * @author guoguibing and Keqiang Wang
 */
public class PersonalityDiagnosisRecommender extends MatrixRecommender {
    /**
     * Gaussian noise: 2.5 suggested in the paper
     */
    private float sigma;

    /**
     * prior probability
     */
    private double prior;

    List<Map<Integer, Integer>> userItemsPosList = Lists.newArrayList();

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
        // plus to adapt to 3.0
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            Map<Integer, Integer> itemIndexPosMap = Maps.newHashMap();
            int[] itemIndices = trainMatrix.row(userIdx).getIndices();
            for (int i = 0; i < itemIndices.length; i++) {
                itemIndexPosMap.put(itemIndices[i], i);
            }
            userItemsPosList.add(itemIndexPosMap);
        }
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

        SequentialSparseVector itemRatingsVector = trainMatrix.row(userIdx);
        SequentialSparseVector userRatingsVector = trainMatrix.column(itemIdx);

        int index = 0;
        for (double ratingValue : ratingScale) {

            double prob = 0.0;
            for (Vector.VectorEntry vectorEntry : userRatingsVector) {
                // other users who rated item j
                int ratedUserIdx = vectorEntry.index();
                double userRatingValue = vectorEntry.get();

                SequentialSparseVector ratedItemRatingsVector = trainMatrix.row(ratedUserIdx);
                Map<Integer, Integer> currItemIndexPosMap = userItemsPosList.get(ratedUserIdx);
                double prod = 1.0;
                for (Vector.VectorEntry itemRatingEntry : itemRatingsVector) {
                    int ratedItemIdx = itemRatingEntry.index();
                    double itemRatingValue = itemRatingEntry.get();
                    if (currItemIndexPosMap.get(ratedItemIdx) != null) {
                        double ratedItemRatingValue = ratedItemRatingsVector.getAtPosition(currItemIndexPosMap.get(ratedItemIdx));
                        prod *= gaussian(itemRatingValue, ratedItemRatingValue, sigma);
                    }
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
