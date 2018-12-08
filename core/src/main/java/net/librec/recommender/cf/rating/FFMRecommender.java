/**
 * Copyright (C) 2017 LibRec
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
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.TensorEntry;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.math.structure.VectorBasedSequentialSparseVector;
import net.librec.recommender.FactorizationMachineRecommender;

import java.util.HashMap;

/**
 * Field-aware Factorization Machines
 * Yuchin Juan, "Field Aware Factorization Machines for CTR Prediction", 10th ACM Conference on Recommender Systems, 2016
 *
 * @author Li Wenxi and Tan Jiale
 */


@ModelData({"isRanking", "ffm", "W", "V", "W0", "k"})
public class FFMRecommender extends FactorizationMachineRecommender {
    /**
     * learning rate of stochastic gradient descent
     */
    private double learnRate;
    /**
     * record the <feature: filed>
     */
    private HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        //Matrix for p * (factor * filed)
        V = new DenseMatrix(p, k * trainTensor.numDimensions);
        // init factors with small value
        V.init(0, 0.1);


        //init the map for feature of filed
        int colindex = 0;
        for (int dim = 0; dim < trainTensor.numDimensions; dim++) {
            for (int index = 0; index < trainTensor.dimensions[dim]; index++) {
                map.put(colindex + index, dim);
            }
            colindex += trainTensor.dimensions[dim];
        }

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

            int userDimension = trainTensor.getUserDimension();
            int itemDimension = trainTensor.getItemDimension();
            for (TensorEntry me : trainTensor) {
                int[] entryKeys = me.keys();
                VectorBasedSequentialSparseVector vector = tenserKeysToFeatureVector(entryKeys);

                double rate = me.get();
                double pred = predict(entryKeys[userDimension], entryKeys[itemDimension], vector);

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
                        double oldVlf = V.get(l, map.get(l) + f);
                        double hVlf = 0;
                        double xl = ve.get();
                        for (VectorEntry ve2 : vector) {
                            int j = ve2.index();
                            if (j != l) {
                                hVlf += xl * V.get(j, f) * ve2.get();
                            }
                        }
                        double gradVlf = gradLoss * hVlf + regF * oldVlf;
                        V.plus(l, map.get(l) + f, -learnRate * gradVlf);
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

    protected double predict(int userId, int itemId, VectorBasedSequentialSparseVector x) throws LibrecException {
        double res = 0;
        // global bias
        res += w0;

        // 1-way interaction
        for (VectorEntry ve : x) {
            double val = ve.get();
            int ind = ve.index();
            res += val * W.get(ind);
        }

        // 2-way interaction
        for (int f = 0; f < k; f++) {
            double sum = 0;
            for (VectorEntry vi : x) {
                for (VectorEntry vj : x) {
                    double xi = vi.get();
                    double xj = vj.get();
                    int i = vi.index();
                    int j = vj.index();
                    if (i == j) continue;
                    double vifj = V.get(i, map.get(j) + f);
                    double vjfi = V.get(j, map.get(i) + f);
                    sum += vifj * vjfi * xi * xj;
                }
            }
            res += sum;
        }

        return res;
    }


    @Deprecated
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return 0.0;
    }
}
