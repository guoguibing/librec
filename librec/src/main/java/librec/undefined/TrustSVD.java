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

	private DenseMatrix Tr, Te;
	private String model; 

	public TrustSVD(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "TrustSVD";

		model = cf.getString("TrustSVD.model");
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

		Tr.init(initMean, initStd);
		Te.init(initMean, initStd);
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

				SparseVector tr = socialMatrix.row(u);
				int[] tns = tr.getIndex();
				double w = Math.sqrt(tns.length);

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
					for (int k : tns)
						sum_f += Tr.get(k, f);

					sum_ys[f] = w > 0 ? sum_f / w : sum_f;
				}

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double sgd_u = euj * qjf - regU * puf;
					double sgd_j = euj * (puf + sum_ys[f]) - regI * qjf;

					P.add(u, f, lRate * sgd_u);
					Q.add(j, f, lRate * sgd_j);

					loss += regU * puf * puf + regI * qjf * qjf;

					for (int k : tns) {
						double ykf = Tr.get(k, f);
						double delta_y = euj * qjf / w - regU * ykf;
						Tr.add(k, f, lRate * delta_y);

						loss += regU * ykf * ykf;
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
	}

	private void TrTeSVD() {
	}

	@Override
	protected void buildModel() {
		switch (model) {
		case "Tr":
			TrusterSVD();
			break;
		case "Te":
			TrusteeSVD();
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