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
import research.model.eval.AbstractRecommenderEvaluator;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * ReciprocalRankEvaluator, calculate the MRR@n, if you want get MRR, please set top-n = number of items
 * <p>
 * <a href=https://en.wikipedia.org/wiki/Mean_reciprocal_rank>wikipedia, MRR</a>
 *
 * @author WangYuFeng and Keqiang Wang
 */
public class ReciprocalRankEvaluator extends AbstractRecommenderEvaluator<IntCollection> {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        double reciprocalRank = 0.0;

        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx);
            if (testSetByContext.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);

                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int key = recommendListByContext.get(indexOfKey).getKey();
                    if (testSetByContext.contains(key)) {
                        reciprocalRank += 1.0d / (indexOfKey + 1.0d);
                        break;
                    }
                }
                nonZeroContext++;
            }
        }
        return nonZeroContext > 0 ? reciprocalRank / nonZeroContext : 0.0d;
    }


    public double evaluate(IntCollection itemIdSetInTest, List<KeyValue> recommendedList){
        double reciprocalRank = 0.0;
        int topK = this.topN <= recommendedList.size() ? this.topN : recommendedList.size();
        for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
            int key = (int)recommendedList.get(indexOfKey).getKey();
            if (itemIdSetInTest.contains(key)) {
                reciprocalRank += 1.0d / (indexOfKey + 1.0d);
            }
        }
        return reciprocalRank;
    }
}
