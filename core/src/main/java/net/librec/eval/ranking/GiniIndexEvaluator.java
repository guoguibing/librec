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

import java.util.Arrays;
import java.util.List;

/**
 * EntropyEvaluator
 *
 * This is a 'Diversity/Fairness'-Measure, or a horizontal equity measure. Or the degree of inequality in a distribution.
 * Individuals with equal ability/needs should get equal resources.
 * (the need of a special demographic group is not considered.)
 *
 *
 * This is the measure of fair distribution of items in recommendation lists of all the users.
 * The ideal (maximum fairness) case is when this distribution is uniform. The Gini-index of uniform
 * distribution is equal to zero and so smaller values of Gini-index are desired.
 *
 * refer to
 * Fleder, D.M., Hosanagar, K.: Recommender systems and their impact on sales diversity. In:
 * EC ’07: Proceedings of the 8th ACM conference on Electronic commerce, pp. 192–199.
 * ACM, New York, NY, USA (2007). DOI http://doi.acm.org/10.1145/1250910.1250939
 *
 * The probability of an item is assumed to be the probability to be in a recommendation result list.
 * (Estimated by count of this item in all reco list divided by the count of reco lists)
 *
 * @author Nasim Sonboli
 */
public class GiniIndexEvaluator extends AbstractRecommenderEvaluator {

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
        int numItems = conf.getInt("rec.eval.item.num");

        // First collect item counts needed for estimating probabilities of the items
        // We want to calculate the probability of each item to be in the recommendation list.
        // (This differs from the probability of the item purchased!)
        int itemCounts[] = new int[numItems];
        for (int contextIdx = 0; contextIdx < numUsers; contextIdx++) {
            List<KeyValue<Integer, Double>> recoList = recommendedList.getKeyValueListByContext(contextIdx);
            int topK = this.topN <= recoList.size() ? this.topN : recoList.size();
            for (int recoIdx = 0; recoIdx < topK; recoIdx++) {
                itemCounts[recoList.get(recoIdx).getKey()]++;
            }
        }

        double itemProb[] = new double[numItems];
        for (int index = 0; index < numItems; index++) {
            int count = itemCounts[index];
            if (count > 0) {
                double estmProbability = ((double)count)/ Arrays.stream(itemCounts).sum();
//                double estmProbability = ((double)count)/numUsers;
                itemProb[index] = estmProbability;
            }
        }

        double sumGini = 0;
        Arrays.sort(itemProb);
        for (int index = 0; index < numItems; index++) {
            sumGini +=  ((2.0*index) - numItems + 1.0) * itemProb[index];
        }
        sumGini /= (numItems-1.0);

        return sumGini;
    }
}