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
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;

/**
 * Latent Dirichlet Allocation for implicit feedback: Tom Griffiths, <strong>Gibbs sampling in the generative model of
 * Latent Dirichlet Allocation</strong>, 2002.
 * 
 * @author Guibing Guo
 *
 */
public class LDA extends GraphicRecommender {

	public LDA(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {

		thetaSum = new DenseMatrix(numUsers, numFactors);
		phiSum = new DenseMatrix(numFactors, numItems);

		// initialize count variables.
		Nik = new DenseMatrix(numItems, numFactors);
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nk = new DenseVector(numFactors);
		Nu = new DenseVector(numUsers);

		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseVector(numItems);
		beta.setAll(initBeta);

		// The z_u,i are initialized to values in [0, K-1] to determine the initial state of the Markov chain.
		z = HashBasedTable.create();
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			int t = (int) (Math.random() * numFactors); // 0 ~ k-1

			// assign a topic t to pair (u, i)
			z.put(u, i, t);

			// number of items of user u assigned to topic t.
			Nuk.add(u, t, 1);
			// total number of items of user u
			Nu.add(u, 1);
			// number of instances of item i assigned to topic t
			Nik.add(i, t, 1);
			// total number of words assigned to topic t.
			Nk.add(t, 1);
		}
	}

	protected void inferParams() {

		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();

		// Gibbs sampling from full conditional distribution
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			int t = z.get(u, i); // topic

			Nik.add(i, t, -1);
			Nuk.add(u, t, -1);
			Nk.add(t, -1);
			Nu.add(u, -1);

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			for (int k = 0; k < numFactors; k++) {
				p[k] = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha) * (Nik.get(i, k) + beta.get(i))
						/ (Nk.get(k) + sumBeta);
			}
			// cumulating multinomial parameters
			for (int k = 1; k < p.length; k++) {
				p[k] += p[k - 1];
			}
			// scaled sample because of unnormalized p[], randomly sampled a new topic t
			double rand = Math.random() * p[numFactors - 1];
			for (t = 0; t < p.length; t++) {
				if (rand < p[t])
					break;
			}

			// add newly estimated z_i to count variables
			Nik.add(i, t, 1);
			Nuk.add(u, t, 1);
			Nk.add(t, 1);
			Nu.add(u, 1);

			z.put(u, i, t);
		}

	}

	@Override
	protected void updateHyperParams() {
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();
		double ak, bi;

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
	}

	/**
	 * Add to the statistics the values of theta and phi for the current state.
	 */
	protected void readoutParams() {
		double sumAlpha = alpha.sum();
		double sumBeta = beta.sum();

		double val = 0;
		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nuk.get(u, k) + alpha.get(k)) / (Nu.get(u) + sumAlpha);
				thetaSum.add(u, k, val);
			}
		}

		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				val = (Nik.get(i, k) + beta.get(i)) / (Nk.get(k) + sumBeta);
				phiSum.add(k, i, val);
			}
		}
		numStats++;
	}

	@Override
	protected void postProbDistr() {
		theta = thetaSum.scale(1.0 / numStats);
		phi = phiSum.scale(1.0 / numStats);
	}

	@Override
	protected double ranking(int u, int j) throws Exception {

		return DenseMatrix.product(theta, u, phi, j);
	}

}
