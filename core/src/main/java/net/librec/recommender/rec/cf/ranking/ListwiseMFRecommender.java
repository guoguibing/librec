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
package net.librec.recommender.rec.cf.ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.MatrixFactorizationRecommender;

/**
 * 
 * Shi et al., <strong>List-wise learning to rank with matrix factorization for
 * collaborative filtering</strong>, RecSys 2010.
 * 
 * @author Bin Wu(wubin@gs.zzu.edu.cn)
 * 
 */
public class ListwiseMFRecommender extends MatrixFactorizationRecommender {
	public DenseVector userExp;
	protected double learnRate;
	private List<Set<Integer>> userItemsSet;

	protected void setup() throws LibrecException {
		super.setup();
		learnRate = conf.getFloat("rec.iteration.learnrate", 0.01f);
		userExp = new DenseVector(numUsers);
		for (MatrixEntry matrixentry : trainMatrix) {
			int userIdx = matrixentry.row();
			double realRating = matrixentry.get();

			userExp.add(userIdx, Math.exp(realRating));
		}
	}

	@Override
	protected void trainModel() throws LibrecException {
		userItemsSet = getUserItemsSet(trainMatrix);
		for (int iter = 1; iter <= numIterations; iter++) {
			for (MatrixEntry matrixentry : trainMatrix) {

				int userIdx = matrixentry.row();
				int itemIdx = matrixentry.column();
				double realRating = matrixentry.get();

				double predictRating = DenseMatrix.rowMult(userFactors, userIdx, itemFactors, itemIdx);
				double uexp = 0;

				List<Integer> items = trainMatrix.getColumns(userIdx);
				for (int item : items) {
					uexp += Math.exp(DenseMatrix.rowMult(userFactors, userIdx, itemFactors, item));
				}

				// update factors
				for (int factorIdx = 0; factorIdx < numFactors; factorIdx++) {
					double userFactorIdx = userFactors.get(userIdx, factorIdx);
					double itemFactorIdx = itemFactors.get(itemIdx, factorIdx);
					double delta_user = (Math.exp(realRating) / userExp.get(userIdx) - Math.exp(predictRating) / uexp)
							* Maths.gd(predictRating) * itemFactorIdx - regUser * userFactorIdx;
					double delta_item = (Math.exp(realRating) / userExp.get(userIdx) - Math.exp(predictRating) / uexp)
							* Maths.gd(predictRating) * userFactorIdx - regItem * itemFactorIdx;

					userFactors.add(userIdx, factorIdx, learnRate * delta_user);
					itemFactors.add(itemIdx, factorIdx, learnRate * delta_item);
				}

			}
		} // end of training
	}


	private List<Set<Integer>> getUserItemsSet(SparseMatrix sparseMatrix) {
		List<Set<Integer>> userItemsSet = new ArrayList<>();
		for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
			userItemsSet.add(new HashSet<Integer>(sparseMatrix.getColumns(userIdx)));
		}
		return userItemsSet;
	}
}