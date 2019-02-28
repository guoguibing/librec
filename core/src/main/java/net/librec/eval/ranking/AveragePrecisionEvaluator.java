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
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.List;
import java.util.Set;

/**
 * AveragePrecisionEvaluator, calculate the MAP@n, if you want get MAP, please set top-n = number of items
 * <p>
 * <a href=https://en.wikipedia.org/wiki/Information_retrieval>wikipedia, MAP</a>
 * <a href=https://www.kaggle.com/wiki/MeanAveragePrecision>kaggle, MAP@n</a>
 *
 * @author Keqiang Wang
 */
public class AveragePrecisionEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        double totalPrecision = 0.0;
        int numContext = groundTruthList.size();
        int nonZeroContext = 0;

        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            Set<Integer> testSetByContext = groundTruthList.getKeySetByContext(contextIdx);
            if (testSetByContext.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByContext = recommendedList.getKeyValueListByContext(contextIdx);

                int numHits = 0;
                int topK = this.topN <= recommendListByContext.size() ? this.topN : recommendListByContext.size();
                double tempPrecision = 0.0d;
                for (int indexOfKey = 0; indexOfKey < topK; ++indexOfKey) {
                    int key = recommendListByContext.get(indexOfKey).getKey();
                    if (testSetByContext.contains(key)) {
                        numHits++;
                        tempPrecision += 1.0 * numHits / (indexOfKey + 1);
                    }
                }
                if(topK != 0) {
                    totalPrecision += tempPrecision / (testSetByContext.size() < topK ? testSetByContext.size() : topK); //$$ap@n = \sum_{k=1}^n P(k) / min(m, n)$$ advised by WuBin
                    nonZeroContext++;
                }
            }
        }
        return nonZeroContext > 0 ? totalPrecision / nonZeroContext : 0.0d;
    }
}
