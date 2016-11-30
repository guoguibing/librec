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
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedList;

import static net.librec.math.algorithm.Maths.g;
import static net.librec.math.algorithm.Maths.gd;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI 2013.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRating", "trustmf", "trusterUserTrusterFactors", "trusterUserTrusteeFactors","trusteeUserTrusterFactors","trusteeUserTrusteeFactors","model"})
public class TrustMFRecommender extends SocialMFRecommender {
    /**
     * learning  rate
     */
    protected double learnRate;
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
        learnRate = conf.getDouble("rec.iteration.learnrate", 0.01);
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
        trusterUserTrusterFactors = new DenseMatrix(numItems, numFactors);
        trusterUserTrusteeFactors = new DenseMatrix(numUsers, numFactors);
        trusterItemFactors = new DenseMatrix(numUsers, numFactors);

        trusterUserTrusterFactors.init();
        trusterUserTrusteeFactors.init();
        trusterItemFactors.init();
    }

    protected void initTe() {
        trusteeUserTrusterFactors = new DenseMatrix(numItems, numFactors);
        trusteeUserTrusteeFactors = new DenseMatrix(numUsers, numFactors);
        trusteeItemFactors = new DenseMatrix(numUsers, numFactors);

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
     */
    protected void TrusterMF() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            // gradients of B, V, W
            DenseMatrix userTrusterGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix userTrusteeGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix itemGradients = new DenseMatrix(numItems, numFactors);
            // rate matrix
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx, false);
                double error = g(predictRating) - normalize(rating);

                double csgd = gd(rating) * error;
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    userTrusterGradients.add(userIdx, factorIdx, csgd * trusterItemFactors.get(itemIdx, factorIdx)
                            + regUser * trusterUserTrusterFactors.get(userIdx, factorIdx));
                    itemGradients.add(itemIdx, factorIdx, csgd * trusterUserTrusterFactors.get(userIdx, factorIdx)
                            + regItem * trusterItemFactors.get(itemIdx, factorIdx));
                }
            }

            // social matrix
            for (MatrixEntry matrixEntry : socialMatrix) {
                int userIdx = matrixEntry.row();
                int userSocialIdx = matrixEntry.column();
                double socialValue = matrixEntry.get();

                if (socialValue > 0) {
                    double preddictSocialValue = DenseMatrix.rowMult(trusterUserTrusterFactors, userIdx, trusterUserTrusteeFactors, userSocialIdx);
                    double socialError = g(preddictSocialValue) - socialValue;

                    double csgd = gd(preddictSocialValue) * socialError;

                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        userTrusterGradients.add(userIdx, factorIdx, regSocial * csgd * trusterUserTrusteeFactors.get(userSocialIdx, factorIdx)
                                + regUser * trusterUserTrusterFactors.get(userIdx, factorIdx));
                        userTrusteeGradients.add(userSocialIdx, factorIdx, regSocial * csgd * trusterUserTrusterFactors.get(userIdx, factorIdx)
                                + regUser * trusterUserTrusteeFactors.get(userSocialIdx, factorIdx));
                    }
                }
            }

            trusterUserTrusterFactors.addEqual(userTrusterGradients.scale(-learnRate));
            trusterItemFactors.addEqual(itemGradients.scale(-learnRate));
            trusterUserTrusteeFactors.addEqual(userTrusteeGradients.scale(-learnRate));
        }
    }

    /**
     * Build TrusteeMF model: We*Ve
     */
    protected void TrusteeMF() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            // gradients of B, V, W
            DenseMatrix userTrusterGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix userTrusteeGradients = new DenseMatrix(numUsers, numFactors);
            DenseMatrix itemGradients = new DenseMatrix(numItems, numFactors);

            // rate matrix
            for (MatrixEntry matrixEntry : trainMatrix) {
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();
                double rating = matrixEntry.get();

                double predictRating = predict(userIdx, itemIdx, false);
                double error = g(rating) - normalize(rating);
                double csgd = gd(predictRating) * error;
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    userTrusteeGradients.add(userIdx, factorIdx, csgd * trusteeItemFactors.get(itemIdx, factorIdx)
                            + regUser * trusteeUserTrusteeFactors.get(userIdx, factorIdx));
                    itemGradients.add(itemIdx, factorIdx, csgd * trusteeUserTrusteeFactors.get(userIdx, factorIdx)
                            + regItem * trusteeItemFactors.get(itemIdx, factorIdx));

                }
            }

            // social matrix
            for (MatrixEntry matrixEntry : socialMatrix) {
                int userSocialIdx = matrixEntry.row();
                int userIdx = matrixEntry.column();
                double socialValue = matrixEntry.get();
                if (socialValue > 0) {
                    double predictSocialValue = DenseMatrix.rowMult(trusteeUserTrusterFactors, userSocialIdx, trusteeUserTrusteeFactors, userIdx);
                    double socialError = g(predictSocialValue) - socialValue;

                    double csgd = gd(predictSocialValue) * socialError;
                    for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                        userTrusteeGradients.add(userIdx, factorIdx, regSocial * csgd * trusteeUserTrusterFactors.get(userSocialIdx, factorIdx)
                                + regUser * trusteeUserTrusteeFactors.get(userIdx, factorIdx));
                        userTrusterGradients.add(userSocialIdx, factorIdx, regSocial * csgd * trusteeUserTrusteeFactors.get(userIdx, factorIdx)
                                + regUser * trusteeUserTrusterFactors.get(userSocialIdx, factorIdx));
                    }
                }
            }

            trusteeUserTrusterFactors.addEqual(userTrusterGradients.scale(-learnRate));
            trusteeItemFactors.addEqual(itemGradients.scale(-learnRate));
            trusteeUserTrusteeFactors.addEqual(userTrusteeGradients.scale(-learnRate));
        }
    }


    /**
     * This is the method used by the paper authors
     */
    protected void updateLRate(int iter) {
        if (iter == 10)
            learnRate *= 0.6;
        else if (iter == 30)
            learnRate *= 0.333;
        else if (iter == 100)
            learnRate *= 0.5;
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
