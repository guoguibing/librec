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
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.SocialRecommender;

import java.util.ArrayList;
import java.util.List;

/**
 * Hao Ma, Haixuan Yang, Michael R. Lyu and Irwin King, <strong>SoRec: Social recommendation using probabilistic matrix
 * factorization</strong>, ACM CIKM 2008.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "sorec", "userFactors", "itemFactors"})
public class SoRecRecommender extends SocialRecommender {
    /**
     * adaptive learn rate
     */
    private DenseMatrix userSocialFactors;

    private float regRateSocial, regUserSocial;

    private List<Integer> inDegrees, outDegrees;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        itemFactors.init(1.0);
        regRateSocial = conf.getFloat("rec.rate.social.regularization", 0.01f);
        regUserSocial = conf.getFloat("rec.user.social.regularization", 0.01f);

        userSocialFactors = new DenseMatrix(numUsers, numFactors);
        userSocialFactors.init(1.0);

        inDegrees = new ArrayList<>();
        outDegrees = new ArrayList<>();

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            int in = socialMatrix.columnSize(userIdx);
            int out = socialMatrix.rowSize(userIdx);

            inDegrees.add(in);
            outDegrees.add(out);
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);
            DenseMatrix userSocialTempFactors = new DenseMatrix(numUsers, numFactors);

            // ratings
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = Maths.logistic(predictRating) - Maths.normalize(rating, minRate, maxRate);

                loss += error * error;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                    tempUserFactors.add(userIdx, factorIdx, Maths.logisticGradientValue(predictRating) * error * itemFactorValue + regUser * userFactorValue);
                    tempItemFactors.add(itemIdx, factorIdx, Maths.logisticGradientValue(predictRating) * error * userFactorValue + regItem * itemFactorValue);

                    loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;
                }
            }

            // friends
            for (MatrixEntry matrixEntry : socialMatrix) {
                int userIdx = matrixEntry.row();
                int userSocialIdx = matrixEntry.column();
                double socialValue = matrixEntry.get(); // tuv ~ cik in the original paper
                if (socialValue <= 0)
                    continue;

                double socialPredictRating = DenseMatrix.rowMult(userFactors, userIdx, userSocialFactors, userSocialIdx);

                int userSocialInDegree = inDegrees.get(userSocialIdx); // ~ d-(k)
                int userOutDegree = outDegrees.get(userIdx); // ~ d+(i)
                double weight = Math.sqrt(userSocialInDegree / (userOutDegree + userSocialInDegree + 0.0));

                double socialError = Maths.logistic(socialPredictRating) - weight * socialValue;

                loss += regRateSocial * socialError * socialError;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double userSocialFactorValue = userSocialFactors.get(userSocialIdx, factorIdx);

                    tempUserFactors.add(userIdx, factorIdx, regRateSocial * Maths.logisticGradientValue(socialPredictRating) * socialError * userSocialFactorValue);
                    userSocialTempFactors.add(userSocialIdx, factorIdx, regRateSocial * Maths.logisticGradientValue(socialPredictRating) * socialError * userFactorValue + regUserSocial * userSocialFactorValue);

                    loss += regUserSocial * userSocialFactorValue * userSocialFactorValue;
                }
            }

            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));
            userSocialFactors = userSocialFactors.add(userSocialTempFactors.scale(-learnRate));


            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }
}
