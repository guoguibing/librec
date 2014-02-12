package librec.ext;

import happy.coding.io.Strings;
import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends IterativeRecommender {

	// V = W * H
	protected DenseMatrix W, H;
	protected SparseMatrix V;

	public NMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";

		// no need to update learning rate
		lRate = -1;
	}

	@Override
	protected void initModel() {
		W = new DenseMatrix(numUsers, numFactors);
		H = new DenseMatrix(numFactors, numItems);

		W.init();
		H.init();

		V = trainMatrix;
	}

	@Override
	protected void buildModel() {
		for (int iter = 1; iter < maxIters; iter++) {
			loss = 0;
			errs = 0;

			// transpose of W
			DenseMatrix trW = W.transpose();

			// trW * V
			DenseMatrix trW_V = trW.mult(V);

			// trW * W * H
			DenseMatrix trW_W_H = trW.mult(W).mult(H);

			// transpose of H
			DenseMatrix trH = H.transpose();

			// V * trH
			DenseMatrix V_trH = DenseMatrix.mult(V, trH);

			// W * H * trH
			DenseMatrix W_H_trH = W.mult(H.mult(trH));

			// update: H_ij = H_ij * (trW_V)_ij / (trW_W_H)_ij
			for (int i = 0; i < H.numRows(); i++)
				for (int j = 0; j < H.numCols(); j++) {
					double denorm = trW_W_H.get(i, j) + 1e-9;
					H.set(i, j, H.get(i, j) * (trW_V.get(i, j) / denorm));
				}

			// update: W_ij = W_ij * (V_trH)_ij / (W_H_trH)_ij
			for (int i = 0; i < W.numRows(); i++)
				for (int j = 0; j < W.numCols(); j++) {
					double denorm = W_H_trH.get(i, j) + 1e-9;
					W.set(i, j, W.get(i, j) * (V_trH.get(i, j) / denorm));
				}

			// compute errors
			for (MatrixEntry me : V) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				if (ruj <= 0)
					continue;

				double pred = predict(u, j);
				double euj = pred - ruj;

				errs += euj * euj;
				loss += euj * euj;
			}

			loss += regU * Math.pow(W.norm(), 2) + regI * Math.pow(H.norm(), 2);

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	@Override
	protected double predict(int u, int j) {
		return DenseMatrix.product(W, u, H, j);
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { regU, regI, numFactors,
				maxIters, isBoldDriver }, ",");
	}
}
