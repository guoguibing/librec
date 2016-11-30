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
 * DiversityEvaluator, average dissimilarity of all pairs of items in the
 * recommended list at a specific cutoff position. Reference: Avoiding monotony:
 * improving the diversity of recommendation lists, ReSys, 2008
 *
 */
public class DiversityEvaluator extends AbstractRecommenderEvaluator {

	public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
		double totalDiversity = 0.0;
		int numUsers = testMatrix.numRows();
		if (similarityMatrix != null) {
			for (int userID = 0; userID < numUsers; userID++) {
				List<ItemEntry<Integer, Double>> recommendArrayListByUser = recommendedList.getItemIdxListByUserIdx(userID);
				// calculate the sum of similarities for each pair of items per user
				double totalSimilarityPerUser = 0.0;
				int topK = this.topN <= recommendArrayListByUser.size() ? this.topN : recommendArrayListByUser.size();
				for (int i = 0; i < topK; i++) {
					for (int j = 0; j < topK; j++) {
						if (i == j) {
							continue;
						}
						int item1 = recommendArrayListByUser.get(i).getKey();
						int item2 = recommendArrayListByUser.get(j).getKey();
						totalSimilarityPerUser += similarityMatrix.get(item1, item2);
					}
				}
				totalDiversity += totalSimilarityPerUser * 2 / (topK * (topK - 1));
			}
		}

		return totalDiversity / numUsers;
	}

}
