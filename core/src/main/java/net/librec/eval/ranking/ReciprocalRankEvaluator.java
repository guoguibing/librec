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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ReciprocalRankEvaluator
 * 
 * @author KEVIN
 */
public class ReciprocalRankEvaluator extends AbstractRecommenderEvaluator {

	public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
		if (testMatrix.size() == 0) {
			return 0.0;
		}

		double reciprocalRank = 0.0;

		int numUsers = testMatrix.numRows();
		for (int userID = 0; userID < numUsers; userID++) {
			Set<Integer> testListByUser = testMatrix.getColumnsSet(userID);
			List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);

			int numItemsInRecommendedList = recommendListByUser.size();
			for (int i = 0; i < numItemsInRecommendedList; i++) {
				int itemID = recommendListByUser.get(i).getKey();
				if (testListByUser.contains(itemID)) {
					reciprocalRank += 1 / (i + 1.0);
				}
			}
		}

		return reciprocalRank / numUsers;
	}

}
