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

/**
 * NoveltyEvaluator
 *
 * Often also called 'Mean Self-Information' or Surprisal
 *
 * Look at Section '4.2.5 Novelty' of article:
 *
 * Javari, Amin, and Mahdi Jalili. "A probabilistic model to resolve diversity–accuracy challenge of recommendation systems." Knowledge and Information Systems 44.3 (2015): 609-627.
 *
 *
 * Calculates Self-Information of each recommender result list.
 * And then calculates the average of this of all result lists in test set.
 *
 * But please take also attention to the assumed probability space:
 *
 * The probability of an item is assumed to be the purchase probability.
 * (Estimated by items purchased divided by all items purchased.)
 * Surely there is also independence assumed between items.
 *
 * This assumption about the probability space is different from the EntropyEvaluator
 *
 *
 * @author Daniel Velten, Karlsruhe, Germany, SunYatong
 */
public class NoveltyEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    @Override
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        int numUsers = groundTruthList.size();

        // First collect item counts needed for estimating probabilities of the items
        int[] itemCounts = conf.getInts("rec.eval.item.purchase.num");

        double sumInformation = 0;
        for (int contextIdx = 0; contextIdx < numUsers; contextIdx++) {
            List<KeyValue<Integer, Double>> recoList = recommendedList.getKeyValueListByContext(contextIdx);
            int topK = this.topN <= recoList.size() ? this.topN : recoList.size();
            for (int recoIdx = 0; recoIdx < topK; recoIdx++) {
                int itemIdx = recoList.get(recoIdx).getKey();
                int count = itemCounts[itemIdx];
                if (count>0) {
                    double estmProbability = ((double)count)/numUsers;
                    double selfInformation = -Math.log(estmProbability);
                    sumInformation += selfInformation;
                }
            }
        }
        return sumInformation/(numUsers * Math.log(2));
    }


}