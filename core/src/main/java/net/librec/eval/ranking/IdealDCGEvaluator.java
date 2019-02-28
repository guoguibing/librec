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
import net.librec.recommender.item.RecommendedList;

/**
 * IdealDCGEvaluator
 *<a href=https://en.wikipedia.org/wiki/Discounted_cumulative_gain>wikipedia, ideal dcg</a>
 * @author WangYuFeng and Keqiang Wang
 */
public class IdealDCGEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList the given ground truth list
     * @param recommendedList the list of recommended items
     * @return evaluate result
     */
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        double iDCG = 0.0;

        int numContext = groundTruthList.size();
        int nonZeroContext = 0;
        for (int contextIdx = 0; contextIdx < numContext; ++contextIdx) {
            double idcg = 0.0;

            int sizeByContext = groundTruthList.getKeyValueListByContext(contextIdx).size();
            if (sizeByContext > 0) {
                // calculate the IDCG
                for (int i = 0; i < sizeByContext; i++) {
                    idcg += 1 / Maths.log(i + 2.0, 2);
                }
                iDCG += idcg;
                ++nonZeroContext;
            }
        }

        return nonZeroContext > 0 ? iDCG / nonZeroContext : 0.0d;
    }

}
