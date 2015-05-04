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
import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * Bayesian UCM: Nicola Barbieri et al., <strong>Modeling Item Selection and Relevance for Accurate Recommendations: a
 * Bayesian Approach</strong>, RecSys 2011.
 * 
 * @author Guo Guibing
 *
 */
@AddConfiguration(before = "factors, alpha, beta, gamma")
public class BUCM extends GraphicRecommender {

	private float initGamma;
	private DenseVector gamma;

	public BUCM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	/**
	 * number of occurrences of entry (t, i, r)
	 */
	private int[][][] Nkir;

	/**
	 * cumulative statistics of probabilities of (t, i, r)
	 */
	private double[][][] epsilonSum;

	/**
	 * posterior probabilities of parameters epsilon_{k, i, r}
	 */
	protected double[][][] epsilon;

	@Override
	protected void initModel() throws Exception {

		// cumulative parameters
		PukSum = new DenseMatrix(numUsers, numFactors);
		PkiSum = new DenseMatrix(numItems, numFactors);
		epsilonSum = new double[numFactors][numItems][numLevels];

		// initialize count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);

		Nik = new DenseMatrix(numItems, numFactors);
		Nk = new DenseVector(numFactors);

		Nkir = new int[numFactors][numItems][numLevels];

		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseVector(numItems);
		beta.setAll(initBeta);

		gamma = new DenseVector(numLevels);
		initGamma = algoOptions.getFloat("-gamma", 1.0f / numLevels);
		gamma.setAll(initGamma);

		// initialize topics
		z = HashBasedTable.create();
		for (MatrixEntry me : trainMatrix) {

			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = (int) (Math.random() * numFactors); // 0 ~ k-1

			// assign a topic t to pair (u, i)
			z.put(u, i, t);
			// for users
			Nuk.add(u, t, 1);
			Nu.add(u, 1);
			// for items
			Nik.add(i, t, 1);
			Nk.add(t, 1);
			// for ratings
			Nkir[t][i][r]++;
		}
	}

	@Override
	protected void eStep() {

		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double sumGamma = gamma.sum();

		// collapse Gibbs sampling
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = z.get(u, i);

			Nuk.add(u, t, -1);
			Nu.add(u, -1);
			Nik.add(i, t, -1);
			Nk.add(t, -1);
			Nkir[t][i][r]--;

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			double v1, v2, v3;
			for (int k = 0; k < numFactors; k++) {

				v1 = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);

				v2 = (Nik.get(i, k) + beta.get(i)) / (Nk.get(t) + sumBeta);

				v3 = (Nkir[k][i][r] + gamma.get(r)) / (Nik.get(i, t) + sumGamma);

				p[k] = v1 * v2 * v3;
			}
			// cumulate multinomial parameters
			for (int k = 1; k < p.length; k++) {
				p[k] += p[k - 1];
			}
			// scaled sample because of unnormalized p[], randomly sampled a new topic t
			double rand = Math.random() * p[numFactors - 1];
			for (t = 0; t < p.length; t++) {
				if (rand < p[t])
					break;
			}

			// new topic t
			z.put(u, i, t);

			// add newly estimated z_i to count variables
			Nuk.add(u, t, 1);
			Nu.add(u, 1);
			Nik.add(i, t, 1);
			Nk.add(t, 1);
			Nkir[t][i][r]++;
		}
	}

	/**
	 * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
	 */
	@Override
	protected void mStep() {
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double sumGamma = gamma.sum();
		double ak, bi, gr;

		// update alpha
		for (int k = 0; k < numFactors; k++) {

			ak = alpha.get(k);
			double numerator = 0, denominator = 0;
			for (int u = 0; u < numUsers; u++) {
				numerator += digamma(Nuk.get(u, k) + ak) - digamma(ak);
				denominator += digamma(Nu.get(u) + sumAlpha) - digamma(sumAlpha);
			}
			if (numerator != 0)
				alpha.set(k, ak * (numerator / denominator));
		}

		// update beta
		for (int i = 0; i < numItems; i++) {

			bi = beta.get(i);
			double numerator = 0, denominator = 0;
			for (int k = 0; k < numFactors; k++) {
				numerator += digamma(Nik.get(i, k) + bi) - digamma(bi);
				denominator += digamma(Nk.get(k) + sumBeta) - digamma(sumBeta);
			}
			if (numerator != 0)
				beta.set(i, bi * (numerator / denominator));
		}

		// update gamma
		for (int r = 0; r < numLevels; r++) {

			gr = gamma.get(r);
			double numerator = 0, denominator = 0;
			for (int i = 0; i < numItems; i++) {
				for (int k = 0; k < numFactors; k++) {
					numerator += digamma(Nkir[k][i][r] + gr) - digamma(gr);
					denominator += digamma(Nik.get(i, k) + sumGamma) - digamma(sumGamma);
				}
			}
			if (numerator != 0)
				gamma.set(r, gr * (numerator / denominator));
		}

	}

	protected void readoutParams() {
		double val = 0;
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double sumGamma = gamma.sum();

		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);
				PukSum.add(u, k, val);
			}
		}

		for (int i = 0; i < numItems; i++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nik.get(i, k) + beta.get(i)) / (Nk.get(k) + sumBeta);
				PkiSum.add(i, k, val);
			}
		}

		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					val = (Nkir[k][i][r] + gamma.get(r)) / (Nik.get(i, k) + sumGamma);
					epsilonSum[k][i][r] += val;
				}
			}
		}
		numStats++;
	}

	@Override
	protected void postProbDistr() {
		Puk = PukSum.scale(1.0 / numStats);
		Pki = PkiSum.scale(1.0 / numStats);

		epsilon = new double[numFactors][numItems][numLevels];
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					epsilon[k][i][r] = epsilonSum[k][i][r] / numStats;
				}
			}
		}
	}

	@Override
	protected double predict(int u, int i) throws Exception {
		double pred = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = (r + 1) * minRate;

			double prob = 0;
			for (int k = 0; k < numFactors; k++) {
				prob += Puk.get(u, k) * epsilon[k][i][r];
			}

			pred += prob * rate;
		}

		return pred;
	}

	@Override
	protected double ranking(int u, int j) throws Exception {
		double rank = 0;

		for (int k = 0; k < numFactors; k++) {

			double sum = 0;
			for (int r = 0; r < numLevels; r++) {
				double rate = (r + 1) * minRate;
				if (rate > globalMean) {
					sum += epsilon[k][j][r];
				}
			}

			rank += Puk.get(u, k) * Pki.get(j, k) * sum;
		}

		return rank;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, initAlpha, initBeta, initGamma }) + ", " + super.toString();
	}
}
