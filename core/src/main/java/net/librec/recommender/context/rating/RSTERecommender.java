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
package net.librec.recommender.context.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.recommender.SocialRecommender;

/**
 * Hao Ma, Irwin King and Michael R. Lyu, <strong>Learning to Recommend with Social Trust Ensemble</strong>, SIGIR 2009.<br>
 * <p>
 * This method is quite time-consuming when dealing with the social influence part.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "rste", "userFactors", "itemFactors", "userSocialRatio", "socialMatrix"})
public class RSTERecommender extends SocialRecommender {
    private float userSocialRatio;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        itemFactors.init(1.0);
        userSocialRatio = conf.getFloat("rec.user.social.ratio", 0.8f);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            // ratings
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                SequentialSparseVector userSoicalValues = socialMatrix.row(userIdx);

                double weightSocialSum = 0;
                for (VectorEntry ve : userSoicalValues) {
                    double socialValue = ve.get();
                    weightSocialSum += socialValue;
                }

                double[] sumUserSocialFactor = new double[numFactors];
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    for (VectorEntry ve : userSoicalValues) {
                        int userSocialIdx = ve.index();
                        double socialValue = ve.get();
                        sumUserSocialFactor[factorIdx] += socialValue * userFactors.get(userSocialIdx, factorIdx);
                    }
                }

                for (VectorEntry vectorEntry : trainMatrix.row(userIdx)) {
                    int itemIdx = vectorEntry.index();
                    double rating = vectorEntry.get();
                    double norRating = Maths.normalize(rating, minRate, maxRate);

                    // compute directly to speed up calculation
                    double predictRating = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                    double sum = 0.0;
                    for (VectorEntry ve : userSoicalValues) {
                        int userSocialIdx = ve.index();
                        double socialValue = ve.get();
                        sum += socialValue * userFactors.row(userSocialIdx).dot(itemFactors.row(itemIdx));
                    }

                    double socialPredictRating = weightSocialSum > 0 ? sum / weightSocialSum : 0;
                    double finalPredictRating = userSocialRatio * predictRating + (1 - userSocialRatio) * socialPredictRating;

                    // prediction error
                    double error = Maths.logistic(finalPredictRating) - norRating;

                    loss += error * error;

                    double deriValue = Maths.logisticGradientValue(finalPredictRating) * error;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double userFactorValue = userFactors.get(userIdx, factorIdx);
                        double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                        double userDeriValue = userSocialRatio * deriValue * itemFactorValue + regUser * userFactorValue;
                        double userSocialFactorValue = weightSocialSum > 0 ? sumUserSocialFactor[factorIdx] / weightSocialSum : 0;
                        double itemDeriValue = deriValue * (userSocialRatio * userFactorValue + (1 - userSocialRatio) * userSocialFactorValue) + regItem * itemFactorValue;

                        tempUserFactors.plus(userIdx, factorIdx, userDeriValue);
                        tempItemFactors.plus(itemIdx, factorIdx, itemDeriValue);

                        loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;
                    }
                }
            }

            // social
            for (int userSocialIdx = 0; userSocialIdx < numUsers; userSocialIdx++) {

                SequentialSparseVector socialUserValues = socialMatrix.column(userSocialIdx);
                for (VectorEntry ve_1: socialUserValues) {
                    int socialUserIdx = ve_1.index();
                    double socialUserValue = ve_1.get();

                    SequentialSparseVector socialItemValues = trainMatrix.row(socialUserIdx);
                    SequentialSparseVector socialUserSoicalValues = socialMatrix.row(socialUserIdx);
                    int[] socialUserSocialIndices = socialUserSoicalValues.getIndices();

                    for (VectorEntry ve_2: socialItemValues) {
                        int socialItemIdx = ve_2.index();
                        double socialItemValue = ve_2.get();

                        // compute prediction for user-item (p, j)
                        double predictRating = userFactors.row(socialUserIdx).dot(itemFactors.row(socialItemIdx));
                        double sum = 0.0, socialWeightSum = 0.0;
                        for (VectorEntry ve_3: socialUserSoicalValues) {
                            int socialUserSocialIdx = ve_3.index();
                            double socialUserSocialValue = ve_3.get();
                            sum += socialUserSocialValue * userFactors.row(socialUserSocialIdx).dot(itemFactors.row(socialItemIdx));
                            socialWeightSum += socialUserSocialValue;
                        }

                        double socialPredictRating = socialWeightSum > 0 ? sum / socialWeightSum : 0;
                        double finalPredictRating = userSocialRatio * predictRating + (1 - userSocialRatio) * socialPredictRating;

                        // double pred = predict(p, j, false);
                        double error = Maths.logistic(finalPredictRating) - Maths.normalize(socialItemValue, minRate, maxRate);
                        double deriValue = Maths.logisticGradientValue(finalPredictRating) * error * socialUserValue;

                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                            tempUserFactors.plus(userSocialIdx, factorIdx, (1 - userSocialRatio) * deriValue * itemFactors.get(socialItemIdx, factorIdx));
                    }
                }
            }

            userFactors = userFactors.plus(tempUserFactors.times(-learnRate));
            itemFactors = itemFactors.plus(tempItemFactors.times(-learnRate));

            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    protected double predict(int userIdx, int itemIdx) {
        double predictRating = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
        double sum = 0.0, socialWeightSum = 0.0;
        SequentialSparseVector userSocialVector = socialMatrix.row(userIdx);

        for (VectorEntry ve : userSocialVector) {
            int userSoicalIdx = ve.index();
            double userSocialValue = ve.get();

            sum += userSocialValue * userFactors.row(userSoicalIdx).dot(itemFactors.row(itemIdx));
            socialWeightSum += userSocialValue;
        }

        double soicalPredictRatting = socialWeightSum > 0 ? sum / socialWeightSum : 0;

        predictRating = userSocialRatio * predictRating + (1 - userSocialRatio) * soicalPredictRatting;

        return predictRating;
    }


}
