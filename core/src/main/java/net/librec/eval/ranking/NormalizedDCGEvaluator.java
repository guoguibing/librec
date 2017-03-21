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
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * NormalizedDCGEvaluator @topN
 *
 * @author WangYuFeng
 */
public class NormalizedDCGEvaluator extends AbstractRecommenderEvaluator {

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

        double nDCG = 0.0;
        int maxNumTestItemsByUser = conf.getInt("rec.eval.item.test.maxnum", testMatrix.numColumns());
        int idcgsSize = Math.min(maxNumTestItemsByUser, topN);
        List<Double> idcgs = new ArrayList<>(idcgsSize + 1);
        idcgs.add(0.0d);
        for (int index = 0; index < idcgsSize; index++) {
            idcgs.add(1.0d / Maths.log(index + 2, 2) + idcgs.get(index));
        }
        int numUsers = testMatrix.numRows();
        int nonZeroNumUsers = 0;
        for (int userID = 0; userID < numUsers; userID++) {
            Set<Integer> testSetByUser = testMatrix.getColumnsSet(userID);
            if (testSetByUser.size() > 0) {

                double dcg = 0.0;
                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);

                // calculate DCG
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int itemID = recommendListByUser.get(indexOfItem).getKey();
                    if (!testSetByUser.contains(itemID)) {
                        continue;
                    }

                    int rank = indexOfItem + 1;
                    dcg += 1 / Maths.log(rank + 1, 2);
                }

                nDCG += dcg / idcgs.get(testSetByUser.size() < topK ? testSetByUser.size(): topK);
                nonZeroNumUsers++;
            }
        }

        return nonZeroNumUsers > 0 ? nDCG / nonZeroNumUsers : 0.0d;
    }

}
