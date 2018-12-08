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
import net.librec.math.structure.*;

/**
 * SVD++ Recommender
 * Yehuda Koren, <strong>Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model</strong>, KDD 2008.
 *
 * @author GuoGuibing and Keqiang Wang
 */
@ModelData({"isRating", "svdplusplus", "userFactors", "itemFactors", "userBiases", "itemBiases", "impItemFactors", "trainMatrix"})
public class SVDPlusPlusRecommender extends BiasedMFRecommender {
    /**
     * item implicit feedback factors, "imp" string means implicit
     */
    protected DenseMatrix impItemFactors;

    /**
     * implicit item regularization
     */
    private double regImpItem;

    private DenseVector factorVector;


    /*
     * (non-Javadoc)
	 *
	 * @see net.librec.recommender.AbstractRecommender#setup()
	 */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        regImpItem = conf.getDouble("rec.impItem.regularization", 0.015d);

        impItemFactors = new DenseMatrix(numItems, numFactors);
        impItemFactors.init(initMean, initStd);
        factorVector = new VectorBasedDenseVector(numFactors);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iterationStep = 1; iterationStep <= numIterations; iterationStep++) {
            loss = 0D;
            for (int userIndex = 0; userIndex < numUsers; userIndex++) {
                SequentialSparseVector userVector = trainMatrix.row(userIndex);
                if (userVector.size() == 0) {
                    continue;
                }
                double[] steps = new double[numFactors];
                factorVector.assign((index, value) -> 0.0D);
                for (Vector.VectorEntry vectorEntry : userVector) {
                    factorVector.assign((index, value) -> impItemFactors.row(vectorEntry.index()).get(index) + value);
                }
                double scale = Math.pow(userVector.getNumEntries(), -0.5);
                factorVector.assign((index, value) -> value * scale);

                for (Vector.VectorEntry vectorEntry : userVector) {
                    int itemIndex = vectorEntry.index();

                    double error = vectorEntry.get() - predict(userIndex, itemIndex, factorVector);
                    loss += error * error;
                    // update user and item bias
                    double userBias = userBiases.get(userIndex);
                    userBiases.plus(userIndex, learnRate * (error - regBias * userBias));
                    loss += regBias * userBias * userBias;
                    double itemBias = itemBiases.get(itemIndex);
                    itemBiases.plus(itemIndex, learnRate * (error - regBias * itemBias));
                    loss += regBias * itemBias * itemBias;

//                    // update user and item factors
                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        double userFactor = userFactors.get(userIndex, factorIndex);
                        double itemFactor = itemFactors.get(itemIndex, factorIndex);
                        userFactors.plus(userIndex, factorIndex, learnRate * (error * itemFactor - regUser * userFactor));
                        itemFactors.plus(itemIndex, factorIndex, learnRate * (error * (userFactor + factorVector.get(factorIndex)) - regItem * itemFactor));
                        loss += regUser * userFactor * userFactor + regItem * itemFactor * itemFactor;

                        steps[factorIndex] += error * itemFactor * scale;
                    }
                }
                int size = userVector.getNumEntries();
                for (Vector.VectorEntry vectorEntry : userVector) {
                    int index = vectorEntry.index();
                    for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
                        double factor = impItemFactors.get(index, factorIndex);
                        impItemFactors.plus(index, factorIndex, learnRate * (steps[factorIndex] - regImpItem * factor * size));
                        loss += regImpItem * factor * factor * size;
                    }
                }
            }
            loss *= 0.5d;

            if (isConverged(iterationStep) && earlyStop) {
                break;
            }
            updateLRate(iterationStep);
        }
    }


    private double predict(int userIndex, int itemIndex, DenseVector factorVector) {
        double value = userBiases.get(userIndex) + itemBiases.get(itemIndex) + globalMean;
        DenseVector userFactorVector = userFactors.row(userIndex);
        DenseVector itemFactorVector = itemFactors.row(itemIndex);
        // sum with user factors
        for (int index = 0; index < numFactors; index++) {
            value += (factorVector.get(index) + userFactorVector.get(index)) * itemFactorVector.get(index);
        }
        return value;
    }

    @Override
    protected double predict(int userIndex, int itemIndex) {
        SequentialSparseVector userVector = trainMatrix.row(userIndex);
        factorVector.assign((index, value) -> 0.0D);
        // sum of implicit feedback factors of userIdx with weight Math.sqrt(1.0
        // / userItemsList.get(userIdx).cardinality())
        for (Vector.VectorEntry vectorEntry : userVector) {
            factorVector.assign((index, value) -> impItemFactors.row(vectorEntry.index()).get(index) + value);
        }
        double scale = Math.sqrt(userVector.getNumEntries());
        if (scale > 0D) {
            factorVector.assign((index, value) -> value / scale);
        }
        return predict(userIndex, itemIndex, factorVector);
    }
}
