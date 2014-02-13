package librec.core;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.SocialRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI
 * 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialRecommender {

	protected DenseMatrix Br, Wr, Vr;
	protected DenseMatrix Be, We, Ve;

	protected String model;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		model = cf.getString("TrustMF.model");
		algoName = "TrustMF (" + model + ")";
	}

	protected void initTr() {
		Vr = new DenseMatrix(numItems, numFactors);
		// Vr.init(initMean, initStd);
		Vr.init();

		for (int j = 0; j < numItems; j++)
			if (trainMatrix.columnSize(j) == 0)
				Vr.setRow(j, 0.0);

		Br = new DenseMatrix(numUsers, numFactors);
		Wr = new DenseMatrix(numUsers, numFactors);

		// Br.init(initMean, initStd);
		// Wr.init(initMean, initStd);
		Br.init();
		Wr.init();

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0)
				Br.setRow(u, 0.0);

			if (socialMatrix.columnSize(u) == 0)
				Wr.setRow(u, 0.0);
		}
	}

	protected void initTe() {
		Ve = new DenseMatrix(numItems, numFactors);
		// Ve.init(initMean, initStd);
		Ve.init();

		for (int j = 0; j < numItems; j++)
			if (trainMatrix.columnSize(j) == 0)
				Ve.setRow(j, 0.0);

		Be = new DenseMatrix(numUsers, numFactors);
		We = new DenseMatrix(numUsers, numFactors);

		// Be.init(initMean, initStd);
		// We.init(initMean, initStd);
		Be.init();
		We.init();

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

						double euj = g(pred) - ruj / maxRate;

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
				for (int f = 0; f < numFactors; f++) {
					double buf = Br.get(u, f);
					BS.add(u, f, regU * (nbu + mbu) * buf);

					loss += regU * (nbu + mbu) * buf * buf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.column(j);
				for (int u : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(u);
					// double euj = minRate + g(pred) * (maxRate - minRate) -
					// ruj;
					double euj = g(pred) - ruj / maxRate;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * Br.get(u, f));
				}

				// lambda
				int nvj = rv.getCount() > 0 ? rv.getCount() : 1;
				for (int f = 0; f < numFactors; f++) {
					double vjf = Vr.get(j, f);
					VS.add(j, f, regI * nvj * vjf);

					loss += regI * nvj * vjf * vjf;
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
				for (int f = 0; f < numFactors; f++) {
					double wkf = Wr.get(k, f);
					WS.add(k, f, regU * mwk * wkf);

					loss += regU * mwk * wkf * wkf;
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

	protected void TrusterMF2() {
		for (int iter = 1; iter <= maxIters; iter++) {
			loss = 0;
			errs = 0;

			// gradients of B, V, W
			DenseMatrix BS = new DenseMatrix(numFactors, numUsers);
			DenseMatrix VS = new DenseMatrix(numFactors, numItems);
			DenseMatrix WS = new DenseMatrix(numFactors, numUsers);

			// compute F
			Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
			for (int u = 0; u < numUsers; u++) {

				// rated items
				int nbu = 1;
				if (u < trainMatrix.numRows()) {
					SparseVector rv = trainMatrix.row(u);
					nbu = rv.getCount() > 0 ? rv.getCount() : 1;
					for (int j : rv.getIndex()) {
						double pred = predTr(u, j);
						double ruj = rv.get(j);

						double euj = g(pred) - ruj / maxRate;

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
				for (int f = 0; f < numFactors; f++) {
					double buf = Br.get(u, f);
					BS.add(u, f, regU * (nbu + mbu) * buf);

					loss += regU * (nbu + mbu) * buf * buf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.column(j);
				for (int u : rv.getIndex()) {
					double pred = predTr(u, j);
					double ruj = rv.get(u);
					// double euj = minRate + g(pred) * (maxRate - minRate) -
					// ruj;
					double euj = g(pred) - ruj / maxRate;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * Br.get(u, f));
				}

				// lambda
				int nvj = rv.getCount() > 0 ? rv.getCount() : 1;
				for (int f = 0; f < numFactors; f++) {
					double vjf = Vr.get(j, f);
					VS.add(j, f, regI * nvj * vjf);

					loss += regI * nvj * vjf * vjf;
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
				for (int f = 0; f < numFactors; f++) {
					double wkf = Wr.get(k, f);
					WS.add(k, f, regU * mwk * wkf);

					loss += regU * mwk * wkf * wkf;
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

						double euj = g(pred) - ruj / maxRate;

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
				for (int f = 0; f < numFactors; f++) {
					double wuf = We.get(u, f);
					WS.add(u, f, regU * (nwu + mwu) * wuf);

					loss += regU * (nwu + mwu) * wuf * wuf;
				}
			}

			// compute V sgds
			for (int j = 0; j < numItems; j++) {
				// users who rated item j
				SparseVector rv = trainMatrix.column(j);
				for (int u : rv.getIndex()) {
					double pred = predTe(u, j);
					double ruj = rv.get(u);
					double euj = g(pred) - ruj / maxRate;

					double csgd = gd(pred) * euj;
					for (int f = 0; f < numFactors; f++)
						VS.add(j, f, csgd * We.get(u, f));
				}

				// lambda
				int nvj = rv.getCount() > 0 ? rv.getCount() : 1;
				for (int f = 0; f < numFactors; f++) {
					double vjf = Ve.get(j, f);
					VS.add(j, f, regI * nvj * vjf);

					loss += regI * nvj * vjf * vjf;
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
				for (int f = 0; f < numFactors; f++) {
					double bkf = Be.get(k, f);
					BS.add(k, f, regU * mbk * bkf);

					loss += regU * mbk * bkf * bkf;
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

	protected void updateLRate2(int iter) {
		if (iter == 10)
			lRate = 0.03;
		else if (iter == 30)
			lRate = 0.01;
		else if (iter == 100)
			lRate = 0.005;

		last_loss = loss;
		last_errs = errs;
	}

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
			DenseVector uv = Br.row(u).add(We.row(u, false));
			DenseVector jv = Vr.row(j).add(Ve.row(j, false));

			pred = uv.scale(0.5).inner(jv.scale(0.5));
			break;
		}

		return g(pred) * maxRate;
	}
}
