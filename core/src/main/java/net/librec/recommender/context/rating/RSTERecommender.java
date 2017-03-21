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
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
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
                    double rating =  vectorEntry.get();
                    double norRating = Maths.normalize(rating, minRate, maxRate);

                    // compute directly to speed up calculation
                    double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                    double sum = 0.0;
                    for (int k : userSocialIndice)
                        sum += userSoicalValues.get(k) * DenseMatrix.rowMult(userFactors, k, itemFactors, itemIdx);

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

                        tempUserFactors.add(userIdx, factorIdx, userDeriValue);
                        tempItemFactors.add(itemIdx, factorIdx, itemDeriValue);

                        loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;
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
                        double finalPredictRating = userSocialRatio * predictRating + (1 - userSocialRatio) * socialPredictRating;

                        // double pred = predict(p, j, false);
                        double error = Maths.logistic(finalPredictRating) - Maths.normalize(socialItemValues.get(socialItemIdx), minRate, maxRate);
                        double deriValue = Maths.logisticGradientValue(finalPredictRating) * error * socialUserValues.get(socialUserIdx);

                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                            tempUserFactors.add(userSocialIdx, factorIdx, (1 - userSocialRatio) * deriValue * itemFactors.get(socialItemIdx, factorIdx));
                    }
                }
            }




            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));

            loss *= 0.5d;





            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
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

        predictRating = userSocialRatio * predictRating + (1 - userSocialRatio) * soicalPredictRatting;

        return predictRating;
    }
}
