package librec.undefined;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.SocialRecommender;

/**
 * Our ongoing testing algorithm
 * 
 * @author guoguibing
 * 
 */
public class TrustSVD extends SocialRecommender {

	private DenseMatrix Tr, Te, Y;
	private DenseVector userRates, itemRates, userTrs, userTes;
	private String model;

	public TrustSVD(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		model = cf.getString("TrustSVD.model");
		algoName = "TrustSVD (" + model + ")";
	}

	@Override
	protected void initModel() {
		super.initModel();

		userBiases = new DenseVector(numUsers);
		itemBiases = new DenseVector(numItems);

		userBiases.init(initMean, initStd);
		itemBiases.init(initMean, initStd);

		Tr = new DenseMatrix(numUsers, numFactors);
		Te = new DenseMatrix(numUsers, numFactors);
		Y = new DenseMatrix(numItems, numFactors);

		Tr.init(initMean, initStd);
		Te.init(initMean, initStd);
		Y.init(initMean, initStd);

		userRates = new DenseVector(numUsers);
		userTrs = new DenseVector(numUsers);
		userTes = new DenseVector(numUsers);
		
		for (int u = 0; u < numUsers; u++) {
			userRates.set(u, trainMatrix.rowSize(u));
			userTrs.set(u, socialMatrix.rowSize(u));
			userTes.set(u, socialMatrix.columnSize(u));
		}

		itemRates = new DenseVector(numItems);
		for (int i = 0; i < numItems; i++)
			itemRates.set(i, trainMatrix.columnSize(i));

	}

	private void TrusterSVD() {
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

				SparseVector ur = trainMatrix.row(u);
				int[] nu = ur.getIndex();
				double w_nu = Math.sqrt(nu.length);

				SparseVector tr = socialMatrix.row(u);
				int[] tu = tr.getIndex();
				double w_tu = Math.sqrt(tu.length);

				int wlr_u = nu.length;
				int wlr_j = (int) itemRates.get(j);

				// update factors
				double bu = userBiases.get(u);
				double sgd = euj - regU * wlr_u * bu;
				userBiases.add(u, lRate * sgd);

				loss += regU * wlr_u * bu * bu;

				double bj = itemBiases.get(j);
				sgd = euj - regI * wlr_j * bj;
				itemBiases.add(j, lRate * sgd);

				loss += regI * wlr_j * bj * bj;

				double[] sum_ys = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int i : nu)
						sum += Y.get(i, f);

					sum_ys[f] = w_nu > 0 ? sum / w_nu : sum;
				}

				double[] sum_ts = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int v : tu)
						sum += Tr.get(v, f);

					sum_ts[f] = w_tu > 0 ? sum / w_tu : sum;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf - regU * wlr_u * puf;
					double delta_j = euj * (puf + sum_ys[f] + sum_ts[f]) - regI * wlr_j * qjf;

					P.add(u, f, lRate * delta_u);
					Q.add(j, f, lRate * delta_j);

					loss += regU * wlr_u * puf * puf + regI * wlr_j * qjf * qjf;

					for (int i : nu) {
						double yif = Y.get(i, f);
						int wlr_i = (int) itemRates.get(i);

						double delta_y = euj * qjf / w_nu - regU * wlr_i * yif;
						Y.add(i, f, lRate * delta_y);

						loss += regU * wlr_i * yif * yif;
					}

					for (int v : tu) {
						double tvf = Tr.get(v, f);
						int wlr_v = (int) userTes.get(v);

						double delta_t = euj * qjf / w_tu - regS * wlr_v * tvf;
						Tr.add(v, f, lRate * delta_t);

						loss += regS * wlr_v * tvf * tvf;
					}
				}

			}

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training
	}

	private void TrusteeSVD() {
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

				SparseVector ur = trainMatrix.row(u);
				int[] nu = ur.getIndex();
				double w_nu = Math.sqrt(nu.length);

				SparseVector te = socialMatrix.column(u);
				int[] tu = te.getIndex();
				double w_tu = Math.sqrt(tu.length);

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
					double sum = 0;
					for (int i : nu)
						sum += Y.get(i, f);

					sum_ys[f] = w_nu > 0 ? sum / w_nu : sum;
				}

				double[] sum_ts = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int v : tu)
						sum += Te.get(v, f);

					sum_ts[f] = w_tu > 0 ? sum / w_tu : sum;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf - regU * puf;
					double delta_j = euj * (puf + sum_ys[f] + sum_ts[f]) - regI * qjf;

					P.add(u, f, lRate * delta_u);
					Q.add(j, f, lRate * delta_j);

					loss += regU * puf * puf + regI * qjf * qjf;

					for (int i : nu) {
						double yif = Y.get(i, f);
						double delta_y = euj * qjf / w_nu - regU * yif;
						Y.add(i, f, lRate * delta_y);

						loss += regU * yif * yif;
					}

					for (int v : tu) {
						double tvf = Te.get(v, f);
						double delta_t = euj * qjf / w_tu - regS * tvf;
						Te.add(v, f, lRate * delta_t);

						loss += regS * tvf * tvf;
					}
				}

			}

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training
	}

	private void TrTeSVD() {
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

				SparseVector ur = trainMatrix.row(u);
				int[] nu = ur.getIndex();
				double w_nu = Math.sqrt(nu.length);

				SparseVector tr = socialMatrix.row(u);
				int[] tru = tr.getIndex();
				double w_tru = Math.sqrt(tru.length);

				SparseVector te = socialMatrix.column(u);
				int[] teu = te.getIndex();
				double w_teu = Math.sqrt(teu.length);

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
					double sum = 0;
					for (int i : nu)
						sum += Y.get(i, f);

					sum_ys[f] = w_nu > 0 ? sum / w_nu : sum;
				}

				double[] sum_trs = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int v : tru)
						sum += Tr.get(v, f);

					sum_trs[f] = w_tru > 0 ? sum / w_tru : sum;
				}

				double[] sum_tes = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double sum = 0;
					for (int k : tru)
						sum += Te.get(k, f);

					sum_tes[f] = w_teu > 0 ? sum / w_teu : sum;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf - regU * puf;
					double delta_j = euj * (puf + sum_ys[f] + sum_trs[f] + sum_tes[f]) - regI * qjf;

					P.add(u, f, lRate * delta_u);
					Q.add(j, f, lRate * delta_j);

					loss += regU * puf * puf + regI * qjf * qjf;

					for (int i : nu) {
						double yif = Y.get(i, f);
						double delta_y = euj * qjf / w_nu - regU * yif;
						Y.add(i, f, lRate * delta_y);

						loss += regU * yif * yif;
					}

					for (int v : tru) {
						double tvf = Tr.get(v, f);
						double delta_t = euj * qjf / w_tru - regS * tvf;
						Tr.add(v, f, lRate * delta_t);

						loss += regS * tvf * tvf;
					}

					for (int k : teu) {
						double tkf = Te.get(k, f);
						double delta_t = euj * qjf / w_teu - regS * tkf;
						Te.add(k, f, lRate * delta_t);

						loss += regS * tkf * tkf;
					}
				}

			}

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training
	}

	@Override
	protected void buildModel() {
		switch (model) {
		case "Tr":
			TrusterSVD();
			break;
		case "Te":
			TrusteeSVD();
			break;
		case "T":
		default:
			TrTeSVD();
			break;
		}
	}

	private double predictTr(int u, int j) {

		double pred = 0.0;
		SparseVector tr = socialMatrix.row(u);
		int count = tr.getCount();

		if (count > 0) {
			double sum = 0.0;
			for (int v : tr.getIndex())
				sum += DenseMatrix.rowMult(Tr, v, Q, j);

			pred = sum / Math.sqrt(count);
		}

		return pred;
	}

	private double predictTe(int u, int j) {

		double pred = 0.0;
		SparseVector te = socialMatrix.column(u);
		int count = te.getCount();

		if (count > 0) {
			double sum = 0.0;
			for (int w : te.getIndex())
				sum += DenseMatrix.rowMult(Te, w, Q, j);

			pred = sum / Math.sqrt(count);
		}

		return pred;
	}

	@Override
	protected double predict(int u, int j) {
		double pred = globalMean + userBiases.get(u) + itemBiases.get(j);
		pred += DenseMatrix.rowMult(P, u, Q, j);

		switch (model) {
		case "Tr":
			pred += predictTr(u, j);
			break;
		case "Te":
			pred += predictTe(u, j);
			break;
		case "T":
		default:
			pred += predictTr(u, j) + predictTe(u, j);
			break;
		}

		return pred;
	}

}