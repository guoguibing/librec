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

package librec.rating;

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Randoms;
import librec.util.Strings;

/**
 * Salakhutdinov and Mnih, <strong>Bayesian Probabilistic Matrix Factorization using Markov Chain Monte Carlo</strong>,
 * ICML 2008.
 * 
 * <p>
 * Matlab version is provided by the authors via <a href="http://www.utstat.toronto.edu/~rsalakhu/BPMF.html">this
 * link</a>. This implementation is modified from the BayesianPMF by the PREA package.
 * </p>
 * 
 * @author guoguibing
 * 
 */
@Configuration("factors, iters")
public class BPMF extends IterativeRecommender {

	public BPMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		lRate = -1;
	}

	@Override
	protected void buildModel() throws Exception {

		// Initialize hierarchical priors
		int beta = 2; // observation noise (precision)
		DenseVector mu_u = new DenseVector(numFactors);
		DenseVector mu_m = new DenseVector(numFactors);

		// parameters of Inv-Whishart distribution
		DenseMatrix WI_u = DenseMatrix.eye(numFactors);
		int b0_u = 2;
		int df_u = numFactors;
		DenseVector mu0_u = new DenseVector(numFactors);

		DenseMatrix WI_m = DenseMatrix.eye(numFactors);
		int b0_m = 2;
		int df_m = numFactors;
		DenseVector mu0_m = new DenseVector(numFactors);

		// initializing Bayesian PMF using MAP solution found by PMF
		P = new DenseMatrix(numUsers, numFactors);
		Q = new DenseMatrix(numItems, numFactors);

		P.init(0, 1);
		Q.init(0, 1);

		for (int f = 0; f < numFactors; f++) {
			mu_u.set(f, P.columnMean(f));
			mu_m.set(f, Q.columnMean(f));
		}

		DenseMatrix alpha_u = P.cov().inv();
		DenseMatrix alpha_m = Q.cov().inv();

		// Iteration:
		DenseVector x_bar = new DenseVector(numFactors);
		DenseVector normalRdn = new DenseVector(numFactors);

		DenseMatrix S_bar, WI_post, lam;
		DenseVector mu_temp;
		double df_upost, df_mpost;

		int M = numUsers, N = numItems;

		for (int iter = 1; iter <= numIters; iter++) {

			// Sample from user hyper parameters:
			for (int f = 0; f < numFactors; f++)
				x_bar.set(f, P.columnMean(f));
			S_bar = P.cov();

			DenseVector mu0_u_x_bar = mu0_u.minus(x_bar);
			DenseMatrix e1e2 = mu0_u_x_bar.outer(mu0_u_x_bar).scale(M * b0_u / (b0_u + M + 0.0));
			WI_post = WI_u.inv().add(S_bar.scale(M)).add(e1e2);
			WI_post = WI_post.inv();
			WI_post = WI_post.add(WI_post.transpose()).scale(0.5);

			df_upost = df_u + M;
			DenseMatrix wishrnd_u = wishart(WI_post, df_upost);
			if (wishrnd_u != null)
				alpha_u = wishrnd_u;
			mu_temp = mu0_u.scale(b0_u).add(x_bar.scale(M)).scale(1 / (b0_u + M + 0.0));
			lam = alpha_u.scale(b0_u + M).inv().cholesky();

			if (lam != null) {
				lam = lam.transpose();

				for (int f = 0; f < numFactors; f++)
					normalRdn.set(f, Randoms.gaussian(0, 1));

				mu_u = lam.mult(normalRdn).add(mu_temp);
			}

			// Sample from item hyper parameters:
			for (int f = 0; f < numFactors; f++)
				x_bar.set(f, Q.columnMean(f));
			S_bar = Q.cov();

			DenseVector mu0_m_x_bar = mu0_m.minus(x_bar);
			DenseMatrix e3e4 = mu0_m_x_bar.outer(mu0_m_x_bar).scale(N * b0_m / (b0_m + N + 0.0));
			WI_post = WI_m.inv().add(S_bar.scale(N)).add(e3e4);
			WI_post = WI_post.inv();
			WI_post = WI_post.add(WI_post.transpose()).scale(0.5);

			df_mpost = df_m + N;
			DenseMatrix wishrnd_m = wishart(WI_post, df_mpost);
			if (wishrnd_m != null)
				alpha_m = wishrnd_m;
			mu_temp = mu0_m.scale(b0_m).add(x_bar.scale(N)).scale(1 / (b0_m + N + 0.0));
			lam = alpha_m.scale(b0_m + N).inv().cholesky();

			if (lam != null) {
				lam = lam.transpose();

				for (int f = 0; f < numFactors; f++)
					normalRdn.set(f, Randoms.gaussian(0, 1));

				mu_m = lam.mult(normalRdn).add(mu_temp);
			}

			// Gibbs updates over user and item feature vectors given hyper parameters:
			// NOTE: in PREA, only 1 iter for gibbs where in the original Matlab code, 2 iters are used.
			for (int gibbs = 0; gibbs < 2; gibbs++) {
				// Infer posterior distribution over all user feature vectors
				for (int u = 0; u < numUsers; u++) {
					// list of items rated by user uu:
					SparseVector rv = trainMatrix.row(u);
					int count = rv.getCount();

					if (count == 0)
						continue;

					// features of items rated by user uu:
					DenseMatrix MM = new DenseMatrix(count, numFactors);
					DenseVector rr = new DenseVector(count);
					int idx = 0;
					for (int j : rv.getIndex()) {
						rr.set(idx, rv.get(j) - globalMean);
						for (int f = 0; f < numFactors; f++)
							MM.set(idx, f, Q.get(j, f));

						idx++;
					}

					DenseMatrix covar = alpha_u.add((MM.transpose().mult(MM)).scale(beta)).inv();
					DenseVector a = MM.transpose().mult(rr).scale(beta);
					DenseVector b = alpha_u.mult(mu_u);
					DenseVector mean_u = covar.mult(a.add(b));
					lam = covar.cholesky();

					if (lam != null) {
						lam = lam.transpose();
						for (int f = 0; f < numFactors; f++)
							normalRdn.set(f, Randoms.gaussian(0, 1));

						DenseVector w1_P1_u = lam.mult(normalRdn).add(mean_u);

						for (int f = 0; f < numFactors; f++)
							P.set(u, f, w1_P1_u.get(f));
					}
				}

				// Infer posterior distribution over all movie feature vectors
				for (int j = 0; j < numItems; j++) {
					// list of users who rated item ii:
					SparseVector jv = trainMatrix.column(j);
					int count = jv.getCount();
					if (count == 0)
						continue;

					// features of users who rated item ii:
					DenseMatrix MM = new DenseMatrix(count, numFactors);
					DenseVector rr = new DenseVector(count);
					int idx = 0;
					for (int u : jv.getIndex()) {
						rr.set(idx, jv.get(u) - globalMean);
						for (int f = 0; f < numFactors; f++)
							MM.set(idx, f, P.get(u, f));

						idx++;
					}

					DenseMatrix covar = alpha_m.add((MM.transpose().mult(MM)).scale(beta)).inv();
					DenseVector a = MM.transpose().mult(rr).scale(beta);
					DenseVector b = alpha_m.mult(mu_m);
					DenseVector mean_m = covar.mult(a.add(b));
					lam = covar.cholesky();

					if (lam != null) {
						lam = lam.transpose();
						for (int f = 0; f < numFactors; f++)
							normalRdn.set(f, Randoms.gaussian(0, 1));

						DenseVector w1_M1_j = lam.mult(normalRdn).add(mean_m);

						for (int f = 0; f < numFactors; f++)
							Q.set(j, f, w1_M1_j.get(f));
					}
				}
			} // end of gibbs

			loss = 0;
			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();
				double pred = predict(u, j);
				double euj = ruj - pred;

				loss += euj * euj;
			}
			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * Randomly sample a matrix from Wishart Distribution with the given parameters.
	 * 
	 * @param scale
	 *            scale parameter for Wishart Distribution.
	 * @param df
	 *            degree of freedom for Wishart Distribution.
	 * @return the sample randomly drawn from the given distribution.
	 */
	protected DenseMatrix wishart(DenseMatrix scale, double df) {
		DenseMatrix A = scale.cholesky();
		if (A == null)
			return null;

		int p = scale.numRows();
		DenseMatrix z = new DenseMatrix(p, p);

		for (int i = 0; i < p; i++) {
			for (int j = 0; j < p; j++) {
				z.set(i, j, Randoms.gaussian(0, 1));
			}
		}

		SparseVector y = new SparseVector(p);
		for (int i = 0; i < p; i++)
			y.set(i, Randoms.gamma((df - (i + 1)) / 2, 2));

		DenseMatrix B = new DenseMatrix(p, p);
		B.set(0, 0, y.get(0));

		if (p > 1) {
			// rest of diagonal:
			for (int j = 1; j < p; j++) {
				SparseVector zz = new SparseVector(j);
				for (int k = 0; k < j; k++)
					zz.set(k, z.get(k, j));

				B.set(j, j, y.get(j) + zz.inner(zz));
			}

			// first row and column:
			for (int j = 1; j < p; j++) {
				B.set(0, j, z.get(0, j) * Math.sqrt(y.get(0)));
				B.set(j, 0, B.get(0, j)); // mirror
			}
		}

		if (p > 2) {
			for (int j = 2; j < p; j++) {
				for (int i = 1; i <= j - 1; i++) {
					SparseVector zki = new SparseVector(i);
					SparseVector zkj = new SparseVector(i);

					for (int k = 0; k <= i - 1; k++) {
						zki.set(k, z.get(k, i));
						zkj.set(k, z.get(k, j));
					}
					B.set(i, j, z.get(i, j) * Math.sqrt(y.get(i)) + zki.inner(zkj));
					B.set(j, i, B.get(i, j)); // mirror
				}
			}
		}

		return A.transpose().mult(B).mult(A);
	}

	@Override
	protected double predict(int u, int j) {
		return globalMean + DenseMatrix.rowMult(P, u, Q, j);
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, numIters });
	}
}
