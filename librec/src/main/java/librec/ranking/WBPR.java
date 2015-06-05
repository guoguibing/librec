// Copyright (C) 2014-2015 Guibing Guo
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

package librec.ranking;

import happy.coding.io.Lists;
import happy.coding.io.Strings;
import happy.coding.math.Randoms;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 
 * Gantner et al., <strong>Bayesian Personalized Ranking for Non-Uniformly Sampled Items</strong>, JMLR, 2012.
 * 
 * @author guoguibing
 * 
 */
public class WBPR extends IterativeRecommender {

	private List<Entry<Integer, Double>> sortedItemPops;
	private LoadingCache<Integer, List<Entry<Integer, Double>>> cacheItemProbs;

	public WBPR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		itemBias = new DenseVector(numItems);
		itemBias.init(smallValue);

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);

		// pre-compute and sort by item's popularity
		sortedItemPops = new ArrayList<>();
		for (int i = 0; i < numItems; i++) {
			sortedItemPops.add(new AbstractMap.SimpleEntry<Integer, Double>(i, trainMatrix.columnSize(i) + 0.0));
		}
		Lists.sortList(sortedItemPops, true);

		// cache each user's candidate items with probabilities
		cacheItemProbs = CacheBuilder.from(cacheSpec).build(new CacheLoader<Integer, List<Entry<Integer, Double>>>() {

			@Override
			public List<Entry<Integer, Double>> load(Integer u) throws Exception {
				List<Entry<Integer, Double>> itemProbs = new ArrayList<>();

				List<Integer> ratedItems = userItemsCache.get(u);

				// filter candidate items
				double sum = 0;
				for (Entry<Integer, Double> itemPop : sortedItemPops) {
					Integer item = itemPop.getKey();
					double popularity = itemPop.getValue();

					if (!ratedItems.contains(item) && popularity > 0) {
						// make a clone to prevent bugs from normalization
						itemProbs.add(new AbstractMap.SimpleEntry<Integer, Double>(itemPop));
						sum += popularity;
					}
				}

				// normalization
				for (Entry<Integer, Double> itemProb : itemProbs) {
					itemProb.setValue(itemProb.getValue() / sum);
				}

				return itemProbs;
			}
		});
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				// randomly draw (u, i, j)
				int u = 0, i = 0, j = 0;
				List<Integer> ratedItems = null;
				List<Entry<Integer, Double>> itemProbs = null;

				while (true) {
					u = Randoms.uniform(numUsers);
					ratedItems = userItemsCache.get(u);

					if (ratedItems.size() == 0)
						continue;

					i = Randoms.random(ratedItems);

					// sample j by popularity (probability)
					itemProbs = cacheItemProbs.get(u);

					double rand = Randoms.random();
					double sum = 0;
					for (Entry<Integer, Double> itemProb : itemProbs) {
						sum += itemProb.getValue();
						if (sum >= rand) {
							j = itemProb.getKey();
							break;
						}
					}

					break;
				}

				// update parameters
				double xui = predict(u, i);
				double xuj = predict(u, j);
				double xuij = xui - xuj;

				double vals = -Math.log(g(xuij));
				loss += vals;

				double cmg = g(-xuij);

				// update bias
				double bi = itemBias.get(i), bj = itemBias.get(j);
				itemBias.add(i, lRate * (cmg - regB * bi));
				itemBias.add(j, lRate * (-cmg - regB * bj));
				loss += regB * (bi * bi + bj * bj);

				// update user/item vectors
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qif = Q.get(i, f);
					double qjf = Q.get(j, f);

					P.add(u, f, lRate * (cmg * (qif - qjf) - regU * puf));
					Q.add(i, f, lRate * (cmg * puf - regI * qif));
					Q.add(j, f, lRate * (cmg * (-puf) - regI * qjf));

					loss += regU * puf * puf + regI * qif * qif + regI * qjf * qjf;
				}
			}

			if (isConverged(iter))
				break;

		}
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, regB, numIters });
	}

	@Override
	protected double predict(int u, int j) throws Exception {
		return itemBias.get(j) + DenseMatrix.rowMult(P, u, Q, j);
	}
}
