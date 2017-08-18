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
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.SocialRecommender;

/**
 * Hao Ma, Dengyong Zhou, Chao Liu, Michael R. Lyu and Irwin King, <strong>Recommender systems with social
 * regularization</strong>, WSDM 2011.<br>
 * <p>
 * In the original paper, this method is named as "SR2_pcc". For consistency, we rename it as "SoReg" as used by some
 * other papers such as: Tang et al., <strong>Exploiting Local and Global Social Context for Recommendation</strong>,
 * IJCAI 2013.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "soreg", "userFactors", "itemFactors"})
public class SoRegRecommender extends SocialRecommender {
    private SymmMatrix userSocialCorrs;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        userFactors.init(1.0);
        itemFactors.init(1.0);

        userSocialCorrs = context.getSimilarity().getSimilarityMatrix();

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            for (int simUserIdx = userIdx + 1; simUserIdx < numUsers; simUserIdx++) {
                if (userSocialCorrs.contains(userIdx, simUserIdx)) {
                    double sim = userSocialCorrs.get(userIdx, simUserIdx);
                    sim = (1.0 + sim) / 2;
                    userSocialCorrs.set(userIdx, simUserIdx, sim);
                }
            }
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;

            // temp data
            DenseMatrix tempUserFactors = new DenseMatrix(numUsers, numFactors);
            DenseMatrix tempItemFactors = new DenseMatrix(numItems, numFactors);

            // ratings
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double realRating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx);
                double error = predictRating - realRating;

                loss += error * error;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                    tempUserFactors.add(userIdx, factorIdx, error * itemFactorValue + regUser * userFactorValue);
                    tempItemFactors.add(itemIdx, factorIdx, error * userFactorValue + regItem * itemFactorValue);

                    loss += regUser * userFactorValue * userFactorValue + regItem * itemFactorValue * itemFactorValue;
                }
            }

            // friends
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                // out links: F+
                SparseVector userOutLinks = socialMatrix.row(userIdx);

                for (int userOutIdx : userOutLinks.getIndex()) {
                    double userOutSim = userSocialCorrs.get(userIdx, userOutIdx);
                    if (!Double.isNaN(userOutSim)) {
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double errorOut = userFactors.get(userIdx, factorIdx) - userFactors.get(userOutIdx, factorIdx);
                            tempUserFactors.add(userIdx, factorIdx, regSocial * userOutSim * errorOut);

                            loss += regSocial * userOutSim * errorOut * errorOut;
                        }
                    }
                }

                // in links: F-
                SparseVector userInLinks = socialMatrix.column(userIdx);
                for (int userInIdx : userInLinks.getIndex()) {
                    double userInSim = userSocialCorrs.get(userIdx, userInIdx);
                    if (!Double.isNaN(userInSim)) {
                        for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                            double errorIn = userFactors.get(userIdx, factorIdx) - userFactors.get(userInIdx, factorIdx);
                            tempUserFactors.add(userIdx, factorIdx, regSocial * userInSim * errorIn);

                            loss += regSocial * userInSim * errorIn * errorIn;
                        }
                    }
                }

            } // end of for loop
            userFactors.addEqual(tempUserFactors.scale(-learnRate));
            itemFactors.addEqual(tempItemFactors.scale(-learnRate));

            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx. It is useful for evalution which requires predictions are
     * bounded.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @param bound   whether there is a bound
     * @return predictive rating for user userIdx on item itemIdx with bound
     * @throws LibrecException if error occurs during predicting
     */
    protected double predict(int userIdx, int itemIdx, boolean bound) throws LibrecException {
        double predictRating = predict(userIdx, itemIdx);

        if (bound) {
            if (predictRating > maxRate) {
                predictRating = maxRate;
            } else if (predictRating < minRate) {
                predictRating = minRate;
            }
        }

        return predictRating;
    }
}
