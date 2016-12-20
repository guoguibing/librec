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
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * PrecisionEvaluator, calculate precision@n
 *
 * @author KEVIN
 */
public class PrecisionEvaluator extends AbstractRecommenderEvaluator {

    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
        if (testMatrix.size() == 0) {
            return 0.0;
        }

        double totalPrecision = 0.0;
        int numUsers = testMatrix.numRows();
        for (int userID = 0; userID < numUsers; userID++) {
            Set<Integer> testListByUser = testMatrix.getColumnsSet(userID);
            List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);

            int numHits = 0;
            int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
            for (int i = 0; i < topK; i++) {
                int itemID = recommendListByUser.get(i).getKey();
                if (testListByUser.contains(itemID)) {
                    numHits++;
                }
            }
            if (topK > 0) {
                totalPrecision += numHits / (topK + 0.0);
            }
        }

        return totalPrecision / numUsers;
    }

}
