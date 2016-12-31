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
package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.List;

/**
 * Shi et al., <strong>List-wise learning to rank with matrix factorization for
 * collaborative filtering</strong>, RecSys 2010.
 *
 * @author Bin Wu(wubin@gs.zzu.edu.cn)
 */
public class ListwiseMFRecommender extends MatrixFactorizationRecommender {
    public DenseVector userExp;

    protected void setup() throws LibrecException {
        super.setup();
        userExp = new DenseVector(numUsers);
        for (MatrixEntry matrixentry : trainMatrix) {
            int userIdx = matrixentry.row();
            double realRating = matrixentry.get();

            userExp.add(userIdx, Math.exp(realRating));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {

            loss = 0.0d;
            for (MatrixEntry matrixentry : trainMatrix) {

                int userIdx = matrixentry.row();
                int itemIdx = matrixentry.column();
                double realRating = matrixentry.get();

                double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
                double uexp = 0;

                List<Integer> items = trainMatrix.getColumns(userIdx);
                for (int item : items) {
                    uexp += Math.exp(DenseMatrix.rowMult(userFactors, userIdx, itemFactors, item));
                }

                loss -= Math.exp(realRating) / userExp.get(userIdx) * Math.log(Math.exp(predictRating) / uexp);

                // update factors
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);
                    double delta_user = (Math.exp(realRating) / userExp.get(userIdx) - Math.exp(predictRating) / uexp)
                            * Maths.logisticGradientValue(predictRating) * itemFactorValue - regUser * userFactorValue;
                    double delta_item = (Math.exp(realRating) / userExp.get(userIdx) - Math.exp(predictRating) / uexp)
                            * Maths.logisticGradientValue(predictRating) * userFactorValue - regItem * itemFactorValue;

                    userFactors.add(userIdx, factorIdx, learnRate * delta_user);
                    itemFactors.add(itemIdx, factorIdx, learnRate * delta_item);

                    loss += 0.5 * regUser * userFactorValue * userFactorValue + 0.5 * regItem * itemFactorValue * itemFactorValue;
                }

            }
            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        } // end of training
    }
}