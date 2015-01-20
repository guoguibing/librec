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

import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.Recommender;

/**
 * 
 * Choonho Kim and Juntae Kim, <strong>A Recommendation Algorithm Using Multi-Level Association Rules</strong>, WI 2003.
 * 
 * <p>
 * Simple Association Rule Recommender: we do not consider the item categories (or multi levels) used in the original
 * paper. Besides, we consider all association rules without ruling out weak ones (by setting high support and
 * confidence threshold).
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class AR extends Recommender {

	// confidence matrix of association rules
	private Table<Integer, Integer, Double> A;

	public AR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		// cannot predict ratings, but only order preferences
		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		A = HashBasedTable.create(numItems, numItems);

		userCache = trainMatrix.rowCache(cacheSpec);
	}

	@Override
	protected void buildModel() throws Exception {

		// simple rule: X => Y, given that each user vector is regarded as a
		// transaction
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

				if (count > 0) {
					double conf = (count + 0.0) / total;
					A.put(x, y, conf);
				}
			}
		}
	}

	@Override
	protected double ranking(int u, int j) throws Exception {
		SparseVector pu = userCache.get(u);

		double rank = 0;
		for (Entry<Integer, Double> en : A.column(j).entrySet()) {
			int i = en.getKey();
			double support = en.getValue();

			rank += pu.get(i) * support;
		}

		return rank;
	}

}
