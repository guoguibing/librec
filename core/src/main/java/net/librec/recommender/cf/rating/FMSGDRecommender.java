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

package net.librec.recommender.cf.rating;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.TensorEntry;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.math.structure.VectorBasedSequentialSparseVector;
import net.librec.recommender.FactorizationMachineRecommender;


/**
 * Stochastic Gradient Descent with Square Loss
 * Rendle, Steffen, "Factorization Machines", Proceedings of the 10th IEEE International Conference on Data Mining, 2010
 * Rendle, Steffen, "Factorization Machines with libFM", ACM Transactions on Intelligent Systems and Technology, 2012
 *
 * @author Jiaxi Tang and Ma Chen
 */

@ModelData({"isRanking", "fmsgd", "W", "V", "W0", "k"})
public class FMSGDRecommender extends FactorizationMachineRecommender {
    /**
     * learning rate of stochastic gradient descent
     */
    private double learnRate;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getDouble("rec.iterator.learnRate");
    }

    @Override
    protected void trainModel() throws LibrecException {
        if (!isRanking) {
            buildRatingModel();
        }
    }

    private void buildRatingModel() throws LibrecException {
        for (int iter = 0; iter < numIterations; iter++) {
            loss = 0.0;

            for (TensorEntry me : trainTensor) {
                int[] entryKeys = me.keys();
                VectorBasedSequentialSparseVector vector = tenserKeysToFeatureVector(entryKeys);

                double rate = me.get();
                double pred = predict(entryKeys);

                double err = pred - rate;
                loss += err * err;
                double gradLoss = err;

                // global bias
                loss += regW0 * w0 * w0;

                double hW0 = 1;
                double gradW0 = gradLoss * hW0 + regW0 * w0;

                // update w0
                w0 += -learnRate * gradW0;

                // 1-way interactions
                for (VectorEntry ve : vector) {
                    int l = ve.index();
                    double oldWl = W.get(l);
                    double hWl = ve.get();
                    double gradWl = gradLoss * hWl + regW * oldWl;
                    W.plus(l, -learnRate * gradWl);

                    loss += regW * oldWl * oldWl;

                    // 2-way interactions
                    for (int f = 0; f < k; f++) {
                        double oldVlf = V.get(l, f);
                        double hVlf = 0;
                        double xl = ve.get();
                        for (VectorEntry ve2 : vector) {
                            int j = ve2.index();
                            if (j != l) {
                                hVlf += xl * V.get(j, f) * ve2.get();
                            }
                        }

                        double gradVlf = gradLoss * hVlf + regF * oldVlf;
                        V.plus(l, f, -learnRate * gradVlf);
                        loss += regF * oldVlf * oldVlf;
                    }
                }

            }

            loss *= 0.5;

            if (isConverged(iter) && earlyStop) {
                break;
            }
            lastLoss = loss;
        }
    }
}
