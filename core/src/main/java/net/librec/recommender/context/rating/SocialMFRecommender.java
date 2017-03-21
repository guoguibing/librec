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
import net.librec.math.structure.SparseVector;
import net.librec.recommender.SocialRecommender;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust propagation for recommendation in social
 * networks</strong>, RecSys 2010.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "socialmf", "userFactors", "itemFactors"})
public class SocialMFRecommender extends SocialRecommender {
    @Override
    public void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        itemFactors.init(1.0);

    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            // rated items
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx, false);
                double error = Maths.logistic(predictRating) - normalize(rating);

                loss += error * error;

                double deriValue = Maths.logisticGradientValue(predictRating) * error;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);
                    tempUserFactors.add(userIdx, factorIdx, deriValue * itemFactorValue + regUser * userFactorValue);
                    tempItemFactors.add(itemIdx, factorIdx, deriValue * userFactorValue + regItem * itemFactorValue);

                    loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;
                }
            }

            // social regularization
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                SparseVector userTrustVector = socialMatrix.row(userIdx);
                int numTrust = userTrustVector.getCount();
                if (numTrust == 0)
                    continue;

                double[] sumNNs = new double[numFactors];
                for (int trustUserIdx : userTrustVector.getIndex()) {
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                        sumNNs[factorIdx] += socialMatrix.get(userIdx, trustUserIdx) * userFactors.get(trustUserIdx, factorIdx);
                }

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double diffValue = userFactors.get(userIdx, factorIdx) - sumNNs[factorIdx] / numTrust;
                    tempUserFactors.add(userIdx, factorIdx, regSocial * diffValue);

                    loss += regSocial * diffValue * diffValue;
                }

                // those who trusted user u
                SparseVector userTrustedVector = socialMatrix.column(userIdx);
                int numTrusted = userTrustedVector.getCount();
                for (int trustedUserIdx : userTrustedVector.getIndex()) {
                    double trustedValue = socialMatrix.get(trustedUserIdx, userIdx);

                    SparseVector trustedTrustVector = socialMatrix.row(trustedUserIdx);
                    double[] sumDiffs = new double[numFactors];
                    for (int trustedTrustUserIdx : trustedTrustVector.getIndex()) {
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                            sumDiffs[factorIdx] += socialMatrix.get(trustedUserIdx, trustedTrustUserIdx)
                                    * userFactors.get(trustedTrustUserIdx, factorIdx);
                    }

                    numTrust = trustedTrustVector.getCount();
                    if (numTrust > 0)
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++)
                            tempUserFactors.add(userIdx, factorIdx, -regSocial * (trustedValue / numTrusted) *
                                    (userFactors.get(trustedUserIdx, factorIdx) - sumDiffs[factorIdx] / numTrust));
                }
            }

            // update user factors
            userFactors = userFactors.add(tempUserFactors.scale(-learnRate));
            itemFactors = itemFactors.add(tempItemFactors.scale(-learnRate));

            loss *= 0.5d;



            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);


        }
    }


}
