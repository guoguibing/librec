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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import happy.coding.io.Strings;
import happy.coding.math.Randoms;
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
 * Zhao et al., <strong>Leveraing Social Connections to Improve Personalized Ranking for Collaborative
 * Filtering</strong>, CIKM 2014.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class SBPR extends SocialRecommender {

	private Multimap<Integer, Integer> SP;

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

		// find items rated by trusted neighbors only
		SP = HashMultimap.create();

		for (int u = 0, um = trainMatrix.numRows(); u < um; u++) {
			SparseVector Ru = trainMatrix.row(u);
			if (Ru.getCount() == 0)
				continue; // no rated items

			// SPu
			SparseVector Tu = socialMatrix.row(u);
			for (VectorEntry ve : Tu) {
				int v = ve.index(); // friend v
				if (v >= um)
					continue;

				SparseVector Rv = trainMatrix.row(v); // v's ratings
				for (VectorEntry ve2 : Rv) {
					int j = ve2.index(); // v's rated items
					if (!Ru.contains(j)) // if not rated by user u
						SP.put(u, j);
				}
			}

		}
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			errs = 0;
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				// uniformly draw (u, i, k, j)
				int u = 0, i = 0, j = 0;

				// u
				SparseVector pu = null;
				do {
					u = Randoms.uniform(trainMatrix.numRows());
					pu = trainMatrix.row(u);
				} while (pu.getCount() == 0);

				// i
				int[] is = pu.getIndex();
				i = is[Randoms.uniform(is.length)];

				double xui = predict(u, i);

				// SPu
				List<Integer> SPu = new ArrayList<>(SP.get(u));

				// j
				do {
					j = Randoms.uniform(numItems);
				} while (pu.contains(j) || SPu.contains(j));

				double xuj = predict(u, j);

				if (SPu.size() > 0) {
					// if having social neighbors
					int k = SPu.get(Randoms.uniform(SPu.size()));
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
					errs += vals;

					double cik = g(-xuik), ckj = g(-xukj);

					// update bi, bk, bj
					double bi = itemBias.get(i);
					itemBias.add(i, lRate * (cik / (1 + suk) + regB * bi));
					loss += regB * bi * bi;

					double bk = itemBias.get(k);
					itemBias.add(k, lRate * (-cik / (1 + suk) + ckj + regB * bk));
					loss += regB * bk * bk;

					double bj = itemBias.get(j);
					itemBias.add(j, lRate * (-ckj + regB * bj));
					loss += regB * bj * bj;

					// update P, Q
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qif = Q.get(i, f), qkf = Q.get(k, f);
						double qjf = Q.get(j, f);

						double delta_puf = cik * (qif - qkf) / (1 + suk) + ckj * (qkf - qjf);
						P.add(u, f, lRate * (delta_puf + regU * puf));

						Q.add(i, f, lRate * (cik * puf / (1 + suk) + regI * qif));

						double delta_qkf = cik * (-puf / (1 + suk)) + ckj * puf;
						Q.add(k, f, lRate * (delta_qkf + regI * qkf));

						Q.add(j, f, lRate * (cik * (-puf) + regI * qjf));

						loss += regU * puf * puf + regI * qif * qif;
						loss += regI * qkf * qkf + regI * qjf * qjf;
					}
				} else {
					// if no social neighbors, the same as BPR
					double xuij = xui - xuj;
					double vals = -Math.log(g(xuij));
					errs += vals;
					loss += vals;

					double cij = g(-xuij);

					// update bi, bj
					double bi = itemBias.get(i);
					itemBias.add(i, lRate * (cij + regB * bi));
					loss += regB * bi * bi;

					double bj = itemBias.get(j);
					itemBias.add(j, lRate * (-cij + regB * bj));
					loss += regB * bj * bj;

					// update P, Q
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qif = Q.get(i, f);
						double qjf = Q.get(j, f);

						P.add(u, f, lRate * (cij * (qif - qjf) + regU * puf));
						Q.add(i, f, lRate * (cij * puf + regI * qif));
						Q.add(j, f, lRate * (cij * (-puf) + regI * qjf));

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
		return Strings.toString(new Object[] { binThold, numFactors, initLRate, maxLRate, regU, regI, regB, numIters }, ",");
	}

}
