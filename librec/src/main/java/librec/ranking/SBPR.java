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

package librec.ranking;

import happy.coding.io.Strings;
import happy.coding.math.Randoms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.SocialRecommender;

/**
 * Social Bayesian Personalized Ranking (SBPR)
 * 
 * <p>
 * Zhao et al., <strong>Leveraging Social Connections to Improve Personalized Ranking for Collaborative
 * Filtering</strong>, CIKM 2014.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class SBPR extends SocialRecommender {

	private Map<Integer, List<Integer>> SP;

	public SBPR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		// initialization
		super.initModel();

		itemBias = new DenseVector(numItems);
		itemBias.init();

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);

		// find items rated by trusted neighbors only
		SP = new HashMap<>();

		for (int u = 0, um = trainMatrix.numRows(); u < um; u++) {
			List<Integer> uRatedItems = userItemsCache.get(u);
			if (uRatedItems.size() == 0)
				continue; // no rated items

			// SPu
			List<Integer> trustedUsers = socialMatrix.getColumns(u);
			List<Integer> items = new ArrayList<>();
			for (int v : trustedUsers) {
				if (v >= um) // friend v
					continue;

				List<Integer> vRatedItems = userItemsCache.get(v);
				for (int j : vRatedItems) {
					// v's rated items
					if (!uRatedItems.contains(j) && !items.contains(j)) // if not rated by user u and not already added to item list
						items.add(j);
				}
			}

			SP.put(u, items);

		}
	}

	@Override
	protected void postModel() throws Exception {
		SP = null; // no need for evaluation, release it. 
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				// uniformly draw (u, i, k, j)
				int u = 0, i = 0, j = 0;

				// u
				List<Integer> ratedItems = null;
				do {
					u = Randoms.uniform(trainMatrix.numRows());
					ratedItems = userItemsCache.get(u);
				} while (ratedItems.size() == 0);

				// i
				i = Randoms.random(ratedItems);

				double xui = predict(u, i);

				// SPu
				List<Integer> SPu = SP.get(u);

				// j
				do {
					j = Randoms.uniform(numItems);
				} while (ratedItems.contains(j) || SPu.contains(j));

				double xuj = predict(u, j);

				if (SPu.size() > 0) {
					// if having social neighbors
					int k = Randoms.random(SPu);
					double xuk = predict(u, k);

					SparseVector Tu = socialMatrix.row(u);
					double suk = 0;
					for (VectorEntry ve : Tu) {
						int v = ve.index();
						if (v < trainMatrix.numRows()) {
							double rvk = trainMatrix.get(v, k);
							if (rvk > 0)
								suk += 1;
						}
					}

					double xuik = (xui - xuk) / (1 + suk);
					double xukj = xuk - xuj;

					double vals = -Math.log(g(xuik)) - Math.log(g(xukj));
					loss += vals;

					double cik = g(-xuik), ckj = g(-xukj);

					// update bi, bk, bj
					double bi = itemBias.get(i);
					itemBias.add(i, lRate * (cik / (1 + suk) - regB * bi));
					loss += regB * bi * bi;

					double bk = itemBias.get(k);
					itemBias.add(k, lRate * (-cik / (1 + suk) + ckj - regB * bk));
					loss += regB * bk * bk;

					double bj = itemBias.get(j);
					itemBias.add(j, lRate * (-ckj - regB * bj));
					loss += regB * bj * bj;

					// update P, Q
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qif = Q.get(i, f), qkf = Q.get(k, f);
						double qjf = Q.get(j, f);

						double delta_puf = cik * (qif - qkf) / (1 + suk) + ckj * (qkf - qjf);
						P.add(u, f, lRate * (delta_puf - regU * puf));

						Q.add(i, f, lRate * (cik * puf / (1 + suk) - regI * qif));

						double delta_qkf = cik * (-puf / (1 + suk)) + ckj * puf;
						Q.add(k, f, lRate * (delta_qkf - regI * qkf));

						Q.add(j, f, lRate * (ckj * (-puf) - regI * qjf));

						loss += regU * puf * puf + regI * qif * qif;
						loss += regI * qkf * qkf + regI * qjf * qjf;
					}
				} else {
					// if no social neighbors, the same as BPR
					double xuij = xui - xuj;
					double vals = -Math.log(g(xuij));
					loss += vals;

					double cij = g(-xuij);

					// update bi, bj
					double bi = itemBias.get(i);
					itemBias.add(i, lRate * (cij - regB * bi));
					loss += regB * bi * bi;

					double bj = itemBias.get(j);
					itemBias.add(j, lRate * (-cij - regB * bj));
					loss += regB * bj * bj;

					// update P, Q
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qif = Q.get(i, f);
						double qjf = Q.get(j, f);

						P.add(u, f, lRate * (cij * (qif - qjf) - regU * puf));
						Q.add(i, f, lRate * (cij * puf - regI * qif));
						Q.add(j, f, lRate * (cij * (-puf) - regI * qjf));

						loss += regU * puf * puf + regI * qif * qif + regI * qjf * qjf;
					}
				}
			}

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) {
		return itemBias.get(j) + DenseMatrix.rowMult(P, u, Q, j);
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, regB, numIters });
	}

}
