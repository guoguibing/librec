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
 * <strong>NOTE:</strong> to have a fair comparison with other approaches, we do
 * not use the "weighted-lambda-regularization" as the paper did.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialRecommender {

	protected DenseMatrix Br, Wr, Vr;
	protected DenseMatrix Be, We, Ve;

	protected String model;

	// is weighted-lambda-regularization enabled
	protected boolean isWeightedLambdaReg;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		model = cf.getString("TrustMF.model");
		algoName = "TrustMF (" + model + ")";

		isWeightedLambdaReg = cf.isOn("is.TrustMF.wlr");
	}

	protected void initTr() {
		Vr = new DenseMatrix(numItems, numFactors);
		Vr.init(initMean, initStd);

		for (int j = 0; j < numItems; j++)
			if (trainMatrix.columnSize(j) == 0)
				Vr.setRow(j, 0.0);

		Br = new DenseMatrix(numUsers, numFactors);
		Wr = new DenseMatrix(numUsers, numFactors);

		Br.init(initMean, initStd);
		Wr.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++)
			if (socialMatrix.rowSize(u) == 0) {
				Br.setRow(u, 0.0);

				if (socialMatrix.columnSize(u) == 0)
					Wr.setRow(u, 0.0);
			}
	}

	protected void initTe() {
		Ve = new DenseMatrix(numItems, numFactors);
		Ve.init(initMean, initStd);

		for (int j = 0; j < numItems; j++)
			if (trainMatrix.columnSize(j) == 0)
				Ve.setRow(j, 0.0);

		Be = new DenseMatrix(numUsers, numFactors);
		We = new DenseMatrix(numUsers, numFactors);

		Be.init(initMean, initStd);
		We.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0)
				Be.setRow(u, 0.0);

			if (socialMatrix.columnSize(u) == 0)
				We.setRow(u, 0.0);
		}
	}

	@Override
	protected void initModel() {
		switch (model) {
		case "Tr":
			initTr();
			break;
		case "Te":
			initTe();
			break;
		case "T":
		default:
			initTr();
			initTe();
			break;
		}
	}

	@Override
	protected void buildModel() {
		switch (model) {
		case "Tr":
			TrusterMF();
			break;
		case "Te":
			TrusteeMF();
			break;
		case "T":
		default:
			TrusterMF();
			TrusteeMF();
			break;
		}
	}

	/**
	 * Build TrusterMF model: Br*Vr
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
				int nbu = 1;
				if (u < trainMatrix.numRows()) {
					SparseVector rv = trainMatrix.row(u);
					nbu = rv.getCount() > 0 ? rv.getCount() : 1;
					for (int j : rv.getIndex()) {
						double pred = predTr(u, j);
						double ruj = rv.get(j);

						double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

						loss += euj * euj;
						errs += euj * euj;

						double csgd = gd(pred) * euj;

						for (int f = 0; f < numFactors; f++)
							BS.add(u, f, csgd * Vr.get(j, f));
					}
				}

				// trusted users
				SparseVector tv = socialMatrix.row(u);
				int mbu = tv.getCount() > 0 ? tv.getCount() : 1;
				for (int k : tv.getIndex()) {
					double tuk = tv.get(k);
					double pred = DenseMatrix.rowMult(Br, u, Wr, k);
					double euj = g(pred) - tuk;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						BS.add(u, f, regS * csgd * Wr.get(k, f));
				}

				// lambda
				double wlr = isWeightedLambdaReg ? nbu + mbu : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double buf = Br.get(u, f);
					BS.add(u, f, regU * wlr * buf);

					loss += regU * buf * buf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.column(j);
				for (int u : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(u);
					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * Br.get(u, f));
				}

				// lambda
				int nvj = rv.getCount() > 0 ? rv.getCount() : 1;
				double wlr = isWeightedLambdaReg ? nvj : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double vjf = Vr.get(j, f);
					VS.add(j, f, regI * wlr * vjf);

					loss += regI * vjf * vjf;
				}
			}

			// compute W sgds
			for (int k = 0; k < numUsers; k++) {
				// users who trusted user k
				SparseVector tv = socialMatrix.column(k);
				for (int u : tv.getIndex()) {
					double tuk = tv.get(u);
					double pred = DenseMatrix.rowMult(Br, u, Wr, k);
					double euj = g(pred) - tuk;
					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						WS.add(k, f, regS * csgd * Br.get(u, f));
				}

				// lambda
				int mwk = tv.getCount() > 0 ? tv.getCount() : 1;
				double wlr = isWeightedLambdaReg ? mwk : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double wkf = Wr.get(k, f);
					WS.add(k, f, regU * wlr * wkf);

					loss += regU * wkf * wkf;
				}
			}

			Br.add(BS.scale(-lRate));
			Vr.add(VS.scale(-lRate));
			Wr.add(WS.scale(-lRate));

			loss *= 0.5;
			errs *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	/**
	 * Build TrusteeMF model: We*Ve
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
				int nwu = 1;
				if (u < trainMatrix.numRows()) {
					SparseVector rv = trainMatrix.row(u);
					nwu = rv.getCount() > 0 ? rv.getCount() : 1;
					for (int j : rv.getIndex()) {
						double pred = predTe(u, j);
						double ruj = rv.get(j);

						double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

						loss += euj * euj;
						errs += euj * euj;

						double csgd = gd(pred) * euj;

						for (int f = 0; f < numFactors; f++)
							WS.add(u, f, csgd * Ve.get(j, f));
					}
				}

				// users who trusted user u
				SparseVector tv = socialMatrix.column(u);
				int mwu = tv.getCount() > 0 ? tv.getCount() : 1;
				for (int k : tv.getIndex()) {
					double tku = tv.get(k);
					double pred = DenseMatrix.rowMult(Be, k, We, u);
					double euj = g(pred) - tku;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						WS.add(u, f, regS * csgd * Be.get(k, f));
				}

				// lambda
				double wlr = isWeightedLambdaReg ? nwu + mwu : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double wuf = We.get(u, f);
					WS.add(u, f, regU * wlr * wuf);

					loss += regU * wuf * wuf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.column(j);
				for (int u : rv.getIndex()) {
					double pred = predTe(u, j);
					double ruj = rv.get(u);
					double euj = minRate + g(pred) * (maxRate - minRate) - ruj;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * We.get(u, f));
				}

				// lambda
				int nvj = rv.getCount() > 0 ? rv.getCount() : 1;
				double wlr = isWeightedLambdaReg ? nvj : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double vjf = Ve.get(j, f);
					VS.add(j, f, regI * wlr * vjf);

					loss += regI * vjf * vjf;
				}
			}

			// compute B sgds
			for (int k = 0; k < numUsers; k++) {
				// trusted users
				SparseVector tv = socialMatrix.row(k);
				for (int u : tv.getIndex()) {
					double tku = tv.get(u);
					double pred = DenseMatrix.rowMult(Be, k, We, u);
					double euj = g(pred) - tku;
					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++)
						BS.add(k, f, regS * csgd * Be.get(u, f));
				}

				// lambda
				int mbk = tv.getCount() > 0 ? tv.getCount() : 1;
				double wlr = isWeightedLambdaReg ? mbk : 1.0;
				for (int f = 0; f < numFactors; f++) {
					double bkf = Be.get(k, f);
					BS.add(k, f, regU * wlr * bkf);

					loss += regU * bkf * bkf;
				}
			}

			Be.add(BS.scale(-lRate));
			Ve.add(VS.scale(-lRate));
			We.add(WS.scale(-lRate));

			loss *= 0.5;
			errs *= 0.5;

			if (isConverged(iter))
				break;
		}
	}

	protected double predTr(int u, int j) {
		return DenseMatrix.rowMult(Br, u, Vr, j);
	}

	protected double predTe(int u, int j) {
		return DenseMatrix.rowMult(We, u, Ve, j);
	}

	@Override
	protected double predict(int u, int j) {

		double pred = 0.0;
		switch (model) {
		case "Tr":
			pred = predTr(u, j);
			break;
		case "Te":
			pred = predTe(u, j);
			break;
		case "T":
		default:
			DenseVector uv = Br.row(u).add(We.row(u));
			DenseVector jv = Vr.row(j).add(Ve.row(j));

			pred = uv.inner(jv);
			break;
		}

		return minRate + g(pred) * (maxRate - minRate);
	}

	@Override
	public String toString() {
		return super.toString() + "," + isWeightedLambdaReg;
	}

}
