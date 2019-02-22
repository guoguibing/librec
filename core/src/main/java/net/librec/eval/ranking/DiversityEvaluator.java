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
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.util.List;

/**
 * DiversityEvaluator, average dissimilarity of all pairs of items in the
 * recommended list at a specific cutoff position. Reference: Avoiding monotony:
 * improving the diversity of recommendation lists, ReSys, 2008
 *
 * @author Keqiang Wang
 */
public class DiversityEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList
     *            the given ground truth list
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        double totalDiversity = 0.0;
        int numContext = groundTruthList.size();
        int nonZeroContext = 0;

        if (similarities.containsKey("item")) {
            SymmMatrix itemSimilarity = similarities.get("item").getSimilarityMatrix();
            for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
                List<KeyValue<Integer, Double>> recommendArrayListByContext= recommendedList.getKeyValueListByContext(contextIdx);
                if (recommendArrayListByContext.size() > 1) {
                    // calculate the sum of dissimilarities for each pair of items per user
                    double totalDisSimilarityPerContext = 0.0;
                    int topK = this.topN <= recommendArrayListByContext.size() ? this.topN : recommendArrayListByContext.size();
                    for (int indexOut = 0; indexOut < topK; ++indexOut) {
                        for (int indexIn = 0; indexIn < topK; ++indexIn) {
                            if (indexOut == indexIn) {
                                continue;
                            }
                            int keyOut = recommendArrayListByContext.get(indexOut).getKey();
                            int keyIn = recommendArrayListByContext.get(indexIn).getKey();
                            totalDisSimilarityPerContext += 1.0 - itemSimilarity.get(keyOut, keyIn);
                        }
                    }
                    totalDiversity += totalDisSimilarityPerContext / (topK * (topK - 1));
                    nonZeroContext++;
                }
            }
        }

        return nonZeroContext > 0 ? totalDiversity / nonZeroContext : 0.0d;
    }
}
