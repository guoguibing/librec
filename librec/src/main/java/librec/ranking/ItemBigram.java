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

import static happy.coding.math.Gamma.digamma;
import happy.coding.io.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.RatingContext;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * Hanna M. Wallach, <strong>Topic Modeling: Beyond Bag-of-Words</strong>, ICML 2006.
 * 
 * @author Guo Guibing
 *
 */
@AddConfiguration(before = "factors, alpha, beta")
public class ItemBigram extends GraphicRecommender {

	private Map<Integer, List<Integer>> userItemsMap;

	/**
	 * k: current topic; j: previously rated item; i: current item
	 */
	private int[][][] Nkji;
	private DenseMatrix Nkj;
	private double[][][] Pkji, PkjiSum;

	private DenseMatrix beta;

	public ItemBigram(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {
		// build the training data, sorting by date
		userItemsMap = new HashMap<>();
		for (int u = 0; u < numUsers; u++) {
			List<Integer> unsortedItems = trainMatrix.getColumns(u);
			int size = unsortedItems.size();

			List<RatingContext> rcs = new ArrayList<>(size);
			for (Integer i : unsortedItems) {
				rcs.add(new RatingContext(u, i, timestamps.get(u, i)));
			}
			Collections.sort(rcs);

			List<Integer> sortedItems = new ArrayList<>(size);
			for (RatingContext rc : rcs) {
				sortedItems.add(rc.getItem());
			}

			userItemsMap.put(u, sortedItems);
		}

		// count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);
		Nkji = new int[numFactors][numItems + 1][numItems];
		Nkj = new DenseMatrix(numFactors, numItems + 1);

		// Logs.debug("Nkji consumes {} bytes", Strings.toString(Memory.bytes(Nkji)));

		// parameters
		PukSum = new DenseMatrix(numUsers, numFactors);
		PkjiSum = new double[numFactors][numItems + 1][numItems];
		Pkji = new double[numFactors][numItems + 1][numItems];

		// hyper-parameters
		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseMatrix(numFactors, numItems + 1);
		beta.setAll(initBeta);

		// initialization
		z = HashBasedTable.create();
		for (Entry<Integer, List<Integer>> en : userItemsMap.entrySet()) {
			int u = en.getKey();
			List<Integer> items = en.getValue();

			for (int m = 0; m < items.size(); m++) {
				int i = items.get(m);

				int k = (int) (Math.random() * numFactors);
				z.put(u, i, k);

				Nuk.add(u, k, 1.0);
				Nu.add(u, 1.0);

				int j = m > 0 ? items.get(m - 1) : numItems;
				Nkji[k][j][i]++;
				Nkj.add(k, j, 1);
			}
		}
	}

	@Override
	protected void eStep() {
		double sumAlpha = alpha.sum();
		double v1, v2;

		for (Entry<Integer, List<Integer>> en : userItemsMap.entrySet()) {
			int u = en.getKey();
			List<Integer> items = en.getValue();

			for (int m = 0; m < items.size(); m++) {
				int i = items.get(m);
				int k = z.get(u, i);

				Nuk.add(u, k, -1.0);
				Nu.add(u, -1.0);

				int j = m > 0 ? items.get(m - 1) : numItems;
				Nkji[k][j][i]--;
				Nkj.add(k, j, -1);

				double[] Pk = new double[numFactors];
				for (int t = 0; t < numFactors; t++) {
					v1 = (Nuk.get(u, t) + alpha.get(t)) / (Nu.get(u) + sumAlpha);
					v2 = (Nkji[t][j][i] + beta.get(t, j)) / (Nkj.get(t, j) + beta.sumOfRow(t));

					Pk[t] = v1 * v2;
				}

				for (int t = 1; t < numFactors; t++) {
					Pk[t] += Pk[t - 1];
				}

				double rand = Math.random() * Pk[numFactors - 1];
				for (k = 0; k < numFactors; k++) {
					if (rand < Pk[k])
						break;
				}

				z.put(u, i, k);

				Nuk.add(u, k, 1.0);
				Nu.add(u, 1.0);

				Nkji[k][j][i]++;
				Nkj.add(k, j, 1.0);
			}
		}
	}

	@Override
	protected void mStep() {
		double sumAlpha = alpha.sum();
		for (int k = 0; k < numFactors; k++) {
			double ak = alpha.get(k);
			double numerator = 0, denominator = 0;
			for (int u = 0; u < numUsers; u++) {
				numerator += digamma(Nuk.get(u, k) + ak) - digamma(ak);
				denominator += digamma(Nu.get(u) + sumAlpha) - digamma(sumAlpha);
			}

			if (numerator != 0)
				alpha.set(k, ak * (numerator / denominator));
		}

		for (int k = 0; k < numFactors; k++) {
			double bk = beta.sumOfRow(k);
			for (int j = 0; j < numItems + 1; j++) {
				double bkj = beta.get(k, j);
				double numerator = 0, denominator = 0;
				for (int i = 0; i < numItems; i++) {
					numerator += digamma(Nkji[k][j][i] + bkj) - digamma(bkj);
					denominator += digamma(Nkj.get(k, j) + bk) - digamma(bk);
				}

				if (numerator != 0)
					beta.set(k, j, bkj * (numerator / denominator));
			}
		}
	}

	@Override
	protected void readoutParams() {
		double val = 0.0;
		double sumAlpha = alpha.sum();

		for (int u = 0; u < numFactors; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);
				PukSum.add(u, k, val);
			}
		}

		for (int k = 0; k < numFactors; k++) {
			double bk = beta.sumOfRow(k);
			for (int j = 0; j < numItems + 1; j++) {
				for (int i = 0; i < numItems; i++) {
					val = (Nkji[k][j][i] + beta.get(k, j)) / (Nkj.get(k, j) + bk);
					PkjiSum[k][j][i] += val;
				}
			}
		}

		numStats++;
	}

	@Override
	protected void estimateParams() {
		Puk = PukSum.scale(1.0 / numStats);

		for (int k = 0; k < numFactors; k++) {
			for (int j = 0; j < numItems + 1; j++) {
				for (int i = 0; i < numItems; i++) {
					Pkji[k][j][i] = PkjiSum[k][j][i] / numStats;
				}
			}
		}
	}

	@Override
	protected double ranking(int u, int i) throws Exception {
		List<Integer> items = userItemsMap.get(u);
		int j = items.size() < 1 ? numItems : items.get(items.size() - 1); // last rated item

		double rank = 0;
		for (int k = 0; k < numFactors; k++) {
			rank += Puk.get(u, k) * Pkji[k][j][i];
		}

		return rank;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, initAlpha, initBeta }) + ", " + super.toString();
	}

}
