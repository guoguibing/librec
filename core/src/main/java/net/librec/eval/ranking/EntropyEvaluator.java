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

import java.util.List;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

/**
 * EntropyEvaluator
 * 
 * This is a 'Diversity'-Measure, but not necessarily a 'Novelty' or 'Surprisal'-Measure.
 * 
 * Look at Section '4.2.3 Item space coverage' of article:
 * 
 * Javari, Amin, and Mahdi Jalili. "A probabilistic model to resolve diversity–accuracy challenge of recommendation systems." Knowledge and Information Systems 44.3 (2015): 609-627.
 * 
 * 
 * Calculates the Entropy within all recommender result list.
 * 
 * But please take also attention to the assumed probability space:
 * 
 * The probability of an item is assumed to be the probability to be in an recommendation result list.
 * (Estimated by count of this item in all reco list divided by the count of reco lists)
 * 
 * This assumption about the probability space is different from the NoveltyEvaluator
 * 
 *
 * @author Daniel Velten, Karlsruhe, Germany, SunYatong
 */
public class EntropyEvaluator extends AbstractRecommenderEvaluator {

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param testMatrix
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */
    @Override
	public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {

        int numUsers = testMatrix.numRows();
        int numItems = testMatrix.numColumns();

        // First collect item counts needed for estimating probabilities of the items
        // We want to calculate the probability of each item to be in the recommendation list.
        // (This differs from the probability of the item purchased!)
        int itemCounts[] = new int[numItems];
        for (int userID = 0; userID < numUsers; userID++) {
            List<ItemEntry<Integer, Double>> recoList = recommendedList.getItemIdxListByUserIdx(userID);
            int topK = this.topN <= recoList.size() ? this.topN : recoList.size();
            for (int recoIdx = 0; recoIdx < topK; recoIdx++) {
            	itemCounts[recoList.get(recoIdx).getKey()]++;
            }
        }
        double sumEntropy = 0;
        for (int count: itemCounts) {
            if (count>0){
                double estmProbability = ((double)count)/numUsers;
                sumEntropy += estmProbability * (-Math.log(estmProbability));
            }
        }
		// You can scale the unit of entropy to the well known 'Bit'-Unit by dividing by log(2)
		// (Above we have used the natural logarithm instead of the logarithm with base 2)
        return sumEntropy/Math.log(2);
    }
}
