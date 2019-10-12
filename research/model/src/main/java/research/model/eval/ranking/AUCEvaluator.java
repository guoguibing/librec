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
package research.model.eval.ranking;


import it.unimi.dsi.fastutil.ints.IntCollection;
import research.model.common.LibrecRuntimeException;

import research.model.eval.AbstractRecommenderEvaluator;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AUCEvaluator@n
 * <a href=https://en.wikipedia.org/wiki/Receiver_operating_characteristic#Area_under_the_curve>wikipedia, AUC</a>
 *
 * @author Keqiang Wang
 */
public class AUCEvaluator extends AbstractRecommenderEvaluator<IntCollection> {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList the given  ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    @Override
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


    /**
     * Evaluate on the test set with the itemIdSetInTest.
     *
     * @param itemIdSetInTest
     * @param recommendedList
     * @return
     */
    @Override
    public double evaluate(IntCollection itemIdSetInTest, List<KeyValue> recommendedList){
        int recItemSize = recommendedList.size();
        int topK = this.topN <= recommendedList.size() ? this.topN : recommendedList.size();
        Set<Integer> recommendSetByContext = new HashSet<>();
        int numRelevantKeys = 0, numMissKeys = 0;
        for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
            Integer key = (Integer) recommendedList.get(indexOfKey).getKey();
            recommendSetByContext.add(key);
            if (itemIdSetInTest.contains(key)) {
                numRelevantKeys++;
            } else {
                numMissKeys++;
            }
        }
        int evaluateSum = (itemIdSetInTest.size() + recItemSize - this.topN - numRelevantKeys) * numRelevantKeys;
        if (evaluateSum == 0) {
            return 0.5F;
        }
        int hits = 0, hitSum = 0;
        for (Integer itemIndex : itemIdSetInTest) {
            if (!itemIdSetInTest.contains(itemIndex)) {
                hitSum += hits;
            } else {
                hits++;
            }
        }
        hitSum += hits * (recItemSize - numMissKeys);
        return (hitSum + 0.0) / evaluateSum;
    }

}
