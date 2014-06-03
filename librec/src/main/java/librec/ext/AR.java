// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.ext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.Recommender;

/**
 * 
 * Choonho Kim and Juntae Kim, <strong>A Recommendation Algorithm Using
 * Multi-Level Association Rules</strong>, WI 2003.
 * 
 * <p>
 * Simple Association Rule Recommender: we do not consider the item categories
 * (or multi levels) used in the original paper. Besides, we consider all
 * association rules without ruling out weak ones (by setting high support and
 * confidence threshold).
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class AR extends Recommender {

	// confidence matrix of association rules
	private DenseMatrix A;

	public AR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		// Association rules recommender
		algoName = "AR";

		// cannot predict ratings, but only order preferences
		isRankingPred = true;
	}

	@Override
	protected void buildModel() {
		A = new DenseMatrix(numItems, numItems);

		// simple rule: X => Y, given that each user vector is regarded as a transaction
		for (int x = 0; x < numItems; x++) {
			// all transactions for item x
			SparseVector qx = trainMatrix.column(x);
			int total = qx.getCount();

			for (int y = 0; y < numItems; y++) {
				// compute confidence where containing item y among qx
				int count = 0;
				for (VectorEntry ve : qx) {
					int u = ve.index();
					double ruy = trainMatrix.get(u, y);
					if (ruy > 0)
						count++;
				}
				double conf = (count + 0.0) / total;
				A.set(x, y, conf);
			}
		}
	}

	@Override
	protected Map<Integer, Double> ranking(int u, Collection<Integer> candItems) {
		Map<Integer, Double> itemScores = new HashMap<>();

		SparseVector pu = trainMatrix.row(u);
		for (Integer j : candItems) {
			DenseVector qj = A.column(j);
			double rank = pu.inner(qj);

			itemScores.put(j, rank);
		}

		return itemScores;
	}

}
