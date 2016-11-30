/**
 * Copyright (C) 2016 LibRec
 * 
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.eval.ranking;

import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.ItemEntry;
import net.librec.recommender.item.RecommendedList;
import net.librec.util.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * AUCEvaluator
 * 
 */
public class AUCEvaluator extends AbstractRecommenderEvaluator {

	// the number of relevant items that were not ranked (considered to be ranked below all ranked_items)

	public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
		int numDroppedItems = getConf().getInt("rec.eval.auc.dropped.num");
		if (testMatrix.size() == 0) {
			return 0.0;
		}

		double auc = 0.0;

		int numUsers = testMatrix.numRows();
		for (int userID = 0; userID < numUsers; userID++) {
			List<Integer> testListByUser = testMatrix.getColumns(userID);
			List<ItemEntry<Integer, Double>> recommendArrayListByUser = recommendedList.getItemIdxListByUserIdx(userID);

			List<Integer> recommendListByUser = arrayListToList(recommendArrayListByUser);

			int numRelevantItems = Lists.overlapSize(testListByUser, recommendListByUser);
			int numEvaluatingItems = recommendListByUser.size() + numDroppedItems;
			int numEvaluatingPairs = (numEvaluatingItems - numRelevantItems) * numRelevantItems;

			if (numEvaluatingPairs < 0) {
				System.out.println("numEvaluatingPairs cannot be less than 0.");
				continue;
			}

			if (numEvaluatingPairs == 0) {
				auc += 0.5;
				continue;
			}

			int numCorrectPairs = 0;
			int hits = 0;
			for (Integer itemID: recommendListByUser) {
				if (!testListByUser.contains(itemID)) {
					numCorrectPairs += hits;
				} else {
					hits += 1;
				}
			}

			int numMissItems = Lists.exceptSize(testListByUser, recommendListByUser);
			numCorrectPairs += hits * (numDroppedItems - numMissItems);

			auc += (numCorrectPairs + 0.0) / numEvaluatingPairs;
		}

		return auc / numUsers;
	}

	private List<Integer> arrayListToList(List<ItemEntry<Integer, Double>> recommendArrayListByUser) {

		List<Integer> recommendListByUser = new ArrayList<>();

		for (ItemEntry<Integer, Double> item: recommendArrayListByUser) {
			recommendListByUser.add(item.getKey());
		}

		return recommendListByUser;
	}

}
