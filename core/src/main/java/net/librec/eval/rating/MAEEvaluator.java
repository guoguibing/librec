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
package net.librec.eval.rating;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.item.UserItemRatingEntry;

import java.util.Iterator;

/**
 * MAE: mean absolute error
 *
 * @author zhanghaidong and Keqiang Wang
 */

public class MAEEvaluator extends AbstractRecommenderEvaluator {

    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
        if (testMatrix == null) {
            return 0.0;
        }
        double mae = 0.0;

        Iterator<MatrixEntry> testMatrixIter = testMatrix.iterator();
        Iterator<UserItemRatingEntry> recommendedEntryIter = recommendedList.entryIterator();
        int testSize = 0;

        while (testMatrixIter.hasNext()) {

            if (recommendedEntryIter.hasNext()) {
                MatrixEntry testMatrixEntry = testMatrixIter.next();
                UserItemRatingEntry userItemRatingEntry = recommendedEntryIter.next();

                if (testMatrixEntry.row() == userItemRatingEntry.getUserIdx()
                        && testMatrixEntry.column() == userItemRatingEntry.getItemIdx()) {

                    double realRating = testMatrixEntry.get();
                    double predictRating = userItemRatingEntry.getValue();
                    mae += Math.abs(realRating - predictRating);
                    testSize++;

                } else {
                    throw new IndexOutOfBoundsException("index of recommendedList does not equal testMatrix index");
                }

            } else {
                throw new IndexOutOfBoundsException("index size of recommendedList does not equal testMatrix index size");
            }
        }

        return testSize > 0 ? mae / testSize : 0.0d;
    }

}
