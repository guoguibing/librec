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
		for (int d = 0; d < numDimensions; d++) {
			M[d] = new DenseMatrix(dimensions[d], numFactors);
			M[d].init(smallValue); // randomly initialization
		}

		lambda = new DenseVector(numFactors);

		// normalize M
		for (int d = 0; d < numDimensions; d++) {
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

	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter < numIters; iter++) {

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
				for (int f = 0; f < numFactors; f++) {

					double norm = 0;
					for (int r = 0; r < M[d].numRows(); r++) {
						norm += Math.pow(M[d].get(r, f), 2);
					}
					norm = Math.sqrt(norm);
					lambda.set(f, norm);

					for (int r = 0; r < M[d].numRows(); r++) {
						M[d].set(r, f, M[d].get(r, f) / norm);
					}
				}
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
