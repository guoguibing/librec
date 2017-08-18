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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AUCEvaluator
 *
 * @author WangYuFeng and Keqiang Wang
 */
public class AUCEvaluator extends AbstractRecommenderEvaluator {

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
        double auc = 0.0d;

        int numUsers = testMatrix.numRows();
        int nonZeroNumUsers = 0;
        int[] numDroppedItemsArray = getConf().getInts("rec.eval.auc.dropped.num");

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            Set<Integer> testSetByUser = testMatrix.getColumnsSet(userIdx);
            if (testSetByUser.size() > 0) {
                nonZeroNumUsers++;
                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userIdx);
                int numDroppedItems = numDroppedItemsArray[userIdx] - recommendListByUser.size();
                Set<Integer> recommendSetByUser = new HashSet<>();
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; ++indexOfItem) {
                    recommendSetByUser.add(recommendListByUser.get(indexOfItem).getKey());
                }

                int numRelevantItems = 0, numMissItems = 0;
                for (Integer testItemIdx : testSetByUser) {
                    if (recommendSetByUser.contains(testItemIdx)) {
                        numRelevantItems++;
                    } else {
                        numMissItems++;
                    }
                }

                int numEvaluatingItems = recommendSetByUser.size() + numDroppedItems;
                int numEvaluatingPairs = (numEvaluatingItems - numRelevantItems) * numRelevantItems;

                if (numEvaluatingPairs < 0) {
                    throw new IndexOutOfBoundsException("numEvaluatingPairs cannot be less than 0.");
                }

                if (numEvaluatingPairs == 0) {
                    auc += 0.5;
                    continue;
                }

                int numCorrectPairs = 0;
                int hits = 0;
                for (Integer itemIdx : recommendSetByUser) {
                    if (!testSetByUser.contains(itemIdx)) {
                        numCorrectPairs += hits;
                    } else {
                        hits ++;
                    }
                }

                numCorrectPairs += hits * (numDroppedItems - numMissItems);

                auc += (numCorrectPairs + 0.0) / numEvaluatingPairs;
            }
        }

        return nonZeroNumUsers > 0 ? auc / nonZeroNumUsers : 0.0d;
    }
}