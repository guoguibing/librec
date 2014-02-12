package librec.ext;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends IterativeRecommender {

	protected DenseMatrix W, H;
	protected SparseMatrix V;

	public NMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";
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

			// update H
			DenseMatrix trW = W.transpose();

			// trW * V
			DenseMatrix trW_V = new DenseMatrix(trW.numRows(), V.numColumns());
			for (int i = 0; i < trW_V.numRows(); i++) {
				DenseVector row = trW.row(i);
				for (int j = 0; j < trW_V.numCols(); j++) {
					SparseVector col = V.column(j);

					trW_V.set(i, j, row.inner(col));
				}
			}

			// trW * W * H
			DenseMatrix trW_W_H = trW.mult(W).mult(H);

			// update
			for (int i = 0; i < H.numRows(); i++)
				for (int j = 0; j < H.numCols(); j++)
					H.set(i, j, H.get(i, j) * (trW_V.get(i, j) / trW_W_H.get(i, j)));

			// update W
			DenseMatrix trH = H.transpose();

			// V * trH
			DenseMatrix V_trH = new DenseMatrix(V.numRows(), trH.numCols());
			for (int i = 0; i < V_trH.numRows(); i++) {
				SparseVector row = V.row(i);
				for (int j = 0; j < V_trH.numCols(); j++) {
					DenseVector col = trH.column(j);

					V_trH.set(i, j, col.inner(row));
				}
			}

			// W * H * trH
			DenseMatrix W_H_trH = W.mult(H).mult(trH);

			// update
			for (int i = 0; i < W.numRows(); i++)
				for (int j = 0; j < W.numCols(); j++)
					W.set(i, j, W.get(i, j) * (V_trH.get(i, j) / W_H_trH.get(i, j)));

			// compute errors
			DenseMatrix predV = W.mult(H);

			for (MatrixEntry me : V) {
				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				if (ruj <= 0)
					continue;

				double pred = predV.get(u, j);
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
		return W.row(u).inner(H.column(j));
	}
}
