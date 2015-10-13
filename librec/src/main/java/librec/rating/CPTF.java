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

import librec.data.DenseMatrix;
import librec.data.SparseMatrix;
import librec.data.TensorEntry;
import librec.intf.TensorRecommender;

/**
 * CANDECOMP/PARAFAC (CP) Tensor Factorization <br>
 * 
 * Shao W., <strong>Tensor Completion</strong> (Section 3.2), Saarland University.
 * 
 * @author Guo Guibing
 *
 */
public class CPTF extends TensorRecommender {

	// dimension-feature matrices
	private DenseMatrix[] M;

	public CPTF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		M = new DenseMatrix[numDimensions];

		for (int d = 0; d < numDimensions; d++) {
			M[d] = new DenseMatrix(dimensions[d], numFactors);
			M[d].init(smallValue); // randomly initialization

			normalize(d);
		}
	}

	protected void normalize(int d) {

		// column-wise normalization
		for (int f = 0; f < numFactors; f++) {

			double norm = 0;
			for (int r = 0; r < M[d].numRows(); r++) {
				norm += Math.pow(M[d].get(r, f), 2);
			}
			norm = Math.sqrt(norm);

			for (int r = 0; r < M[d].numRows(); r++) {
				M[d].set(r, f, M[d].get(r, f) / norm);
			}
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter < numIters; iter++) {

			DenseMatrix[] Ms = new DenseMatrix[numDimensions];
			for (int d = 0; d < numDimensions; d++) {
				Ms[d] = new DenseMatrix(dimensions[d], numFactors);
			}

			// SGD Optimization

			loss = 0;
			// Step 1: compute gradients
			for (TensorEntry te : trainTensor) {
				int[] keys = te.keys();
				double rate = te.get();
				if (rate <= 0)
					continue;

				double pred = predict(keys);
				double e = rate - pred;

				loss += e * e;

				// compute gradients 
				for (int d = 0; d < numDimensions; d++) {

					for (int f = 0; f < numFactors; f++) {

						// multiplication of other dimensions
						double sgd = 1;
						for (int dd = 0; dd < numDimensions; dd++) {
							if (dd == d)
								continue;

							sgd *= M[dd].get(keys[dd], f);
						}

						Ms[d].add(keys[d], f, sgd * e);
					}
				}
			}

			// Step 2: update variables
			for (int d = 0; d < numDimensions; d++) {

				// update each M[d](r, c)
				for (int r = 0; r < M[d].numRows(); r++) {
					for (int c = 0; c < M[d].numColumns(); c++) {
						double Mrc = M[d].get(r, c);
						M[d].add(r, c, lRate * (Ms[d].get(r, c) - reg * Mrc));

						loss += reg * Mrc * Mrc;
					}
				}
			}

			loss *= 0.5;
			if (isConverged(iter))
				break;
		}
	}

	protected double predict(int[] keys) {
		double pred = 0;

		for (int f = 0; f < numFactors; f++) {

			double prod = 1;
			for (int d = 0; d < numDimensions; d++) {
				prod *= M[d].get(keys[d], f);
			}

			pred += prod;
		}

		return pred;
	}
}
