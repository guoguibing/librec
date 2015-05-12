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
package librec.baseline;

import happy.coding.io.Logs;
import happy.coding.math.Randoms;

import java.math.BigDecimal;
import java.math.RoundingMode;

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.GraphicRecommender;

/**
 * It is a graphical model that clusters items into K groups for recommendation, as opposite to the {@code UserCluster} recommender.
 * 
 * @author Guo Guibing
 *
 */
@Configuration("factors, max.iters")
public class ItemCluster extends GraphicRecommender {

	private DenseMatrix Pkr; // theta
	private DenseVector Pi; // pi

	private DenseMatrix Gamma;
	private DenseMatrix Nir;
	private DenseVector Ni;

	public ItemCluster(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {

		Pkr = new DenseMatrix(numFactors, numLevels); // k x r
		for (int k = 0; k < numFactors; k++) {
			// random probs 
			double[] probs = Randoms.randProbs(numLevels);
			for (int r = 0; r < numLevels; r++)
				Pkr.set(k, r, probs[r]);
		}

		Pi = new DenseVector(Randoms.randProbs(numFactors));

		Gamma = new DenseMatrix(numItems, numFactors); // i x k

		// pre-computing Nir, Ni
		Nir = new DenseMatrix(numItems, numLevels); // Nir
		Ni = new DenseVector(numItems); // Ni

		for (int i = 0; i < numItems; i++) {
			SparseVector ri = trainMatrix.column(i);

			for (VectorEntry ve : ri) {
				double rui = ve.get();
				int r = ratingScale.indexOf(rui);

				Nir.add(i, r, 1);
			}

			Ni.set(i, ri.size());
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {

			// e-step: compute Gamma_ik = P(Zi_k | ri, theta)
			for (int i = 0; i < numItems; i++) {
				BigDecimal sum_i = BigDecimal.ZERO;
				SparseVector ri = trainMatrix.column(i);

				BigDecimal[] sum_ik = new BigDecimal[numFactors];
				for (int k = 0; k < numFactors; k++) {
					BigDecimal pik = new BigDecimal(Pi.get(k));

					for (VectorEntry ve : ri) {
						double rui = ve.get();
						int r = ratingScale.indexOf(rui);
						BigDecimal pkr = new BigDecimal(Pkr.get(k, r));

						pik = pik.multiply(pkr);
					}

					sum_ik[k] = pik;
					sum_i = sum_i.add(pik);
				}

				for (int k = 0; k < numFactors; k++) {
					double zik = sum_ik[k].divide(sum_i, 6, RoundingMode.HALF_UP).doubleValue();
					Gamma.set(i, k, zik);
				}
			}

			// m-step: update Pkr, Pi
			double sum_ik[] = new double[numFactors];
			double sum = 0;
			for (int k = 0; k < numFactors; k++) {

				for (int r = 0; r < numLevels; r++) {

					double numerator = 0, denorminator = 0;
					for (int i = 0; i < numItems; i++) {
						double ruk = Gamma.get(i, k);

						numerator += ruk * Nir.get(i, r);
						denorminator += ruk * Ni.get(i);
					}

					Pkr.set(k, r, numerator / denorminator);
				}

				double sum_i = 0;
				for (int i = 0; i < numItems; i++) {
					double rik = Gamma.get(i, k);
					sum_i += rik;
				}

				sum_ik[k] = sum_i;
				sum += sum_i;
			}

			for (int k = 0; k < numFactors; k++) {
				Pi.set(k, sum_ik[k] / sum);
			}

			// compute loss value
			loss = 0;
			for (int i = 0; i < numItems; i++) {
				for (int k = 0; k < numFactors; k++) {
					double rik = Gamma.get(i, k);
					double pi_k = Pi.get(k);

					double sum_nl = 0;
					for (int r = 0; r < numLevels; r++) {
						double nur = Nir.get(i, r);
						double pkr = Pkr.get(k, r);

						sum_nl += nur * Math.log(pkr);
					}

					loss += rik * (Math.log(pi_k) + sum_nl);
				}
			}

			loss = -loss; // turn to minimization problem instead

			if (isConverged(iter))
				break;
		}
	}
	
	@Override
	protected boolean isConverged(int iter) throws Exception {
		float deltaLoss = (float) (loss - lastLoss);

		Logs.debug("{}{} iter {} achives loss = {}, delta_loss = {}", algoName, foldInfo, iter, (float) loss, deltaLoss);

		if (iter > 1 && (deltaLoss > 0 || Double.isNaN(deltaLoss))) {
			Logs.debug("{}{} converges at iter {}", algoName, foldInfo, iter);
			return true;
		}

		lastLoss = loss;
		return false;
	}

	@Override
	protected double predict(int u, int j, boolean bound) throws Exception {
		double pred = 0;

		for (int k = 0; k < numFactors; k++) {
			double pj_k = Gamma.get(j, k); // probability that item j belongs to cluster k
			double pred_k = 0;

			for (int r = 0; r < numLevels; r++) {
				double rui = ratingScale.get(r);
				double pkr = Pkr.get(k, r);

				pred_k += rui * pkr;
			}

			pred += pj_k * pred_k;
		}

		return pred;
	}

	@Override
	public String toString() {
		return numFactors + "," + numIters;
	}
}
