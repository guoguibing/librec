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

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;

/**
 * Yehuda Koren, <strong>Factorization Meets the Neighborhood: a Multifaceted Collaborative Filtering Model.</strong>,
 * KDD 2008.
 * 
 * @author guoguibing
 * 
 */
public class SVDPlusPlus extends BiasedMF {

	protected DenseMatrix Y;

	public SVDPlusPlus(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SVD++";
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		Y = new DenseMatrix(numItems, numFactors);
		Y.init(initMean, initStd);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			errs = 0;
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int j = me.column(); // item

				double ruj = me.get();
				if (ruj <= 0.0)
					continue;

				double pred = predict(u, j);
				double euj = ruj - pred;

				errs += euj * euj;
				loss += euj * euj;

				SparseVector uv = trainMatrix.row(u);
				int[] items = uv.getIndex();
				double w = Math.sqrt(items.length);

				// update factors
				double bu = userBiases.get(u);
				double sgd = euj - regB * bu;
				userBiases.add(u, lRate * sgd);

				loss += regB * bu * bu;

				double bj = itemBiases.get(j);
				sgd = euj - regB * bj;
				itemBiases.add(j, lRate * sgd);

				loss += regB * bj * bj;

				double[] sum_ys = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum_f = 0;
					for (int k : items)
						sum_f += Y.get(k, f);

					sum_ys[f] = w > 0 ? sum_f / w : sum_f;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double sgd_u = euj * qjf - regU * puf;
					double sgd_j = euj * (puf + sum_ys[f]) - regI * qjf;

					P.add(u, f, lRate * sgd_u);
					Q.add(j, f, lRate * sgd_j);

					loss += regU * puf * puf + regI * qjf * qjf;

					for (int k : items) {
						double ykf = Y.get(k, f);
						double delta_y = euj * qjf / w - regU * ykf;
						Y.add(k, f, lRate * delta_y);

						loss += regU * ykf * ykf;
					}
				}

			}

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training

	}

	@Override
	protected double predict(int u, int j) {
		double pred = globalMean + userBiases.get(u) + itemBiases.get(j) + DenseMatrix.rowMult(P, u, Q, j);

		SparseVector uv = trainMatrix.row(u);
		double w = Math.sqrt(uv.getCount());
		for (int k : uv.getIndex())
			pred += DenseMatrix.rowMult(Y, k, Q, j) / w;

		return pred;
	}
}
