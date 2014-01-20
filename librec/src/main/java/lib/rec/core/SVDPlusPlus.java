package lib.rec.core;

import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;
import lib.rec.data.SparseVec;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Yehuda Koren, <strong>Factorization Meets the Neighborhood: a Multifaceted
 * Collaborative Filtering Model.</strong>, KDD 2008.
 * 
 * @author guoguibing
 * 
 */
public class SVDPlusPlus extends BiasedMF {

	protected DenseMat Y;

	public SVDPlusPlus(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SVD++";
	}

	@Override
	public void initModel() {
		super.initModel();

		Y = new DenseMat(numItems, numFactors);
		Y.init(initMean, initStd);

		// set factors to zero for items without training examples
		int numCols = trainMatrix.numColumns();
		for (int j = 0; j < numItems; j++) {
			if (j >= numCols || trainMatrix.colSize(j) == 0)
				Y.setRow(j, 0.0);
		}
	}

	@Override
	public void buildModel() {

		for (int iter = 1; iter <= maxIters; iter++) {

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

				SparseVec uv = trainMatrix.row(u);
				int[] items = uv.getIndex();
				double w = Math.sqrt(items.length);

				// update factors
				double bu = userBiases.get(u);
				double sgd = euj - regU * bu;
				userBiases.add(u, lRate * sgd);

				loss += regU * bu * bu;

				double bj = itemBiases.get(j);
				sgd = euj - regI * bj;
				itemBiases.add(j, lRate * sgd);

				loss += regI * bj * bj;

				double[] sum_ys = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum_f = 0;
					for (int k : items)
						sum_f += Y.get(k, f) / w;

					sum_ys[f] = sum_f;
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

			if (postEachIter(iter))
				break;

		}// end of training

	}

	@Override
	protected double predict(int u, int j) {
		double pred = globalMean + userBiases.get(u) + itemBiases.get(j) + DenseMat.rowMult(P, u, Q, j);

		SparseVector uv = trainMatrix.row(u);
		double w = Math.sqrt(uv.getUsed());
		for (int k : uv.getIndex())
			pred += DenseMat.rowMult(Y, k, Q, j) / w;

		return pred;
	}
}
