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
//import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the ratio of unique items recommended to users to total unique items in dataset(test & train)
 *  *
 * @author Nasim Sonboli, Florencia Cabral
 */

public class ItemCoverageEvaluator extends AbstractRecommenderEvaluator {

//    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        if (groundTruthList == null) {
            return 0.0;
        }
        SequentialAccessSparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();

        Set<Integer> uniqueItemsRecList = new HashSet<Integer>();
        Set<Integer> uniqueItemsInTestTrainMatrix = new HashSet<Integer>();

        int numUsers = groundTruthList.size();
        for (int contextIdx = 0; contextIdx < numUsers; contextIdx++) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx); //items from test

            // TODO: DEBUG (note if I comment the trainSetByContext, the result doesn't change!)
            int[] trainSetByContext = trainMatrix.row(contextIdx).getIndices(); // items from train

            if (testSetByContext.size() > 0) {
                //test
                for (int itemID: testSetByContext) {
                    if (!uniqueItemsInTestTrainMatrix.contains(itemID)) {
                        uniqueItemsInTestTrainMatrix.add(itemID);
                    }
                }
                //train
                for (int itemID: trainSetByContext) {
                    if (!uniqueItemsInTestTrainMatrix.contains(itemID)) {
                        uniqueItemsInTestTrainMatrix.add(itemID);
                    }
                }

                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int itemIdx = recommendListByContext.get(indexOfKey).getKey();
                    if (!uniqueItemsRecList.contains(itemIdx)) {
                        uniqueItemsRecList.add(itemIdx);
                    }
                }
            }
        } // end of iteration for the users

        // return ratio of unique items in recommended list to unique items in testMatrix+trainMatrix
        return uniqueItemsInTestTrainMatrix.size() > 0 ? uniqueItemsRecList.size() * 1.0 / uniqueItemsInTestTrainMatrix.size() : 0.0d;
    }
}