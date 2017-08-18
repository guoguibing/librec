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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.FactorizationMachineRecommender;

import java.util.Iterator;

/**
 * Factorization Machine Recommender via Alternating Least Square
 *
 * @author Tang Jiaxi and Ma Chen
 */

@ModelData({"isRanking", "fmals", "W", "V", "W0", "k"})
public class FMALSRecommender extends FactorizationMachineRecommender {
    /**
     * parameter matrix
     */
    private DenseMatrix Q; //  n x k
    /**
     * train appender matrix
     */
    private SparseMatrix trainFeatureMatrix;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        // init Q
        Q = new DenseMatrix(n, k);

        // construct training appender matrix
        Table<Integer, Integer, Double> trainTable = HashBasedTable.create();
        for (int i = 0; i < n; i++) {
            int[] ratingKeys = trainTensor.keys(i);
            int colPrefix = 0;
            for (int j = 0; j < ratingKeys.length; j++) {
                int indexOfFeatureVector = colPrefix + ratingKeys[j];
                colPrefix += trainTensor.dimensions[j];
                trainTable.put(i, indexOfFeatureVector, 1.0);
            }
        }
        trainFeatureMatrix = new SparseMatrix(n, p, trainTable);
    }

    @Override
    protected void trainModel() throws LibrecException {
        // precomputing Q and errors, for efficiency
        DenseVector errors = new DenseVector(n);
        int ind = 0;
        int userDimension = trainTensor.getUserDimension();
        int itemDimension = trainTensor.getItemDimension();
        for (TensorEntry me : trainTensor) {
            int[] entryKeys = me.keys();
            SparseVector x = tenserKeysToFeatureVector(entryKeys);

            double rate = me.get();
            double pred = predict(entryKeys[userDimension], entryKeys[itemDimension], x);

            double err = rate - pred;
            errors.set(ind, err);

            for (int f = 0; f < k; f++) {
                double sum_q = 0;
                for (VectorEntry ve : x) {
                    int l = ve.index();
                    double x_val = ve.get();
                    sum_q += V.get(l, f) * x_val;
                }
                Q.set(ind, f, sum_q);
            }
            ind++;
        }

        /**
         * parameter optimized by using formula in [1].
         * errors updated by using formula: error_new = error_old + theta_old*h_old - theta_new * h_new;
         * reference:
         * [1]. Rendle, Steffen, "Factorization Machines with libFM." ACM Transactions on Intelligent Systems and Technology, 2012.
         */

        for (int iter = 0; iter < numIterations; iter++) {
            lastLoss = loss;
            loss = 0.0;
            // global bias
            double numerator = 0;
            double denominator = 0;
            for (int i = 0; i < n; i++) {
                double h_theta = 1;
                numerator += w0 * h_theta * h_theta + h_theta * errors.get(i);
                denominator += h_theta;
            }
            denominator += regW0;
            double newW0 = numerator / denominator;

            // System.out.println("original:" + errors.sum());

            // update errors
            for (int i = 0; i < n; i++) {
                double oldErr = errors.get(i);
                double newErr = oldErr + (w0 - newW0);
                errors.set(i, newErr);

                loss += oldErr * oldErr;
            }

            // update w0
            w0 = newW0;

            loss += regW0 * w0 * w0;

            // System.out.println("after 0-way:" + errors.sum());

            // 1-way interactions
            for (int l = 0; l < p; l++) {
                double oldWl = W.get(l);
                numerator = 0;
                denominator = 0;

                Iterator<VectorEntry> rowIter = trainFeatureMatrix.rowIterator(l);
                while (rowIter.hasNext()) {
                    VectorEntry vectorEntry = rowIter.next();
                    double h_theta = vectorEntry.get();
                    int i = vectorEntry.index();
                    numerator += oldWl * h_theta * h_theta + h_theta * errors.get(i);
                    denominator += h_theta * h_theta;

                }

                denominator += regW;
                double newWl = numerator / denominator;


                // update errors
                rowIter = trainFeatureMatrix.rowIterator(l);
                while (rowIter.hasNext()) {
                    VectorEntry vectorEntry = rowIter.next();
                    int i = vectorEntry.index();

                    double oldErr = errors.get(i);
                    double newErr = oldErr + (oldWl - newWl) * vectorEntry.get();
                    errors.set(i, newErr);

                }

                // update W
                W.set(l, newWl);

                loss += regW * oldWl * oldWl;
            }

            // System.out.println("after 1-way:" + errors.sum());

            // 2-way interactions
            for (int f = 0; f < k; f++) {
                for (int l = 0; l < p; l++) {
                    double oldVlf = V.get(l, f);
                    numerator = 0;
                    denominator = 0;
                    Iterator<VectorEntry> rowIter = trainFeatureMatrix.rowIterator(l);
                    while (rowIter.hasNext()) {
                        VectorEntry vectorEntry = rowIter.next();
                        int i = vectorEntry.index();
                        double x_val = vectorEntry.get();
                        double h_theta = x_val * (Q.get(i, f) - oldVlf * x_val);
                        numerator += oldVlf * h_theta * h_theta + h_theta * errors.get(i);
                        denominator += h_theta * h_theta;

                    }
                    denominator += regF;
                    double newVlf = numerator / denominator;

                    // update errors and Q
                    rowIter = trainFeatureMatrix.rowIterator(l);
                    while (rowIter.hasNext()) {
                        VectorEntry vectorEntry = rowIter.next();
                        int i = vectorEntry.index();
                        double x_val = vectorEntry.get();

                        double oldQif = Q.get(i, f);
                        double update = (newVlf - oldVlf) * x_val;
                        double newQif = oldQif + update;

                        double h_theta_old = x_val * (oldQif - oldVlf * x_val);
                        double h_theta_new = x_val * (newQif - newVlf * x_val);

                        double oldErr = errors.get(i);
                        double newErr = oldErr + oldVlf * h_theta_old - newVlf * h_theta_new;

                        errors.set(i, newErr);
                        Q.set(i, f, newQif);
                    }


                    // update V
                    V.set(l, f, newVlf);

//                    DenseVector errorGround = computeGroundError();
//                    errors = errorGround;
                    loss += regF * oldVlf * oldVlf;
                }
                //System.out.println("temp:" + errors.sum());
            }

            // System.out.println("after 2-way:" + errors.sum());
            if (isConverged(iter) && earlyStop)
                break;
        }
    }

    /**
     * This kind of prediction function cannot be applied to Factorization Machine.
     * <p>
     * Using the predict() in FactorizationMachineRecommender class instead of this method.
     */
    @Deprecated
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return 0.0;
    }
}
