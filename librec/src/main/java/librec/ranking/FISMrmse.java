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

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Kabbur et al., <strong>FISM: Factored Item Similarity Models for Top-N Recommender Systems</strong>, KDD 2013.
 * 
 * @author guoguibing
 * 
 */
@Configuration("binThold, rho, alpha, factors, lRate, maxLRate, regI, regB, iters")
public class FISMrmse extends IterativeRecommender {

	private float rho, alpha;
	private int nnz;

	public FISMrmse(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {
		P = new DenseMatrix(numItems, numFactors);
		Q = new DenseMatrix(numItems, numFactors);
		P.init(0.01);
		Q.init(0.01);

		userBias = new DenseVector(numUsers);
		itemBias = new DenseVector(numItems);
		userBias.init(0.01);
		itemBias.init(0.01);

		nnz = trainMatrix.size();
		algoOptions = cf.getParamOptions("FISM");
		rho = algoOptions.getFloat("-rho");
		alpha = algoOptions.getFloat("-alpha");

		userItemsCache = trainMatrix.rowColumnsCache(cacheSpec);
	}

	@Override
	protected void buildModel() throws Exception {

		int sampleSize = (int) (rho * nnz);
		int totalSize = numUsers * numItems;

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			// temporal data
			DenseMatrix PS = new DenseMatrix(numItems, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			// new training data by sampling negative values
			Table<Integer, Integer, Double> R = trainMatrix.getDataTable();

			// make a random sample of negative feedback (total - nnz)
			List<Integer> indices = Randoms.randInts(sampleSize, 0, totalSize - nnz);
			int index = 0, count = 0;
			boolean isDone = false;
			for (int u = 0; u < numUsers; u++) {
				for (int j = 0; j < numItems; j++) {
					double ruj = trainMatrix.get(u, j);
					if (ruj != 0)
						continue; // rated items

					if (count++ == indices.get(index)) {
						R.put(u, j, 0.0);
						index++;
						if (index >= indices.size()) {
							isDone = true;
							break;
						}
					}
				}
				if (isDone)
					break;
			}

			// update throughout each user-item-rating (u, j, ruj) cell
			for (Cell<Integer, Integer, Double> cell : R.cellSet()) {
				int u = cell.getRowKey();
				int j = cell.getColumnKey();
				double ruj = cell.getValue();

				// for efficiency, use the below code to predict ruj instead of
				// simply using "predict(u,j)"
				SparseVector Ru = trainMatrix.row(u);
				double bu = userBias.get(u), bj = itemBias.get(j);

				double sum_ij = 0;
				int cnt = 0;
				for (VectorEntry ve : Ru) {
					int i = ve.index();
					// for training, i and j should be equal as j may be rated
					// or unrated
					if (i != j) {
						sum_ij += DenseMatrix.rowMult(P, i, Q, j);
						cnt++;
					}
				}

				double wu = cnt > 0 ? Math.pow(cnt, -alpha) : 0;
				double puj = bu + bj + wu * sum_ij;

				double euj = puj - ruj;

				loss += euj * euj;

				// update bu
				userBias.add(u, -lRate * (euj + regB * bu));

				// update bj
				itemBias.add(j, -lRate * (euj + regB * bj));

				loss += regB * bu * bu + regB * bj * bj;

				// update qjf
				for (int f = 0; f < numFactors; f++) {
					double qjf = Q.get(j, f);

					double sum_i = 0;
					for (VectorEntry ve : Ru) {
						int i = ve.index();
						if (i != j) {
							sum_i += P.get(i, f);
						}
					}

					double delta = euj * wu * sum_i + regI * qjf;
					QS.add(j, f, -lRate * delta);

					loss += regI * qjf * qjf;
				}

				// update pif
				for (VectorEntry ve : Ru) {
					int i = ve.index();
					if (i != j) {
						for (int f = 0; f < numFactors; f++) {
							double pif = P.get(i, f);
							double delta = euj * wu * Q.get(j, f) + regI * pif;
							PS.add(i, f, -lRate * delta);

							loss += regI * pif * pif;
						}
					}
				}
			}

			P = P.add(PS);
			Q = Q.add(QS);

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) throws Exception {
		double pred = userBias.get(u) + itemBias.get(j);

		double sum = 0;
		int count = 0;

		List<Integer> items = userItemsCache.get(u);
		for (int i : items) {
			// for test, i and j will be always unequal as j is unrated
			if (i != j) {
				sum += DenseMatrix.rowMult(P, i, Q, j);
				count++;
			}
		}

		double wu = count > 0 ? Math.pow(count, -alpha) : 0;

		return pred + wu * sum;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, rho, alpha, numFactors, initLRate, maxLRate, regI, regB,
				numIters }, ",");
	}
}
