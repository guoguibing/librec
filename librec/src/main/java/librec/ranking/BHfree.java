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

import happy.coding.io.Strings;
import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Barbieri et al., <strong>Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference
 * Data</strong>, SDM 2012. <br>
 * 
 * <p>
 * <strong>Remarks:</strong> this class implements the BH-free method.
 * </p>
 * 
 * @author Guo Guibing
 *
 */
@AddConfiguration(before = "K, L, alpha, beta, gamma, sigma")
public class BHfree extends GraphicRecommender {

	private float initGamma, initSigma;
	private int K, L;

	private DenseMatrix Nkl;
	private int[][][] Nklr, Nkli;

	private Table<Integer, Integer, Integer> Zk, Zl;

	// parameters
	private DenseMatrix Puk, Pkl, PukSum, PklSum;
	private double[][][] Pklr, Pkli, PklrSum, PkliSum;

	public BHfree(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {

		K = algoOptions.getInt("-k");
		L = algoOptions.getInt("-l");

		initAlpha = pgmOptions.getFloat("-alpha", 1.0f / K);
		initBeta = pgmOptions.getFloat("-beta", 1.0f / L);

		initGamma = algoOptions.getFloat("-gamma", 1.0f / numLevels);
		initSigma = algoOptions.getFloat("-sigma", 1.0f / numItems);

		Nuk = new DenseMatrix(numUsers, K);
		Nu = new DenseVector(numUsers);

		Nkl = new DenseMatrix(K, L);
		Nk = new DenseVector(K);

		Nklr = new int[K][L][numLevels];
		Nkli = new int[K][L][numItems];

		Zk = HashBasedTable.create();
		Zl = HashBasedTable.create();

		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rate = me.get();
			int r = ratingScale.indexOf(rate);

			int k = (int) (K * Math.random()); // user's topic k
			int l = (int) (L * Math.random()); // item's topic l

			Nuk.add(u, k, 1);
			Nu.add(u, 1);

			Nkl.add(k, l, 1);
			Nk.add(k, 1);

			Nklr[k][l][r]++;
			Nkli[k][l][i]++;

			Zk.put(u, i, k);
			Zl.put(u, i, l);
		}

		// parameters
		PukSum = new DenseMatrix(numUsers, K);
		PklSum = new DenseMatrix(K, L);
		PklrSum = new double[K][L][numLevels];
		Pklr = new double[K][L][numLevels];
		PkliSum = new double[K][L][numItems];
		Pkli = new double[K][L][numItems];
	}

	@Override
	protected void eStep() {

		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rate = me.get();
			int r = ratingScale.indexOf(rate);

			int k = Zk.get(u, i);
			int l = Zl.get(u, i);

			Nuk.add(u, k, -1);
			Nu.add(u, -1);
			Nkl.add(k, l, -1);
			Nk.add(k, -1);
			Nklr[k][l][r]--;
			Nkli[k][l][i]--;

			DenseMatrix Pzw = new DenseMatrix(K, L);
			double sum = 0;
			for (int z = 0; z < K; z++) {
				for (int w = 0; w < L; w++) {
					double v1 = (Nuk.get(u, k) + initAlpha) / (Nu.get(u) + K * initAlpha);
					double v2 = (Nkl.get(k, l) + initBeta) / (Nk.get(k) + L * initBeta);
					double v3 = (Nklr[k][l][r] + initGamma) / (Nkl.get(k, l) + numLevels * initGamma);
					double v4 = (Nkli[k][l][i] + initSigma) / (Nkl.get(k, l) + numItems * initSigma);

					double val = v1 * v2 * v3 * v4;
					Pzw.set(z, w, val);
					sum += val;
				}
			}

			// normalization
			Pzw = Pzw.scale(1.0 / sum);

			// resample k
			double[] Pz = new double[K];
			for (int z = 0; z < K; z++)
				Pz[z] = Pzw.sumOfRow(z);
			for (int z = 1; z < K; z++)
				Pz[z] += Pz[z - 1];
			double rand = Math.random();
			for (k = 0; k < K; k++) {
				if (rand < Pz[k])
					break;
			}

			// resample l
			double[] Pw = new double[L];
			for (int w = 0; w < L; w++)
				Pw[w] = Pzw.sumOfColumn(w);
			for (int w = 1; w < L; w++)
				Pw[w] += Pw[w - 1];

			rand = Math.random();
			for (l = 0; l < L; l++) {
				if (rand < Pw[l])
					break;
			}

			// add statistic
			Nuk.add(u, k, 1);
			Nu.add(u, 1);
			Nkl.add(k, l, 1);
			Nk.add(k, 1);
			Nklr[k][l][r]++;
			Nkli[k][l][i]++;

			Zk.put(u, i, k);
			Zl.put(u, i, l);
		}
	}

	@Override
	protected void readoutParams() {
		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < K; k++) {
				PukSum.add(u, k, (Nuk.get(u, k) + initAlpha) / (Nu.get(u) + K * initAlpha));
			}
		}

		for (int k = 0; k < K; k++) {
			for (int l = 0; l < L; l++) {
				PklSum.add(k, l, (Nkl.get(k, l) + initBeta) / (Nk.get(k) + L * initBeta));
			}
		}

		for (int k = 0; k < K; k++) {
			for (int l = 0; l < L; l++) {
				for (int r = 0; r < numLevels; r++) {
					PklrSum[k][l][r] += (Nklr[k][l][r] + initGamma) / (Nkl.get(k, l) + numLevels * initGamma);
				}
			}
		}

		for (int k = 0; k < K; k++) {
			for (int l = 0; l < L; l++) {
				for (int i = 0; i < numItems; i++) {
					PkliSum[k][l][i] += (Nkli[k][l][i] + initSigma) / (Nkl.get(k, l) + numItems * initSigma);
				}
			}
		}

		numStats++;
	}

	@Override
	protected void estimateParams() {

		double scale = 1.0 / numStats;
		Puk = PukSum.scale(scale);
		Pkl = PklSum.scale(scale);

		for (int k = 0; k < K; k++) {
			for (int l = 0; l < L; l++) {
				for (int r = 0; r < numLevels; r++) {
					Pklr[k][l][r] = PklrSum[k][l][r] * scale;
				}
			}
		}

		for (int k = 0; k < K; k++) {
			for (int l = 0; l < L; l++) {
				for (int i = 0; i < numItems; i++) {
					Pkli[k][l][i] = PkliSum[k][l][i] * scale;
				}
			}
		}
	}

	@Override
	protected double predict(int u, int j) throws Exception {
		double sum = 0, probs = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = ratingScale.get(r);

			double prob = 0;
			for (int k = 0; k < K; k++) {
				for (int l = 0; l < L; l++) {
					prob += Puk.get(u, k) * Pkl.get(k, l) * Pklr[k][l][r];
				}
			}

			sum += rate * prob;
			probs += prob;
		}

		return sum / probs;
	}

	@Override
	protected double ranking(int u, int j) throws Exception {
		double rank = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = ratingScale.get(r);

			double prob = 0;
			for (int k = 0; k < K; k++) {
				for (int l = 0; l < L; l++) {
					prob += Puk.get(u, k) * Pkl.get(k, l) * Pkli[k][l][j] * Pklr[k][l][r];
				}
			}

			rank += rate * prob;
		}

		return rank;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { K, L, initAlpha, initBeta, initGamma, initSigma }) + ", "
				+ super.toString();
	}

}
