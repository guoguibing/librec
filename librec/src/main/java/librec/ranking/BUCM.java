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

import static librec.util.Gamma.digamma;
import librec.data.AddConfiguration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;
import librec.util.Logs;
import librec.util.Strings;

import com.google.common.collect.HashBasedTable;

/**
 * Bayesian UCM: Nicola Barbieri et al., <strong>Modeling Item Selection and Relevance for Accurate Recommendations: a
 * Bayesian Approach</strong>, RecSys 2011.
 * 
 * <p>
 * Thank the paper authors for providing source code and for having valuable discussion.
 * </p>
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
	private double[][][] PkirSum;

	/**
	 * posterior probabilities of parameters epsilon_{k, i, r}
	 */
	protected double[][][] Pkir;

	@Override
	protected void initModel() throws Exception {

		// cumulative parameters
		PukSum = new DenseMatrix(numUsers, numFactors);
		PkiSum = new DenseMatrix(numFactors, numItems);
		PkirSum = new double[numFactors][numItems][numLevels];

		// initialize count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);

		Nki = new DenseMatrix(numFactors, numItems);
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

			int r = ratingScale.indexOf(rui); // rating level 0 ~ numLevels
			int t = (int) (Math.random() * numFactors); // 0 ~ k-1

			// assign a topic t to pair (u, i)
			z.put(u, i, t);
			// for users
			Nuk.add(u, t, 1);
			Nu.add(u, 1);
			// for items
			Nki.add(t, i, 1);
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

			int r = ratingScale.indexOf(rui); // rating level 0 ~ numLevels
			int t = z.get(u, i);

			Nuk.add(u, t, -1);
			Nu.add(u, -1);
			Nki.add(t, i, -1);
			Nk.add(t, -1);
			Nkir[t][i][r]--;

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			double v1, v2, v3;
			for (int k = 0; k < numFactors; k++) {

				v1 = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);
				v2 = (Nki.get(k, i) + beta.get(i)) / (Nk.get(k) + sumBeta);
				v3 = (Nkir[k][i][r] + gamma.get(r)) / (Nki.get(k, i) + sumGamma);

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
			Nki.add(t, i, 1);
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
				numerator += digamma(Nki.get(k, i) + bi) - digamma(bi);
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
					denominator += digamma(Nki.get(k, i) + sumGamma) - digamma(sumGamma);
				}
			}
			if (numerator != 0)
				gamma.set(r, gr * (numerator / denominator));
		}

	}

	@Override
	protected boolean isConverged(int iter) throws Exception {
		loss = 0;

		// get params
		estimateParams();

		// compute likelihood
		int count = 0;
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			double rui = me.get();
			int r = ratingScale.indexOf(rui);

			double prob = 0;
			for (int k = 0; k < numFactors; k++) {
				prob += Puk.get(u, k) * Pki.get(k, i) * Pkir[k][i][r];
			}

			loss += -Math.log(prob);
			count++;
		}
		loss /= count;

		float delta = (float) (loss - lastLoss); // loss gets smaller, delta <= 0

		Logs.debug("{}{} iter {} achieves log likelihood = {}, delta_LogLLH = {}", algoName, foldInfo, iter,
				(float) loss, delta);

		if (numStats > 1 && delta > 0) {
			Logs.debug("{}{} has converged at iter {}", algoName, foldInfo, iter);
			return true;
		}

		lastLoss = loss;

		return false;
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

		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				val = (Nki.get(k, i) + beta.get(i)) / (Nk.get(k) + sumBeta);
				PkiSum.add(k, i, val);
			}
		}

		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				for (int r = 0; r < numLevels; r++) {
					val = (Nkir[k][i][r] + gamma.get(r)) / (Nki.get(k, i) + sumGamma);
					PkirSum[k][i][r] += val;
				}
			}
		}
		numStats++;
	}

	@Override
	protected void estimateParams() {
		Puk = PukSum.scale(1.0 / numStats);
		Pki = PkiSum.scale(1.0 / numStats);

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
	protected double perplexity(int u, int j, double ruj) throws Exception {
		int r = (int) (ruj / minRate) - 1;

		double prob = 0;
		for (int k = 0; k < numFactors; k++) {
			prob += Puk.get(u, k) * Pki.get(k, j) * Pkir[k][j][r];
		}

		return -Math.log(prob);
	}

	@Override
	protected double predict(int u, int i) throws Exception {
		double pred = 0, probs = 0;

		for (int r = 0; r < numLevels; r++) {
			double rate = ratingScale.get(r);

			double prob = 0;
			for (int k = 0; k < numFactors; k++) {
				prob += Puk.get(u, k) * Pki.get(k, i) * Pkir[k][i][r];
			}

			pred += prob * rate;
			probs += prob;
		}

		return pred / probs;
	}

	@Override
	protected double ranking(int u, int j) throws Exception {
		double rank = 0;

		for (int k = 0; k < numFactors; k++) {

			double sum = 0;
			for (int r = 0; r < numLevels; r++) {
				double rate = ratingScale.get(r);
				if (rate > globalMean) {
					sum += Pkir[k][j][r];
				}
			}

			rank += Puk.get(u, k) * Pki.get(k, j) * sum;
		}

		return rank;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, initAlpha, initBeta, initGamma }) + ", " + super.toString();
	}
}
