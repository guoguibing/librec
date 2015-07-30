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

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

/**
 * Kabbur et al., <strong>FISM: Factored Item Similarity Models for Top-N Recommender Systems</strong>, KDD 2013.
 * 
 * @author guoguibing
 * 
 */
@Configuration("binThold, rho, alpha, factors, lRate, maxLRate, regI, regB, iters")
public class FISMauc extends IterativeRecommender {

	private int rho;
	private float alpha;

	public FISMauc(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {
		P = new DenseMatrix(numItems, numFactors);
		Q = new DenseMatrix(numItems, numFactors);
		P.init(smallValue);
		Q.init(smallValue);

		itemBias = new DenseVector(numItems);
		itemBias.init(smallValue);

		algoOptions = cf.getParamOptions("FISM");
		rho = algoOptions.getInt("-rho");
		alpha = algoOptions.getFloat("-alpha");

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			// update throughout each (u, i, j) cell
			for (int u : trainMatrix.rows()) {
				SparseVector Ru = trainMatrix.row(u);
				int[] ratedItems = Ru.getIndex();

				for (VectorEntry ve : Ru) {
					int i = ve.index();
					double rui = ve.get();

					// sample a set of items unrated by user u
					List<Integer> js = new ArrayList<>();
					int len = 0;
					while (len < rho) {
						int j = Randoms.uniform(numItems);
						if (Ru.contains(j) || js.contains(j))
							continue;

						js.add(j);
						len++;
					}

					double wu = Ru.getCount() - 1 > 0 ? Math.pow(Ru.getCount() - 1, -alpha) : 0;
					double[] x = new double[numFactors];

					// update for each unrated item
					for (int j : js) {

						double sum_i = 0, sum_j = 0;
						for (int k : ratedItems) {
							// for test, i and j will be always unequal as j is
							// unrated
							if (i != k)
								sum_i += DenseMatrix.rowMult(P, k, Q, i);

							sum_j += DenseMatrix.rowMult(P, k, Q, j);
						}

						double bi = itemBias.get(i), bj = itemBias.get(j);

						double pui = bi + wu * sum_i;
						double puj = bj + Math.pow(Ru.getCount(), -alpha) * sum_j;
						double ruj = 0;

						double eij = (rui - ruj) - (pui - puj);

						loss += eij * eij;

						// update bi
						itemBias.add(i, lRate * (eij - regB * bi));

						// update bj
						itemBias.add(j, -lRate * (eij - regB * bj));

						loss += regB * bi * bi - regB * bj * bj;

						// update qif, qjf
						for (int f = 0; f < numFactors; f++) {
							double qif = Q.get(i, f), qjf = Q.get(j, f);

							double sum_k = 0;
							for (int k : ratedItems) {
								if (k != i) {
									sum_k += P.get(k, f);
								}
							}

							double delta_i = eij * wu * sum_k - regI * qif;
							Q.add(i, f, lRate * delta_i);

							double delta_j = eij * wu * sum_k - regI * qjf;
							Q.add(j, f, -lRate * delta_j);

							x[f] += eij * (qif - qjf);

							loss += regI * qif * qif - regI * qjf * qjf;
						}
					}

					// update for each rated item
					for (int j : ratedItems) {
						if (j != i) {
							for (int f = 0; f < numFactors; f++) {
								double pjf = P.get(j, f);
								double delta = wu * x[f] / rho - regI * pjf;

								P.add(j, f, lRate * delta);

								loss += regI * pjf * pjf;
							}
						}
					}
				}

			}

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int i) throws Exception {

		double sum = 0;
		int count = 0;

		List<Integer> items = userItemsCache.get(u);
		for (int j : items) {
			// for test, i and j will be always unequal as j is unrated
			if (i != j) {
				sum += DenseMatrix.rowMult(P, j, Q, i);
				count++;
			}
		}

		double wu = count > 0 ? Math.pow(count, -alpha) : 0;

		return itemBias.get(i) + wu * sum;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, rho, alpha, numFactors, initLRate, maxLRate, regI, regB,
				numIters });
	}
}
