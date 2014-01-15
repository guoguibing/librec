package lib.rec.core;

import happy.coding.io.Strings;
import lib.rec.IterativeRecommender;
import lib.rec.MatrixUtils;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Regularized SVD: <em>Arkadiusz Paterek, Improving Regularized Singular Value
 * Decomposition Collaborative Filtering, Proceedings of KDD Cup and Workshop,
 * 2007.</em>
 * 
 * @author guoguibing
 * 
 */
public class RegSVD extends IterativeRecommender {

	public RegSVD(CompRowMatrix rm, CompRowMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "RegSVD";
	}

	@Override
	public void initModel() {

		P = new DenseMatrix(numUsers, numFactors);
		Q = new DenseMatrix(numItems, numFactors);

		// initialize model
		MatrixUtils.init(P, initMean, initStd);
		MatrixUtils.init(Q, initMean, initStd);

		// set to 0 for users without any ratings
		for (int u = 0, um = P.numRows(); u < um; u++) {
			if (MatrixUtils.row(trainMatrix, u).getUsed() == 0) {
				MatrixUtils.setOneValue(P, u, 0.0);
			}
		}
		// set to 0 for items without any ratings
		for (int j = 0, jm = Q.numRows(); j < jm; j++) {
			if (MatrixUtils.col(trainMatrix, j).getUsed() == 0) {
				MatrixUtils.setOneValue(Q, j, 0.0);
			}
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

				double pred = predict(u, j, false);
				double euj = ruj - pred;

				errs += euj * euj;
				loss += euj * euj;

				// update factors
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf - regU * puf;
					double delta_j = euj * puf - regI * qjf;

					P.add(u, f, lRate * delta_u);
					Q.add(j, f, lRate * delta_j);

					loss += regU * puf * puf + regI * qjf * qjf;
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
		return MatrixUtils.rowMult(P, u, Q, j);
	}

	@Override
	public String toString() {
		double learnRate = cf.getDouble("val.learn.rate"); // re-get initial learn rate in case bold driver is used. 
		return Strings.toString(new Object[] { learnRate, regU, regI, numFactors, maxIters, isBoldDriver }, ",");
	}

}
