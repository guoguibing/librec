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
package net.librec.recommender.rec.cf.rating;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.MatrixFactorizationRecommender;

/**
 * Biased Matrix Factorization Recommender
 *
 * @author GuoGuibing and Keqiang Wang
 */
@ModelData({"isRating", "biasedMF", "userFactors", "itemFactors", "userBiases","itemBiases"})
public class BiasedMFRecommender extends MatrixFactorizationRecommender {
    /**
     * learning rate
     */
    protected double learnRate;
    /**
     * bias regularization
     */
    protected double regBias;
    /**
     * user biases
     */
    protected DenseVector userBiases;
    /**
     * user biases
     */
    protected DenseVector itemBiases;

    /*
     * (non-Javadoc)
	 *
	 * @see net.librec.recommender.AbstractRecommender#setup()
	 */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        learnRate = conf.getDouble("rec.iteration.learnrate", 0.01);
        regBias = conf.getDouble("rec.bias.regularization", 0.01);

        //initialize the userBiased and itemBiased
        userBiases = new DenseVector(numUsers);
        itemBiases = new DenseVector(numItems);

        userBiases.init(initMean, initStd);
        itemBiases.init(initMean, initStd);
    }

    @Override
    protected void trainModel() throws LibrecException {
//        double loss;
        for (int iter = 1; iter <= numIterations; iter++) {

            for (MatrixEntry matrixEntry : trainMatrix) {

                int userIdx = matrixEntry.row(); // user userIdx
                int itemIdx = matrixEntry.column(); // item itemIdx
                double realRating = matrixEntry.get(); // real rating on item itemIdx rated by user userIdx

                double predictRating = predict(userIdx, itemIdx);
                double error = realRating - predictRating;

                // update user and item bias
                double userBiasValue = userBiases.get(userIdx);
                userBiases.add(userIdx, learnRate * (error - regBias * userBiasValue));

                double itemBiasValue = itemBiases.get(itemIdx);
                itemBiases.add(itemIdx, learnRate * (error - regBias * itemBiasValue));

                //update user and item factors
                for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
                    double userFactorValue = userFactors.get(userIdx, factorIdx);
                    double itemFactorValue = itemFactors.get(itemIdx, factorIdx);

                    userFactors.add(userIdx, factorIdx, learnRate * (error * itemFactorValue - regUser * userFactorValue));
                    itemFactors.add(itemIdx, factorIdx, learnRate * (error * userFactorValue - regItem * itemFactorValue));

                }
            }

        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIdx user index
     * @param itemIdx item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws LibrecException
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx) + userBiases.get(userIdx) + itemBiases.get(itemIdx) + globalMean;
    }

}
