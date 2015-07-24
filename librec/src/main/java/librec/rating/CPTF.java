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
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.TensorEntry;
import librec.intf.TensorRecommender;

/**
 * CANDECOMP/PARAFAC (CP) Tensor Factorization <br>
 * 
 * Tamara G. Kolda and Brett W. Bader, <strong>Tensor Decompositions and Applications</strong> (Section 3.4), SIAM
 * Review, 2009.
 * 
 * @author Guo Guibing
 *
 */
public class CPTF extends TensorRecommender {

	// dimension-feature matrices
	private DenseMatrix[] M;
	// scaling factors
	private DenseVector lambda;

	public CPTF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		M = new DenseMatrix[numDimensions];

		lambda = new DenseVector(numFactors);
		lambda.setAll(1.0);

		for (int d = 0; d < numDimensions; d++) {
			M[d] = new DenseMatrix(dimensions[d], numFactors);
			M[d].init(smallValue); // randomly initialization

			normalize(d);
		}

		// no need to update learning rate
		lRate = 0;
	}

	private void normalize(int d) {

		// column-wise normalization
		for (int f = 0; f < numFactors; f++) {

			double norm = 0;
			for (int r = 0; r < M[d].numRows(); r++) {
				norm += Math.pow(M[d].get(r, f), 2);
			}
			norm = Math.sqrt(norm);
			lambda.set(f, lambda.get(f) * norm);

			for (int r = 0; r < M[d].numRows(); r++) {
				M[d].set(r, f, M[d].get(r, f) / norm);
			}
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter < numIters; iter++) {

			lambda.setAll(1.0);

			// ALS Optimization
			for (int d = 0; d < numDimensions; d++) {

				// Step 1: compute V
				DenseMatrix V = new DenseMatrix(numFactors, numFactors);
				V.setAll(1.0);

				for (int dim = 0; dim < numDimensions; dim++) {
					if (dim != d) {
						V = DenseMatrix.hadamardProduct(V, M[dim].transMult());
					}
				}

				V = V.pinv(); // pseudo inverse

				// Step 2: update M[d]
				SparseMatrix X = trainTensor.matricization(d);

				DenseMatrix A = null;
				for (int dim = numDimensions - 1; dim >= 0; dim--) {
					if (dim != d) {
						A = A == null ? M[dim] : DenseMatrix.khatriRaoProduct(A, M[dim]);
					}
				}

				M[d] = DenseMatrix.mult(X, A).mult(V);

				// Step 3: normalize columns of M[d]
				normalize(d);
			}

			// compute loss value
			loss = 0;
			for (TensorEntry te : trainTensor) {
				int[] keys = te.keys();
				double rate = te.get();

				double pred = predict(keys);
				double e = pred - rate;

				loss += e * e;
			}

			loss *= 0.5;
			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) throws Exception {
		int index = testTensor.getIndices(u, j).get(0);

		return predict(testTensor.keys(index));
	}

	private double predict(int[] keys) {
		double pred = 0;
		for (int f = 0; f < numFactors; f++) {
			double prod = lambda.get(f);
			for (int d = 0; d < numDimensions; d++) {
				prod *= M[d].get(keys[d], f);
			}

			pred += prod;
		}

		return pred;
	}
}
