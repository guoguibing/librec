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
package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.RecommendedList;

import java.util.List;

/**
 * IdealDCGEvaluator
 *
 * @author WangYuFeng
 */
public class IdealDCGEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param testMatrix
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {

        double iDCG = 0.0;

        int numUsers = testMatrix.numRows();
        int nonZeroNumUsers = 0;
        for (int userID = 0; userID < numUsers; userID++) {
            double idcg = 0.0;

            List<Integer> testListByUser = testMatrix.getColumns(userID);
            if (testListByUser.size() > 0) {
                // calculate the IDCG
                int numItemsInTestList = testListByUser.size();
                for (int i = 0; i < numItemsInTestList; i++) {
                    idcg += 1 / Maths.log(i + 2.0, 2);
                }
                iDCG += idcg;
                nonZeroNumUsers++;
            }
        }

        return nonZeroNumUsers > 0 ? iDCG / nonZeroNumUsers : 0.0d;
    }

}
