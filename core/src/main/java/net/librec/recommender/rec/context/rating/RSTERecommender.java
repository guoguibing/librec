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
package net.librec.recommender.rec.context.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.recommender.rec.SocialRecommender;

import static net.librec.math.algorithm.Maths.g;

/**
 * Hao Ma, Irwin King and Michael R. Lyu, <strong>Learning to Recommend with Social Trust Ensemble</strong>, SIGIR 2009.<br>
 * <p>
 * <p>
 * This method is quite time-consuming when dealing with the social influence part.
 * </p>
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "rste", "userFactors", "itemFactors", "alpha", "socialMatrix"})
public class RSTERecommender extends SocialRecommender {
    protected double learnRate;
    private float alpha;


    @Override
    public void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getDouble("rec.iteration.learnrate", 0.01);
        alpha = conf.getFloat("-alpha");

    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            // ratings
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                SparseVector userSoicalValues = socialMatrix.row(userIdx);
                int[] userSocialIndice = userSoicalValues.getIndex();

                double weightSocialSum = 0;
                for (int userSoicalIdx : userSocialIndice)
                    weightSocialSum += userSoicalValues.get(userSoicalIdx);

                double[] sumUserSocialFactor = new double[numFactors];
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (int userSoicalIdx : userSocialIndice)
                        sumUserSocialFactor[factorIdx] += userSoicalValues.get(userSoicalIdx) * userFactors.get(userSoicalIdx, factorIdx);
                }

                for (VectorEntry vectorEntry : trainMatrix.row(userIdx)) {
                    int itemIdx = vectorEntry.index();
                    double rating = vectorEntry.get();
                    double norRating = normalize(rating);

                    // compute directly to speed up calculation
                    double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                    double sum = 0.0;
                    for (int k : userSocialIndice)
                        sum += userSoicalValues.get(k) * DenseMatrix.rowMult(userFactors, k, itemFactors, itemIdx);

                    double socialPredictRating = weightSocialSum > 0 ? sum / weightSocialSum : 0;
                    double pred = alpha * predictRating + (1 - alpha) * socialPredictRating;

                    // prediction error
                    double error = g(pred) - norRating;


                    double csgd = gd(pred) * error;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        double usgd = alpha * csgd * itemFactorValue + regUser * userFactorValue;

                        double userSocialFactorValue = weightSocialSum > 0 ? sumUserSocialFactor[factorIdx] / weightSocialSum : 0;
                        double jsgd = csgd * (alpha * userFactorValue + (1 - alpha) * userSocialFactorValue) + regItem * itemFactorValue;

                        tempUserFactors.add(userIdx, factorIdx, usgd);
                        tempItemFactors.add(itemIdx, factorIdx, jsgd);
                    }
                }
            }

            // social
            for (int userSocialIdx = 0; userSocialIdx < numUsers; userSocialIdx++) {

                SparseVector socialUserValues = socialMatrix.column(userSocialIdx);
                for (int socialUserIdx : socialUserValues.getIndex()) {
                    if (socialUserIdx >= numUsers)
                        continue;

                    SparseVector socialItemValues = trainMatrix.row(socialUserIdx);
                    SparseVector socialUserSoicalValues = socialMatrix.row(socialUserIdx);
                    int[] socialUserSocialIndices = socialUserSoicalValues.getIndex();

                    for (int socialItemIdx : socialItemValues.getIndex()) {

                        // compute prediction for user-item (p, j)
                        double predictRating = DenseMatrix.rowMult(userFactors, socialUserIdx, itemFactors, socialItemIdx);
                        double sum = 0.0, socialWeightSum = 0.0;
                        for (int socialUserSocialIdx : socialUserSocialIndices) {
                            double socialUserSocialValue = socialUserSoicalValues.get(socialUserSocialIdx);
                            sum += socialUserSocialValue * DenseMatrix.rowMult(userFactors, socialUserSocialIdx, itemFactors, socialItemIdx);
                            socialWeightSum += socialUserSocialValue;
                        }
                        double socialPredictRating = socialWeightSum > 0 ? sum / socialWeightSum : 0;
                        double pred = alpha * predictRating + (1 - alpha) * socialPredictRating;

                        // double pred = predict(p, j, false);
                        double error = g(pred) - normalize(socialItemValues.get(socialItemIdx));
                        double csgd = gd(pred) * error * socialUserValues.get(socialItemIdx);

                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                            tempUserFactors.add(userSocialIdx, factorIdx, (1 - alpha) * csgd * itemFactors.get(socialItemIdx, factorIdx));
                    }
                }
            }

            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));
        }
    }

    protected double predict(int userIdx, int itemIdx) {
        double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
        double sum = 0.0, socialWeightSum = 0.0;
        SparseVector userSocialVector = socialMatrix.row(userIdx);

        for (int userSoicalIdx : userSocialVector.getIndex()) {
            double userSocialValue = userSocialVector.get(userSoicalIdx);
            sum += userSocialValue * DenseMatrix.rowMult(userFactors, userSoicalIdx, itemFactors, itemIdx);
            socialWeightSum += userSocialValue;
        }

        double soicalPredictRatting = socialWeightSum > 0 ? sum / socialWeightSum : 0;

        predictRating = alpha * predictRating + (1 - alpha) * soicalPredictRatting;

        return predictRating;
    }

    /**
     * normalize a rating to the region (0, 1)
     */
    protected double normalize(double rating) {
        return (rating - minRate) / (maxRate - minRate);
    }

    /**
     * gradient value of logistic function g(x)
     */
    protected double gd(double x) {
        return g(x) * g(-x);
    }
}
