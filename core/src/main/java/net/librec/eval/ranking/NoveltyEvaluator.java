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
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;

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

    private SparseMatrix trainMatrix;

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
        int[] itemCounts = calculatePurchaseCounts(testMatrix, numItems);

        double sumInformation = 0;
        for (int userID = 0; userID < numUsers; userID++) {
            List<ItemEntry<Integer, Double>> recoList = recommendedList.getItemIdxListByUserIdx(userID);
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

	private int[] calculatePurchaseCounts(SparseMatrix testMatrix, int numItems) {
		
        // Here we use the purchase counts of the train and test-Dataset !!!
        int itemCounts[] = new int[numItems];
        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            itemCounts[itemIdx] = trainMatrix.getRows(itemIdx).size() + testMatrix.getRows(itemIdx).size();
        }
		return itemCounts;
	}

	@Override
	public double evaluate(RecommenderContext context, RecommendedList recommendedList) {
		this.trainMatrix = context.getDataModel().getDataSplitter().getTrainData();
		return super.evaluate(context, recommendedList);
	}
	
	
}
