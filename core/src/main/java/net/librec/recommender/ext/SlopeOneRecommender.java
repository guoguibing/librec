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
package net.librec.recommender.ext;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.recommender.AbstractRecommender;

/**
 * Weighted Slope One: Lemire and Maclachlan,
 * <strong>
 * Slope One Predictors for Online Rating-Based Collaborative Filtering
 * </strong>, SDM 2005.
 *
 * @author GuoGuibing and Keqiang Wang
 */
@ModelData({"isRating", "slopeone", "devMatrix", "cardMatrix", "trainMatrix"})
public class SlopeOneRecommender extends AbstractRecommender {
    /**
     * matrices for item-item differences with number of occurrences/cardinary
     */
    private DenseMatrix devMatrix, cardMatrix;

    /**
     * initialization
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();

        devMatrix = new DenseMatrix(numItems, numItems);
        cardMatrix = new DenseMatrix(numItems, numItems);
    }

    /**
     * train model
     *
     * @throws LibrecException if error occurs
     */
    @Override
    protected void trainModel() throws LibrecException {
        // compute items' differences
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SparseVector itemRatingsVector = trainMatrix.row(userIdx);
            int[] items = itemRatingsVector.getIndex();

            for (int itemIdx : items) {
                double userItemRating = itemRatingsVector.get(itemIdx);
                for (int comparedItemIdx : items) {
                    if (itemIdx != comparedItemIdx) {
                        double comparedRating = itemRatingsVector.get(comparedItemIdx);
                        devMatrix.add(itemIdx, comparedItemIdx, userItemRating - comparedRating);
                        cardMatrix.add(itemIdx, comparedItemIdx, 1);
                    }
                }
            }
        }

        // normalize differences
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            for (int comparedItemIdx = 0; comparedItemIdx < numItems; comparedItemIdx++) {
                double card = cardMatrix.get(itemIdx, comparedItemIdx);
                if (card > 0) {
                    double sum = devMatrix.get(itemIdx, comparedItemIdx);
                    devMatrix.set(itemIdx, comparedItemIdx, sum / card);
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
     * @throws LibrecException if error occurs
     */
    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        SparseVector itemRatingsVector = trainMatrix.row(userIdx, itemIdx);
        double predictRatings = 0, cardinaryValues = 0;
        for (int comparedItemIdx : itemRatingsVector.getIndex()) {
            double cardinaryValue = cardMatrix.get(itemIdx, comparedItemIdx);
            if (cardinaryValue > 0) {
                predictRatings += (devMatrix.get(itemIdx, comparedItemIdx) + itemRatingsVector.get(comparedItemIdx)) * cardinaryValue;
                cardinaryValues += cardinaryValue;
            }
        }

        return cardinaryValues > 0 ? predictRatings / cardinaryValues : globalMean;
    }
}
