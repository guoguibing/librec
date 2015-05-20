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

package librec.rating;

import static happy.coding.math.Gamma.digamma;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * User Rating Profile: a LDA model for rating prediction. <br>
 * 
 * Benjamin Marlin, <strong>Modeling user rating profiles for collaborative filtering</strong>, NIPS 2003.<br>
 * 
 * Nicola Barbieri, <strong>Regularized gibbs sampling for user profiling with soft constraints</strong>, ASONAM 2011.
 * 
 * @author Guo Guibing
 *
 */
@AddConfiguration(before = "factors, alpha, beta")
public class URP extends GraphicRecommender {

	private double preRMSE;

	public URP(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	/**
	 * number of occurrences of entry (t, i, r)
	 */
	private int[][][] Nkir;

	/**
	 * cumulative statistics of probabilities of (t, i, r)
	 */
	private double[][][] PkirSum;

	/**
	 * posterior probabilities of parameters phi_{k, i, r}
	 */
	protected double[][][] Pkir;

	@Override
	protected void initModel() throws Exception {

		// cumulative parameters
		PukSum = new DenseMatrix(numUsers, numFactors);
		PkirSum = new double[numFactors][numItems][numLevels];

		// initialize count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);

		Nkir = new int[numFactors][numItems][numLevels];
		Nki = new DenseMatrix(numFactors, numItems);

		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseVector(numLevels);
		beta.setAll(initBeta);

		// initialize topics
		z = HashBasedTable.create();
		for (MatrixEntry me : trainMatrix) {

			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = ratingScale.indexOf(rui); // rating level 0 ~ numLevels
			int t = (int) (Math.random() * numFactors); // 0 ~ k-1

			// assign a topic t to pair (u, i)
			z.put(u, i, t);
			// number of pairs (u, t) in (u, i, t)
			Nuk.add(u, t, 1);
			// total number of items of user u
			Nu.add(u, 1);

			// number of pairs (t, i, r)
			Nkir[t][i][r]++;
			// total number of words assigned to topic t
			Nki.add(t, i, 1);
		}

	}

	@Override
	protected void eStep() {

		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();

		// collapse Gibbs sampling
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rui = me.get();

			int r = (int) (rui / minRate - 1); // rating level 0 ~ numLevels
			int t = z.get(u, i);

			Nuk.add(u, t, -1);
			Nu.add(u, -1);
			Nkir[t][i][r]--;
			Nki.add(t, i, -1);

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			for (int k = 0; k < numFactors; k++) {
				p[k] = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha) * (Nkir[k][i][r] + beta.get(r))
						/ (Nki.get(k, i) + sumBeta);
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
			Nkir[t][i][r]++;
			Nki.add(t, i, 1);
		}
	}

	/**
	 * Thomas P. Minka, Estimating a Dirichlet distribution, see Eq.(55)
	 */
	@Override
	protected void mStep() {
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double ak, br;

		// update alpha vector
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

		// update beta_k
		for (int r = 0; r < numLevels; r++) {
			br = beta.get(r);
			double numerator = 0, denominator = 0;
			for (int i = 0; i < numItems; i++) {
				for (int k = 0; k < numFactors; k++) {
					numerator += digamma(Nkir[k][i][r] + br) - digamma(br);
					denominator += digamma(Nki.get(k, i) + sumBeta) - digamma(sumBeta);
				}
			}
			if (numerator != 0)
				beta.set(r, br * (numerator / denominator));
		}

	}

	protected void readoutParams() {
		double val = 0;
		double sumAlpha = alpha.sum();

		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);
				PukSum.add(u, k, val);
			}
		}

		double sumBeta = beta.sum();
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					val = (Nkir[k][i][r] + beta.get(r)) / (Nki.get(k, i) + sumBeta);
					PkirSum[k][i][r] += val;
				}
			}
		}
		numStats++;
	}

	@Override
	protected void estimateParams() {
		Puk = PukSum.scale(1.0 / numStats);

		Pkir = new double[numFactors][numItems][numLevels];
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					Pkir[k][i][r] = PkirSum[k][i][r] / numStats;
				}
			}
		}
	}

	@Override
	protected boolean isConverged(int iter) throws Exception {

		if (validationMatrix == null)
			return false;

		// get posterior probability distribution first
		estimateParams();

		// compute current RMSE
		int numCount = 0;
		double sum = 0;
		for (MatrixEntry me : validationMatrix) {
			double rate = me.get();

			int u = me.row();
			int j = me.column();

			double pred = predict(u, j, true);
			if (Double.isNaN(pred))
				continue;

			double err = rate - pred;

			sum += err * err;
			numCount++;
		}

		double RMSE = Math.sqrt(sum / numCount);
		double delta = RMSE - preRMSE;

		if (verbose) {
			Logs.debug("{}{} iter {} achieves RMSE = {}, delta_RMSE = {}", algoName, foldInfo, iter, (float) RMSE,
					(float) (delta));
		}

		if (numStats > 1 && delta > 0)
			return true;

		preRMSE = RMSE;
		return false;
	}

	@Override
	protected double predict(int u, int i) throws Exception {
		double pred = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = ratingScale.get(r);

			double prob = 0;
			for (int k = 0; k < numFactors; k++) {
				prob += Puk.get(u, k) * Pkir[k][i][r];
			}

			pred += prob * rate;
		}

		return pred;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, alpha, beta }) + ", " + super.toString();
	}
}
