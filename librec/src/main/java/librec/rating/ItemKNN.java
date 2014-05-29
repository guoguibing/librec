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

package librec.rating;

import happy.coding.io.KeyValPair;
import happy.coding.io.Lists;
import happy.coding.math.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.SymmMatrix;
import librec.intf.Recommender;

/**
 * <h3>Item-based Nearest Neighbors</h3>
 * 
 * <p>
 * It supports both recommendation tasks: (1) rating prediction; and (2) item
 * ranking (by configuring {@code isRankingPred=on} in the librec.conf). For
 * item ranking, the returned score is the summation of the similarities of
 * nearest neighbors (see Section 4.3.2 of Rendle et al., BPR: Bayesian
 * Personalized Ranking from Implicit Feedback, UAI 2009).
 * </p>
 * 
 * <p>
 * When the number of items is extremely large which makes it memory intensive to
 * store/precompute all item-item correlations, a trick presented by (Jahrer and
 * Toscher, Collaborative Filtering Ensemble, JMLR 2012) can be applied.
 * Specifically, we can use a basic SVD model to obtain item-feature vectors,
 * and then item-item correlations can be computed by Eqs (13, 15).
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class ItemKNN extends Recommender {

	// user: nearest neighborhood
	private SymmMatrix itemCorrs;
	private DenseVector itemMeans;
	private int knn;

	public ItemKNN(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "ItemKNN";
		knn = cf.getInt("num.neighbors");
	}

	@Override
	protected void initModel() {
		itemCorrs = buildCorrs(false);
		itemMeans = new DenseVector(numItems);
		for (int i = 0; i < numItems; i++) {
			SparseVector vs = trainMatrix.column(i);
			itemMeans.set(i, vs.getCount() > 0 ? vs.mean() : globalMean);
		}
	}

	@Override
	protected double predict(int u, int j) {

		// find a number of similar items
		Map<Integer, Double> nns = new HashMap<>();

		SparseVector dv = itemCorrs.row(j);
		for (int i : dv.getIndex()) {
			double sim = dv.get(i);
			double rate = trainMatrix.get(u, i);

			if (isRankingPred && rate > 0)
				nns.put(i, sim);
			else if (sim > 0 && rate > 0)
				nns.put(i, sim);
		}

		// topN similar items
		if (knn > 0 && knn < nns.size()) {
			List<KeyValPair<Integer>> sorted = Lists.sortMap(nns, true);
			List<KeyValPair<Integer>> subset = sorted.subList(0, knn);
			nns.clear();
			for (KeyValPair<Integer> kv : subset)
				nns.put(kv.getKey(), kv.getVal().doubleValue());
		}

		if (nns.size() == 0)
			return isRankingPred ? 0 : globalMean;

		if (isRankingPred) {
			// for recommendation task: item ranking

			return Stats.sum(nns.values());
		} else {
			// for recommendation task: rating prediction

			double sum = 0, ws = 0;
			for (Entry<Integer, Double> en : nns.entrySet()) {
				int i = en.getKey();
				double sim = en.getValue();
				double rate = trainMatrix.get(u, i);

				sum += sim * (rate - itemMeans.get(i));
				ws += Math.abs(sim);
			}

			return ws > 0 ? itemMeans.get(j) + sum / ws : globalMean;
		}
	}

	@Override
	public String toString() {
		return super.toString() + "," + knn + "," + cf.getString("similarity") + "," + cf.getInt("num.shrinkage");
	}
}
