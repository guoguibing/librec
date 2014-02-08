package lib.rec.core;

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

	protected DenseMatrix B1, W1, V1;
	protected DenseMatrix B2, W2, V2;

	protected String model;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		model = cf.getString("TrustMF.model").toLowerCase();
		algoName = "TrustMF-" + model;
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

		B1 = new DenseMatrix(numUsers, numFactors);
		B2 = new DenseMatrix(numUsers, numFactors);
		W1 = new DenseMatrix(numUsers, numFactors);
		W2 = new DenseMatrix(numUsers, numFactors);

		B1.init(initMean, initStd);
		B2.init(initMean, initStd);
		W1.init(initMean, initStd);
		W2.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0) {
				B1.setRow(u, 0.0);
				B2.setRow(u, 0.0);
			}

			if (socialMatrix.colSize(u) == 0) {
				W1.setRow(u, 0.0);
				W2.setRow(u, 0.0);
			}
		}
	}

	@Override
	protected void buildModel() {
		switch (model) {
		case "truster":
			TrusterMF();
			break;
		case "trustee":
			TrusteeMF();
			break;
		case "trust":
		default:
			TrusterMF();
			TrusteeMF();
			break;
		}
	}

	/**
	 * Build TrusterMF model: B1*V1
	 */
	protected void TrusterMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			loss = 0;
			errs = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix VS = new DenseMatrix(numItems, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);

			// compute B sgds
			for (int u = 0; u < numUsers; u++) {

				// rated items
				if (u < trainMatrix.numRows()) {
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
				}

				// trusted users
				SparseVector tv = socialMatrix.row(u);
				for (int k : tv.getIndex()) {
					double tuk = tv.get(k);
					double pred = DenseMatrix.rowMult(B1, u, W1, k);
					double euj = g(pred) - tuk;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						BS.add(u, f, regS * csgd * W1.get(k, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double buf = B1.get(u, f);
					BS.add(u, f, regU * buf);

					loss += regU * buf * buf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.col(j);
				for (int u : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(u);
					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * B1.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double vjf = V1.get(j, f);
					VS.add(j, f, regI * vjf);

					loss += regI * vjf * vjf;
				}
			}

			// compute W sgds
			for (int k = 0; k < numUsers; k++) {
				// users who trusted user k
				SparseVector tv = socialMatrix.col(k);
				for (int u : tv.getIndex()) {
					double tuk = tv.get(u);
					double pred = DenseMatrix.rowMult(B1, u, W1, k);
					double euj = g(pred) - tuk;
					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						WS.add(k, f, regS * csgd * B1.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double wkf = W1.get(k, f);
					WS.add(k, f, regU * wkf);

					loss += regU * wkf * wkf;
				}
			}

			B1.add(BS.scale(-lRate));
			V1.add(VS.scale(-lRate));
			W1.add(WS.scale(-lRate));

			loss *= 0.5;
			errs *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * Build TrusteeMF model: W2*V2
	 */
	protected void TrusteeMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			loss = 0;
			errs = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix VS = new DenseMatrix(numItems, numFactors);
			DenseMatrix WS = new DenseMatrix(numUsers, numFactors);

			// compute W sgds
			for (int u = 0; u < numUsers; u++) {

				// rated items
				if (u < trainMatrix.numRows()) {
					SparseVector rv = trainMatrix.row(u);
					for (int j : rv.getIndex()) {
						double pred = predTe(u, j);
						double ruj = rv.get(j);

						double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

						loss += euj * euj;
						errs += euj * euj;

						double csgd = gd(pred) * euj;

						for (int f = 0; f < numFactors; f++)
							WS.add(u, f, csgd * V2.get(j, f));
					}
				}

				// users who trusted user u
				SparseVector tv = socialMatrix.col(u);
				for (int k : tv.getIndex()) {
					double tku = tv.get(k);
					double pred = DenseMatrix.rowMult(B2, k, W2, u);
					double euj = g(pred) - tku;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						WS.add(u, f, regS * csgd * B2.get(k, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double wuf = W2.get(u, f);
					WS.add(u, f, regU * wuf);

					loss += regU * wuf * wuf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.col(j);
				for (int u : rv.getIndex()) {
					double pred = predTe(u, j);
					double ruj = rv.get(u);
					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * W2.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double vjf = V2.get(j, f);
					VS.add(j, f, regI * vjf);

					loss += regI * vjf * vjf;
				}
			}

			// compute B sgds
			for (int k = 0; k < numUsers; k++) {
				// trusted users
				SparseVector tv = socialMatrix.row(k);
				for (int u : tv.getIndex()) {
					double tku = tv.get(u);
					double pred = DenseMatrix.rowMult(B2, k, W2, u);
					double euj = g(pred) - tku;
					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						BS.add(k, f, regS * csgd * B2.get(u, f));
				}

				// lambda
				for (int f = 0; f < numFactors; f++) {
					double bkf = B2.get(k, f);
					BS.add(k, f, regU * bkf);

					loss += regU * bkf * bkf;
				}
			}

			B2.add(BS.scale(-lRate));
			V2.add(VS.scale(-lRate));
			W2.add(WS.scale(-lRate));

			loss *= 0.5;
			errs *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	protected double predTr(int u, int j) {
		return DenseMatrix.rowMult(B1, u, V1, j);
	}

	protected double predTe(int u, int j) {
		return DenseMatrix.rowMult(W2, u, V2, j);
	}

	@Override
	protected double predict(int u, int j) {

		double pred = 0.0;
		switch (model) {
		case "truster":
			pred = predTr(u, j);
			break;
		case "trustee":
			pred = predTe(u, j);
			break;
		case "trust":
		default:
			DenseVector uv = B1.row(u).add(W2.row(u));
			DenseVector jv = V1.row(j).add(V2.row(j));

			pred = uv.inner(jv);
			break;
		}

		return minRate + g(pred) * (maxRate - minRate);
	}

}
