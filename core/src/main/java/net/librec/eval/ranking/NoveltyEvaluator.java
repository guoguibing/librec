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
 * NoveltyEvaluator
 * 
 * 'Mean Self-Information'
 * 
 * 
 * Zhou, Tao, et al. "Solving the apparent diversity-accuracy dilemma of recommender systems." Proceedings of the National Academy of Sciences 107.10 (2010): 4511-4515.
 * 
 * In this research article measure is described in '(D2) Surprisal/noevelty'
 * 
 * This measure is also called 'Mean Self-Information'
 * 
 * Estimated Entropy/Information per recommender result list in Bytes. 
 * 
 * (recommender result list is shortened to topN)  
 *
 * @author Daniel Velten, Karlsruhe, Germany
 */
public class NoveltyEvaluator extends AbstractRecommenderEvaluator {

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
        int nonZeroNumUsers = 0;

        // First collect item counts needed for estimating probabilities of the items
        int itemCounts[] = new int[numItems];
        for (int userID = 0; userID < numUsers; userID++) {
            List<ItemEntry<Integer, Double>> recoList = recommendedList.getItemIdxListByUserIdx(userID);
            if (recoList.size() > 1) {
                int topK = this.topN <= recoList.size() ? this.topN : recoList.size();
                for (int recoIdx = 0; recoIdx < topK; recoIdx++) {
                	itemCounts[recoList.get(recoIdx).getKey()]++;
                }
                nonZeroNumUsers++;
            }
        }
        double sumInformation = 0;
		for (int i = 0; i < itemCounts.length; i++) {
        	int count = itemCounts[i];
        	if (count>0){
				double estmProbability = ((double)count)/numUsers;
	        	double information = -Math.log(estmProbability);
				sumInformation += count * information;
        	}
		}
        double informationInBytesPerUser = sumInformation/(nonZeroNumUsers * Math.log(2));


        return informationInBytesPerUser;
    }
}
