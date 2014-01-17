package lib.rec.core;

import happy.coding.math.Maths;
import happy.coding.system.Debug;
import lib.rec.MatrixUtils;
import lib.rec.intf.SocialRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust
 * propagation for recommendation in social networks</strong>, RecSys 2010.
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold, String path) {
		super(trainMatrix, testMatrix, fold, path);

		algoName = "SocialMF";

		if (Debug.ON) {
			// use the suggested parameters for epinions from the paper
			regU = 0.1;
			regI = 0.1;
			regS = 5;
		}
	}

	@Override
	protected void initModel() {
		super.initModel();

		invSocialMatrix = socialMatrix.copy();
		socialMatrix.transpose(invSocialMatrix);
	}

	@Override
	protected void buildModel() {
		for (int iter = 0; iter < maxIters; iter++) {

			loss = 0;
			errs = 0;

			DenseMatrix userSgds = new DenseMatrix(numUsers, numFactors);
			DenseMatrix itemSgds = new DenseMatrix(numItems, numFactors);

			// rated items
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double rate = me.get();
				if (rate <= 0.0)
					continue;

				double ruj = Maths.normalize(rate, minRate, maxRate);
				double pred = predict(u, j);
				double euj = ruj - g(pred);

				errs += euj * euj;
				loss += euj * euj;

				double csgd = -euj * gd(pred);

				for (int f = 0; f < numFactors; f++) {
					userSgds.add(u, f, csgd * Q.get(j, f));
					itemSgds.add(j, f, csgd * P.get(u, f));
				}
			}

			// lambdas
			for (int u = 0; u < numUsers; u++)
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					userSgds.add(u, f, regU * puf);

					loss += regU * puf * puf;
				}

			for (int j = 0; j < numItems; j++)
				for (int f = 0; f < numFactors; f++) {
					double qjf = Q.get(j, f);
					itemSgds.add(j, f, regI * qjf);

					loss += regI * qjf * qjf;
				}

			// social regularization
			if (regS != 0) {
				for (int u = 0; u < numUsers; u++) {
					SparseVector uv = MatrixUtils.row(socialMatrix, u);
					double[] sumNNs = new double[numFactors];
					for (int v : uv.getIndex()) {
						for (int f = 0; f < numFactors; f++)
							sumNNs[f] += socialMatrix.get(u, v) * P.get(v, f);
					}

					for (int f = 0; f < numFactors; f++) {
						double diff = P.get(u, f) - sumNNs[f] / uv.getUsed();
						userSgds.add(u, f, regS * diff);

						loss += regS * diff * diff;
					}

					SparseVector iuv = MatrixUtils.row(invSocialMatrix, u);
					for (int v : iuv.getIndex()) {
						double tvu = socialMatrix.get(v, u);

						SparseVector vv = MatrixUtils.row(socialMatrix, v);
						double[] sumDiffs = new double[numFactors];
						for (int w : vv.getIndex()) {
							for (int f = 0; f < numFactors; f++)
								sumDiffs[f] = socialMatrix.get(v, w) * P.get(w, f);
						}

						for (int f = 0; f < numFactors; f++)
							userSgds.add(u, f, -regS * tvu * (P.get(v, f) - sumDiffs[f] / vv.getUsed()));
					}
				}
			}

			// update user factors
			P.add(userSgds.scale(-lRate));
			Q.add(itemSgds.scale(-lRate));

			errs *= 0.5;
			loss *= 0.5;

			if (postEachIter(iter))
				break;
		}

	}

}
