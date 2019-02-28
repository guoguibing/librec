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

import net.librec.common.LibrecException;
import net.librec.common.LibrecRuntimeException;
import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AUCEvaluator@n
 * <a href=https://en.wikipedia.org/wiki/Receiver_operating_characteristic#Area_under_the_curve>wikipedia, AUC</a>
 *
 * @author Keqiang Wang
 */
public class AUCEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList the given  ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        double auc = 0.0d;

        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        int[] numDroppedArray = getConf().getInts("rec.eval.auc.dropped.num");

        if (numDroppedArray == null || numDroppedArray.length != numContext){
            throw new LibrecRuntimeException("please set rec.eval.auc.dropped.num arrays, length of numDroppedArray must be cardinality of groundTruthList.");
        }

        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            Set<Integer> groudTruthSetByContext = groundTruthList.getKeySetByContext(contextIdx);
            if (groudTruthSetByContext.size() > 0) {
                nonZeroContext++;
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                int numDroppedItems = numDroppedArray[contextIdx] - topK;
                Set<Integer> recommendSetByContext = new HashSet<>();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    recommendSetByContext.add(recommendListByContext.get(indexOfKey).getKey());
                }

                int numRelevantKeys = 0, numMissKeys = 0;
                for (Integer testKey : recommendSetByContext) {
                    if (groudTruthSetByContext.contains(testKey)) {
                        numRelevantKeys++;
                    } else {
                        numMissKeys++;
                    }
                }

                int numEvaluatingItems =  numDroppedItems + topK;
                int numEvaluatingPairs = (numEvaluatingItems - numRelevantKeys) * numRelevantKeys;

                if (numEvaluatingPairs < 0) {
                    throw new IndexOutOfBoundsException("numEvaluatingPairs cannot be less than 0.");
                }

                if (numEvaluatingPairs == 0) {
                    auc += 0.5;
                    continue;
                }

                int numCorrectPairs = 0;
                int hits = 0;
                for (Integer itemIdx : groudTruthSetByContext) {
                    if (!recommendSetByContext.contains(itemIdx)) {
                        numCorrectPairs += hits;
                    } else {
                        hits++;
                    }
                }

                numCorrectPairs += hits * (numDroppedItems - numMissKeys);

                auc += (numCorrectPairs + 0.0) / numEvaluatingPairs;
            }
        }

        return nonZeroContext > 0 ? auc / nonZeroContext : 0.0d;
    }
}
