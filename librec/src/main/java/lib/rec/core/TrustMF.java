package lib.rec.core;

import happy.coding.system.Debug;
import lib.rec.data.DenseMatrix;
import lib.rec.data.DenseVector;
import lib.rec.data.SparseMatrix;
import lib.rec.data.SparseVector;
import lib.rec.intf.SocialRecommender;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI
 * 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialRecommender {

	protected DenseMatrix B, W, V1, V2;
	protected String model;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "TrustMF";

		if (Debug.ON) {
			model = "Tr";
			regU = 0.001;
			regI = 0.001;
			regS = 1;
		}
	}

	@Override
	protected void initModel() {

		V1 = new DenseMatrix(numItems, numFactors);
		V2 = new DenseMatrix(numItems, numFactors);

		V1.init(initMean, initStd);
		V2.init(initMean, initStd);

		for (int j = 0; j < numItems; j++)
			if (trainMatrix.colSize(j) == 0) {
				V1.setRow(j, 0.0);
				V2.setRow(j, 0.0);
			}

		B = new DenseMatrix(numUsers, numFactors);
		W = new DenseMatrix(numUsers, numFactors);

		B.init(initMean, initStd);
		W.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0)
				B.setRow(u, 0.0);

			if (socialMatrix.colSize(u) == 0)
				W.setRow(u, 0.0);
		}
	}

	@Override
	protected void buildModel() {
		switch (model) {
		default:
		case "Tr":
			TrusterMF();
			break;
		case "Te":
			TrusteeMF();
			break;
		case "T":
			TrusterMF();
			TrusteeMF();
			break;
		}
	}

	/**
	 * Build TrusterMF model: B*V1
	 */
	protected void TrusterMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			loss = 0;
			errs = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix VS = new DenseMatrix(numItems, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);

			// update B
			for (int u = 0; u < numUsers; u++) {

				// rated items
				SparseVector rv = trainMatrix.row(u);
				for (int j : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(j);

					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					loss += euj * euj;
					errs += euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						BS.add(u, f, csgd * V1.get(j, f));
				}

				// trusted users
				SparseVector tv = socialMatrix.row(u);
				for (int k : tv.getIndex()) {
					double tuk = tv.get(k);
					double pred = DenseMatrix.rowMult(B, u, W, k);
					double euj = g(pred) - tuk;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						BS.add(u, f, regS * csgd * W.get(k, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double buf = B.get(u, f);
					BS.add(u, f, regU * buf);

					loss += regU * buf * buf;
				}
			}

			// update V
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.col(j);
				for (int u : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(u);
					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * B.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double vjf = V1.get(j, f);
					VS.add(j, f, regI * vjf);

					loss += regI * vjf * vjf;
				}
			}

			// update W
			for (int k = 0; k < numUsers; k++) {
				// users who trusted user k
				SparseVector tv = socialMatrix.col(k);
				for (int u : tv.getIndex()) {
					double tuk = tv.get(u);
					double pred = DenseMatrix.rowMult(B, u, W, k);
					double euj = g(pred) - tuk;
					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						WS.add(k, f, regS * csgd * B.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double wkf = W.get(k, f);
					WS.add(k, f, regU * wkf);

					loss += regU * wkf * wkf;
				}
			}

			loss *= 0.5;
			errs *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * Build TrusteeMF model: W*V2
	 */
	protected void TrusteeMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			// 
		}
	}

	protected double predTr(int u, int j) {
		return DenseMatrix.rowMult(B, u, V1, j);
	}

	protected double predTe(int u, int j) {
		return DenseMatrix.rowMult(W, u, V2, j);
	}

	@Override
	protected double predict(int u, int j) {

		double pred = 0.0;
		switch (model) {
		default:
		case "Tr":
			pred = predTr(u, j);
			break;
		case "Te":
			pred = predTe(u, j);
			break;
		case "T":
			DenseVector uv = B.row(u).add(W.row(u));
			DenseVector jv = V1.row(j).add(V2.row(j));

			pred = uv.inner(jv);
			break;
		}

		return minRate + g(pred) * (maxRate - minRate);
	}

}
