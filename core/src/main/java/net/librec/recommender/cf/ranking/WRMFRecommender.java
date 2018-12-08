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

import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;
import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.*;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <h3>WRMF: Weighted Regularized Matrix Factorization.</h3>
 * <p>
 * This implementation refers to the method proposed by Hu et al. at ICDM 2008.
 * <ul>
 * <li><strong>Binary ratings:</strong> Pan et al., One-class Collaborative Filtering, ICDM 2008.</li>
 * <li><strong>Real ratings:</strong> Hu et al., Collaborative filtering for implicit feedback datasets, ICDM 2008.</li>
 * </ul>
 *
 * @author Keqiang Wang
 */
@ModelData({"isRanking", "wrmf", "userFactors", "itemFactors", "trainMatrix"})
public class WRMFRecommender extends MatrixFactorizationRecommender {
    /**
     * confidence weight coefficient
     */
    protected float weightCoefficient;


    @Override
    public void setup() throws LibrecException {
        super.setup();
        weightCoefficient = conf.getFloat("rec.wrmf.weight.coefficient", 4.0f);

        // weighted the train rating matrix as confidence matrix
        weightMatrix();
    }

    public double weight(double value) {
//        return  weightCoefficient * value;
        return Math.log(1.0 + Math.pow(10, weightCoefficient) * value);
    }

    public void weightMatrix() {
        Double2DoubleOpenHashMap ratingWeightMap = new Double2DoubleOpenHashMap();
        for (double rating : ratingScale) {
            ratingWeightMap.putIfAbsent(rating, weight(rating));
        }

        for (MatrixEntry matrixEntry : trainMatrix) {
            matrixEntry.set(ratingWeightMap.get(matrixEntry.get()));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        // To be consistent with the symbols in the paper
        DenseMatrix X = userFactors, Y = itemFactors;

//        DenseMatrix factorMatrix = new DenseMatrix(numFactors, numFactors);
//        DenseVector userFactorVector;
//        DenseVector itemFactorVector;

        List<Integer> userList = new ArrayList<>(numUsers);
        List<Integer> itemList = new ArrayList<>(numItems);
        for (int userIndex=0; userIndex <  numUsers; userIndex++){
            userList.add(userIndex);
        }

        for (int itemIndex=0; itemIndex <  numItems; itemIndex++){
            itemList.add(itemIndex);
        }

        for (int iter = 1; iter <= numIterations; iter++) {
            // Step 1: update user factors;
            DenseMatrix YtY = Y.transpose().times(Y);
            userList.parallelStream().forEach(userIndex->{
//            for (int userIndex = 0; userIndex < numUsers; userIndex++) {
                DenseVector itemFactorVector;
                SequentialSparseVector itemRatingVector = trainMatrix.row(userIndex);
                DenseMatrix factorMatrix = new DenseMatrix(numFactors, numFactors);

                DenseVector YtCuPu = new VectorBasedDenseVector(numFactors);
                for (Vector.VectorEntry vectorEntry : itemRatingVector) {
                    int itemIndex = vectorEntry.index();
                    double weight = vectorEntry.get() + 1.0D;
                    itemFactorVector = itemFactors.row(itemIndex);
                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        YtCuPu.plus(factorIndex, itemFactorVector.get(factorIndex) * weight);
                    }
                }

                factorMatrix.assign((rowIndex, columnIndex, value) -> YtY.get(rowIndex, columnIndex) + regUser);

                for (Vector.VectorEntry vectorEntry : itemRatingVector) {
                    int itemIndex = vectorEntry.index();
                    double weight = vectorEntry.get();
                    itemFactorVector = itemFactors.row(itemIndex);
                    for (int rowIndex = 0; rowIndex < numFactors; rowIndex++) {
                        double temp = itemFactorVector.get(rowIndex) * weight;
                        for (int columnIndex = 0; columnIndex < numFactors; columnIndex++) {
                            factorMatrix.plus(rowIndex, columnIndex, temp * itemFactorVector.get(columnIndex));
                        }
                    }
                }

                DenseMatrix Wu = factorMatrix.inverse();
                DenseVector xu = Wu.times(YtCuPu);
                // udpate user factors
                X.set(userIndex, xu);
            });

            // Step 2: update item factors;
            DenseMatrix XtX = X.transpose().times(X);

            itemList.parallelStream().forEach(itemIndex->{
//            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {

                SequentialSparseVector userRatingVector = trainMatrix.viewColumn(itemIndex);
                DenseVector XtCiPu = new VectorBasedDenseVector(numFactors);
                DenseMatrix factorMatrix = new DenseMatrix(numFactors, numFactors);
                DenseVector userFactorVector;
                for (Vector.VectorEntry vectorEntry : userRatingVector) {
                    int userIndex = vectorEntry.index();
                    double weight = vectorEntry.get() + 1.0D;
                    userFactorVector = userFactors.row(userIndex);
                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        XtCiPu.plus(factorIndex, userFactorVector.get(factorIndex) * weight);
                    }
                }

                factorMatrix.assign((rowIndex, columnIndex, value) -> XtX.get(rowIndex, columnIndex) + regItem);

                for (Vector.VectorEntry vectorEntry : userRatingVector) {
                    int userIndex = vectorEntry.index();
                    double weight = vectorEntry.get();
                    userFactorVector = userFactors.row(userIndex);
                    for (int rowIndex = 0; rowIndex < numFactors; rowIndex++) {
                        double temp = userFactorVector.get(rowIndex) * weight;
                        for (int columnIndex = 0; columnIndex < numFactors; columnIndex++) {
                            factorMatrix.plus(rowIndex, columnIndex, temp * userFactorVector.get(columnIndex));
                        }
                    }
                }

                DenseMatrix Wi = factorMatrix.inverse();

                DenseVector yi = Wi.times(XtCiPu);
                // udpate item factors
                Y.set(itemIndex, yi);
            });

            if (verbose) {
                LOG.info(getClass() + " runs at iteration = " + iter + " " + new Date());
            }
        }
    }
}
