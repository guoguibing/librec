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

import happy.coding.io.Strings;
import librec.data.DenseMatrix;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.SocialRecommender;

/**
 * Hao Ma, Irwin King and Michael R. Lyu, <strong>Learning to Recommend with Social Trust Ensemble</strong>, SIGIR 2009.<br>
 * 
 * <p>
 * This method is quite time-consuming when dealing with the social influence part.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class RSTE extends SocialRecommender {

	private float alpha;

	public RSTE(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		initByNorm = false;
		alpha = algoOptions.getFloat("-alpha");
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			// ratings
			for (int u : trainMatrix.rows()) {

				SparseVector tu = socialMatrix.row(u);
				int[] tks = tu.getIndex();

				double ws = 0;
				for (int k : tks)
					ws += tu.get(k);

				double[] sum_us = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					for (int k : tks)
						sum_us[f] += tu.get(k) * P.get(k, f);
				}

				for (VectorEntry ve : trainMatrix.row(u)) {

					int j = ve.index();
					double rate = ve.get();
					double ruj = normalize(rate);

					// compute directly to speed up calculation
					double pred1 = DenseMatrix.rowMult(P, u, Q, j);
					double sum = 0.0;
					for (int k : tks)
						sum += tu.get(k) * DenseMatrix.rowMult(P, k, Q, j);

					double pred2 = ws > 0 ? sum / ws : 0;
					double pred = alpha * pred1 + (1 - alpha) * pred2;

					// prediction error
					double euj = g(pred) - ruj;

					loss += euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qjf = Q.get(j, f);

						double usgd = alpha * csgd * qjf + regU * puf;

						double jd = ws > 0 ? sum_us[f] / ws : 0;
						double jsgd = csgd * (alpha * puf + (1 - alpha) * jd) + regI * qjf;

						PS.add(u, f, usgd);
						QS.add(j, f, jsgd);

						loss += regU * puf * puf + regI * qjf * qjf;
					}
				}
			}

			// social
			for (int u : socialMatrix.columns()) {

				SparseVector bu = socialMatrix.column(u);
				for (int p : bu.getIndex()) {
					if (p >= trainMatrix.numRows())
						continue;

					SparseVector pp = trainMatrix.row(p);
					SparseVector tp = socialMatrix.row(p);
					int[] tps = tp.getIndex();

					for (int j : pp.getIndex()) {

						// compute prediction for user-item (p, j)
						double pred1 = DenseMatrix.rowMult(P, p, Q, j);
						double sum = 0.0, ws = 0.0;
						for (int k : tps) {
							double tuk = tp.get(k);
							sum += tuk * DenseMatrix.rowMult(P, k, Q, j);
							ws += tuk;
						}
						double pred2 = ws > 0 ? sum / ws : 0;
						double pred = alpha * pred1 + (1 - alpha) * pred2;

						// double pred = predict(p, j, false);
						double epj = g(pred) - normalize(pp.get(j));
						double csgd = gd(pred) * epj * bu.get(p);

						for (int f = 0; f < numFactors; f++)
							PS.add(u, f, (1 - alpha) * csgd * Q.get(j, f));
					}
				}
			}

			loss *= 0.5;

			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j, boolean bound) {
		double pred1 = DenseMatrix.rowMult(P, u, Q, j);
		double sum = 0.0, ws = 0.0;
		SparseVector tu = socialMatrix.row(u);

		for (int k : tu.getIndex()) {
			double tuk = tu.get(k);
			sum += tuk * DenseMatrix.rowMult(P, k, Q, j);
			ws += tuk;
		}

		double pred2 = ws > 0 ? sum / ws : 0;

		double pred = alpha * pred1 + (1 - alpha) * pred2;

		if (bound)
			return denormalize(g(pred));

		return pred;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { initLRate, maxLRate, regU, regI, numFactors, numIters, isBoldDriver,
				alpha }, ",");
	}
}
