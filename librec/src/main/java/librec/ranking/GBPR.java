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

import java.util.ArrayList;
import java.util.List;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.intf.SocialRecommender;
import librec.util.Randoms;
import librec.util.Strings;

/**
 * Pan and Chen, <strong>GBPR: Group Preference Based Bayesian Personalized Ranking for One-Class Collaborative
 * Filtering</strong>, IJCAI 2013.
 * 
 * @author guoguibing
 * 
 */
public class GBPR extends SocialRecommender {

	private float rho;
	private int gLen;

	public GBPR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
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

		rho = algoOptions.getFloat("-rho");
		gLen = algoOptions.getInt("-gSize");

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
		itemUsersCache = trainMatrix.columnRowsCache(cacheSpec);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				// uniformly draw (u, i, g, j)
				int u = 0, i = 0, j = 0;

				// u
				List<Integer> ratedItems = null; // row u
				do {
					u = Randoms.uniform(trainMatrix.numRows());
					ratedItems = userItemsCache.get(u);
				} while (ratedItems.size() == 0);

				// i
				i = Randoms.random(ratedItems);

				// g
				List<Integer> ws = itemUsersCache.get(i); // column i
				List<Integer> g = new ArrayList<>();
				if (ws.size() <= gLen) {
					g.addAll(ws);
				} else {

					g.add(u); // u in G
					while (g.size() < gLen) {
						Integer w = Randoms.random(ws);
						if (!g.contains(w))
							g.add(w);
					}

				}

				double pgui = predict(u, i, g);

				// j
				do {
					j = Randoms.uniform(numItems);
				} while (ratedItems.contains(j));

				double puj = predict(u, j);

				double pgij = pgui - puj;
				double vals = -Math.log(g(pgij));
				loss += vals;

				double cmg = g(-pgij);

				// update bi, bj
				double bi = itemBias.get(i);
				itemBias.add(i, lRate * (cmg - regB * bi));
				loss += regB * bi * bi;

				double bj = itemBias.get(j);
				itemBias.add(j, lRate * (-cmg - regB * bj));
				loss += regB * bj * bj;

				// update Pw
				double n = 1.0 / g.size();
				double sum_w[] = new double[numFactors];
				for (int w : g) {
					double delta = w == u ? 1 : 0;
					for (int f = 0; f < numFactors; f++) {
						double pwf = P.get(w, f);
						double qif = Q.get(i, f);
						double qjf = Q.get(j, f);

						double delta_pwf = rho * n * qif + (1 - rho) * delta * qif - delta * qjf;
						PS.add(w, f, lRate * (cmg * delta_pwf - regU * pwf));

						loss += regU * pwf * pwf;

						sum_w[f] += pwf;
					}
				}

				// update Qi, Qj
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qif = Q.get(i, f);
					double qjf = Q.get(j, f);

					double delta_qif = rho * n * sum_w[f] + (1 - rho) * puf;
					QS.add(i, f, lRate * (cmg * delta_qif - regI * qif));
					loss += regI * qif * qif;

					double delta_qjf = -puf;
					QS.add(j, f, lRate * (cmg * delta_qjf - regI * qjf));
					loss += regI * qjf * qjf;
				}
			}

			P = P.add(PS);
			Q = Q.add(QS);

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) {
		return itemBias.get(j) + DenseMatrix.rowMult(P, u, Q, j);
	}

	protected double predict(int u, int j, List<Integer> g) {
		double ruj = predict(u, j);

		double sum = 0;
		for (int w : g)
			sum += DenseMatrix.rowMult(P, w, Q, j);

		double rgj = sum / g.size() + itemBias.get(j);

		return rho * rgj + (1 - rho) * ruj;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, rho, gLen, numFactors, initLRate, maxLRate, regU, regI, regB,
				numIters });
	}

}
