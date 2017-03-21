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
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.SocialRecommender;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI 2013.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "trustmf", "trusterUserTrusterFactors", "trusterUserTrusteeFactors", "trusteeUserTrusterFactors", "trusteeUserTrusteeFactors", "model"})
public class TrustMFRecommender extends SocialRecommender {
    /**
     * truster model
     */
    protected DenseMatrix trusterUserTrusterFactors, trusterUserTrusteeFactors, trusterItemFactors;

    /**
     * trustee model
     */
    protected DenseMatrix trusteeUserTrusterFactors, trusteeUserTrusteeFactors, trusteeItemFactors;

    /**
     * model selection identifier
     */
    protected String model;

    @Override
    public void setup() throws LibrecException {
        super.setup();
        model = conf.get("rec.social.model", "T");
//        algoName = "TrustMF (" + model + ")";
        switch (model) {
            case "Tr":
                initTr();
                break;
            case "Te":
                initTe();
                break;
            case "T":
            default:
                initTr();
                initTe();
        }
    }

    protected void initTr() {
        trusterUserTrusterFactors = new DenseMatrix(numUsers, numFactors);
        trusterUserTrusteeFactors = new DenseMatrix(numUsers, numFactors);
        trusterItemFactors = new DenseMatrix(numItems, numFactors);

        trusterUserTrusterFactors.init();
        trusterUserTrusteeFactors.init();
        trusterItemFactors.init();
    }

    protected void initTe() {
        trusteeUserTrusterFactors = new DenseMatrix(numUsers, numFactors);
        trusteeUserTrusteeFactors = new DenseMatrix(numUsers, numFactors);
        trusteeItemFactors = new DenseMatrix(numItems, numFactors);

        trusteeUserTrusterFactors.init();
        trusteeUserTrusteeFactors.init();
        trusteeItemFactors.init();
    }

    @Override
    protected void trainModel() throws LibrecException {
        switch (model) {
            case "Tr":
                TrusterMF();
                break;
            case "Te":
                TrusteeMF();
                break;
            case "T":
            default:
                TrusterMF();
                TrusteeMF();
        }
    }

    /**
     * Build TrusterMF model: Br*Vr
     *
     * @throws LibrecException if error occurs
     */
    protected void TrusterMF() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;

            // gradients of trusterUserTrusterFactors, trusterUserTrusteeFactors, trusterItemFactors
            DenseMatrix userTrusterGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix userTrusteeGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix itemGradients = new DenseMatrix(numItems, numFactors);
            // rate matrix
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx, false);
                double error = Maths.logistic(predictRating) - normalize(rating);

                loss += error * error;

                double deriValue = Maths.logisticGradientValue(predictRating) * error;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double trusterUserTrusterFactorValue = trusterUserTrusterFactors.get(userIdx, factorIdx);
                    double trusterItemFactorValue = trusterItemFactors.get(itemIdx, factorIdx);

                    userTrusterGradients.add(userIdx, factorIdx, deriValue * trusterItemFactorValue
                            + regUser * trusterUserTrusterFactorValue);
                    itemGradients.add(itemIdx, factorIdx, deriValue * trusterUserTrusterFactorValue
                            + regItem * trusterItemFactorValue);

                    loss += regUser * trusterUserTrusterFactorValue * trusterUserTrusterFactorValue +
                            regItem * trusterItemFactorValue * trusterItemFactorValue;
                }
            }

            // social matrix
            for (MatrixEntry matrixEntry : socialMatrix) {
                int userIdx = matrixEntry.row();
                int userSocialIdx = matrixEntry.column();
                double socialValue = matrixEntry.get();

                if (socialValue > 0) {
                    double preddictSocialValue = DenseMatrix.rowMult(trusterUserTrusterFactors, userIdx, trusterUserTrusteeFactors, userSocialIdx);
                    double socialError = Maths.logistic(preddictSocialValue) - socialValue;

                    loss += regSocial * socialError * socialError;

                    double deriValue = Maths.logisticGradientValue(preddictSocialValue) * socialError;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double trusterUserTrusterFactorValue = trusterUserTrusterFactors.get(userIdx, factorIdx);
                        double trusterUserTrusteeFactorValue = trusterUserTrusteeFactors.get(userSocialIdx, factorIdx);

                        userTrusterGradients.add(userIdx, factorIdx, regSocial * deriValue * trusterUserTrusteeFactorValue
                                + regUser * trusterUserTrusterFactorValue);
                        userTrusteeGradients.add(userSocialIdx, factorIdx, regSocial * deriValue * trusterUserTrusterFactorValue
                                + regUser * trusterUserTrusteeFactorValue);

                        loss += regUser * trusterUserTrusterFactorValue * trusterUserTrusterFactorValue +
                                regUser * trusterUserTrusteeFactorValue * trusterUserTrusteeFactorValue;
                    }
                }
            }

            trusterUserTrusterFactors.addEqual(userTrusterGradients.scale(-learnRate));
            trusterItemFactors.addEqual(itemGradients.scale(-learnRate));
            trusterUserTrusteeFactors.addEqual(userTrusteeGradients.scale(-learnRate));

            loss *= 0.5d;


            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    /**
     * Build TrusteeMF model: We*Ve
     *
     * @throws LibrecException if error occurs
     */
    protected void TrusteeMF() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0d;

            // gradients of trusteeUserTrusterFactors, trusteeUserTrusteeFactors, trusteeItemFactors
            DenseMatrix userTrusterGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix userTrusteeGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix itemGradients = new DenseMatrix(numItems, numFactors);

            // rate matrix
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx, false);
                double error = Maths.logistic(predictRating) - normalize(rating);

                loss += error * error;

                double deriValue = Maths.logisticGradientValue(predictRating) * error;

                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double trusteeUserTrusteeFactorValue = trusteeUserTrusteeFactors.get(userIdx, factorIdx);
                    double trusteeItemFactorValue = trusteeItemFactors.get(itemIdx, factorIdx);

                    userTrusteeGradients.add(userIdx, factorIdx, deriValue * trusteeItemFactorValue + regUser * trusteeUserTrusteeFactorValue);
                    itemGradients.add(itemIdx, factorIdx, deriValue * trusteeUserTrusteeFactorValue + regItem * trusteeItemFactorValue);

                    loss += regUser * trusteeUserTrusteeFactorValue * trusteeUserTrusteeFactorValue +
                            regItem * trusteeItemFactorValue * trusteeItemFactorValue;
                }
            }

            // social matrix
            for (MatrixEntry matrixEntry : socialMatrix) {
                int userSocialIdx = matrixEntry.row();
                int userIdx = matrixEntry.column();
                double socialValue = matrixEntry.get();
                if (socialValue > 0) {
                    double predictSocialValue = DenseMatrix.rowMult(trusteeUserTrusterFactors, userSocialIdx, trusteeUserTrusteeFactors, userIdx);
                    double socialError = Maths.logistic(predictSocialValue) - socialValue;

                    loss += regSocial * socialError * socialError;

                    double deriValue = Maths.logisticGradientValue(predictSocialValue) * socialError;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        double trusteeUserTrusteeFactorValue = trusteeUserTrusteeFactors.get(userIdx, factorIdx);
                        double trusteeUserTrusterFactorValue = trusteeUserTrusterFactors.get(userSocialIdx, factorIdx);

                        userTrusteeGradients.add(userIdx, factorIdx, regSocial * deriValue * trusteeUserTrusterFactorValue
                                + regUser * trusteeUserTrusteeFactorValue);
                        userTrusterGradients.add(userSocialIdx, factorIdx, regSocial * deriValue * trusteeUserTrusteeFactorValue
                                + regUser * trusteeUserTrusterFactorValue);

                        loss += regUser * trusteeUserTrusteeFactorValue * trusteeUserTrusteeFactorValue +
                                regUser * trusteeUserTrusterFactorValue * trusteeUserTrusterFactorValue;
                    }
                }
            }

            trusteeUserTrusterFactors.addEqual(userTrusterGradients.scale(-learnRate));
            trusteeItemFactors.addEqual(itemGradients.scale(-learnRate));
            trusteeUserTrusteeFactors.addEqual(userTrusteeGradients.scale(-learnRate));

            loss *= 0.5d;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }


    /**
     * This is the method used by the paper authors
     *
     * @param iter number of iteration
     */
    protected void updateLRate(int iter) {
        if (iter == 10)
            learnRate *= 0.6;
        else if (iter == 30)
            learnRate *= 0.333;
        else if (iter == 100)
            learnRate *= 0.5;
        lastLoss = loss;
    }


    @Override
    protected double predict(int userIdx, int itemIdx) {
        double predictRating;
        switch (model) {
            case "Tr":
                predictRating = DenseMatrix.rowMult(trusterUserTrusterFactors, userIdx, trusterItemFactors, itemIdx);
                break;
            case "Te":
                predictRating = DenseMatrix.rowMult(trusteeUserTrusteeFactors, userIdx, trusteeItemFactors, itemIdx);
                break;
            case "T":
            default:
                DenseVector userVector = trusterUserTrusterFactors.row(userIdx).add(trusteeUserTrusteeFactors.row(userIdx, false));
                DenseVector itemVector = trusterItemFactors.row(itemIdx).add(trusteeItemFactors.row(itemIdx, false));
                predictRating = userVector.inner(itemVector) / 4;
        }

        return predictRating;
    }
}
