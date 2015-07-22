package librec.rating;

import librec.data.DenseMatrix;
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

	private DenseMatrix[] M;

	public CPTF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		M = new DenseMatrix[numDimensions];
		for (int d = 0; d < numDimensions; d++) {
			M[d] = new DenseMatrix(dimensions[d], numFactors);
			M[d].init(smallValue);
		}
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter < numIters; iter++) {

			loss = 0;

			// CP-ALS
			for (int dim = 0; dim < numDimensions; dim++) {

				for (TensorEntry te : trainTensor) {
					double rate = te.get();
					double pred = predict(te.keys());

					double e = pred - rate;
					loss += e * e;

					int j = te.key(dim);

					for (int f = 0; f < numFactors; f++) {

						double grad = 1;
						for (int d = 0; d < numDimensions; d++) {
							if (d != dim) {
								grad *= M[d].get(te.key(d), f);
							}
						}

						double Md_jf = M[dim].get(j, f);
						double sgd = e * grad + reg * Md_jf;
						M[dim].add(j, f, -lRate * sgd);

						loss += reg * Md_jf * Md_jf;
					}

				}
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
			double prod = 1;
			for (int d = 0; d < numDimensions; d++) {
				prod *= M[d].get(keys[d], f);
			}

			pred += prod;
		}

		return pred;
	}
}
