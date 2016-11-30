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
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.SocialRecommender;

import java.util.ArrayList;
import java.util.List;

import static net.librec.math.algorithm.Maths.g;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust propagation for recommendation in social
 * networks</strong>, RecSys 2010.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "sorec", "userFactors", "itemFactors"})
public class SoRecRecommender extends SocialRecommender {
    /**
     * adaptive learn rate
     */
    protected double learnRate;
    private DenseMatrix userSocialFactors;
    private float regRateSocial, regUserSocial;
    private List<Integer> inDegrees, outDegrees;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getDouble("rec.iteration.learnrate", 0.01);
        regRateSocial = conf.getFloat("reg.rate.social.regularization", 0.01f);
        regUserSocial = conf.getFloat("reg.user.social.regularization", 0.01f);

        userSocialFactors = new DenseMatrix(numUsers, numFactors);
        userSocialFactors.init(initMean, initStd);

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

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);
            DenseMatrix userSocialTempFactors = new DenseMatrix(numUsers, numFactors);

            // ratings
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = g(predictRating) - normalize(rating);


                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    tempUserFactors.add(userIdx, factorIdx, gd(predictRating) * error * itemFactors.get(itemIdx, factorIdx) + regUser * userFactors.get(userIdx, factorIdx));
                    tempItemFactors.add(itemIdx, factorIdx, gd(predictRating) * error * userFactors.get(userIdx, factorIdx) + regItem * itemFactors.get(itemIdx, factorIdx));
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

                double socialError = g(socialPredictRating) - weight * socialValue;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double userSocialFactorValue = userSocialFactors.get(userSocialIdx, factorIdx);

                    tempUserFactors.add(userIdx, factorIdx, regRateSocial * gd(socialPredictRating) * socialError * userSocialFactorValue);
                    userSocialTempFactors.add(userSocialIdx, factorIdx, regRateSocial * gd(socialPredictRating) * socialError * userFactorValue + regUserSocial * userSocialFactorValue);
                }
            }

            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));
            userSocialFactors = userSocialFactors.add(userSocialTempFactors.scale(-learnRate));

        }
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
