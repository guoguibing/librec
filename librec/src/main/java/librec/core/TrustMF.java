package librec.core;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.SocialRecommender;

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

			// rate matrix
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();
				if (ruj > 0) {
					double pred = predTr(u, j);
					double euj = g(pred) - ruj / maxRate;

					loss += euj * euj;
					errs += euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						BS.add(u, f, csgd * Vr.get(j, f) + regU * Br.get(u, f));
						VS.add(j, f, csgd * Br.get(u, f) + regI * Vr.get(j, f));
					}
				}
			}

			// social matrix
			for (MatrixEntry me : socialMatrix) {

				int u = me.row();
				int k = me.column();
				double tuk = me.get();

				if (tuk > 0) {
					double pred = DenseMatrix.rowMult(Br, u, Wr, k);
					double euj = g(pred) - tuk;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						BS.add(u, f, regS * csgd * Wr.get(k, f) + regU * Br.get(u, f));
						WS.add(k, f, regS * csgd * Br.get(u, f) + regU * Wr.get(k, f));
					}
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

			// rate matrix
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();
				if (ruj > 0) {
					double pred = predTr(u, j);
					double euj = g(pred) - ruj / maxRate;

					loss += euj * euj;
					errs += euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						WS.add(u, f, csgd * Ve.get(j, f) + regU * We.get(u, f));
						VS.add(j, f, csgd * We.get(u, f) + regI * Ve.get(j, f));
					}
				}
			}

			// social matrix
			for (MatrixEntry me : socialMatrix) {

				int k = me.row();
				int u = me.column();
				double tku = me.get();

				if (tku > 0) {
					double pred = DenseMatrix.rowMult(Be, k, We, u);
					double euj = g(pred) - tku;

					loss += regS * euj * euj;

					double csgd = gd(pred) * euj;

					for (int f = 0; f < numFactors; f++) {
						WS.add(u, f, regS * csgd * Be.get(k, f) + regU * We.get(u, f));
						BS.add(k, f, regS * csgd * We.get(u, f) + regU * Be.get(k, f));
					}
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

	protected void updateLRate(int iter) {
		if (iter == 10)
			lRate = 0.03;
		else if (iter == 30)
			lRate = 0.01;
		else if (iter == 100)
			lRate = 0.005;
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
