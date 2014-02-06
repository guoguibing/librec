package lib.rec.core;

import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;
import lib.rec.data.SparseVec;
import lib.rec.intf.SocialRecommender;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust
 * propagation for recommendation in social networks</strong>, RecSys 2010.
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SocialMF";
	}

	@Override
	protected void initModel() {
		super.initModel();

		/*
		 * invSocialMatrix = new FlexCompRowMatrix(numUsers, numUsers); for
		 * (MatrixEntry me : socialMatrix) invSocialMatrix.set(me.column(),
		 * me.row(), me.get());
		 */
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
				double ruj = me.get();
				if (ruj <= 0.0)
					continue;

				double pred = DenseMat.rowMult(P, u, Q, j);
				double euj = ruj - (minRate + g(pred) * (maxRate - minRate));

				errs += euj * euj;
				loss += euj * euj;

				double csgd = -euj * gd(pred);

				for (int f = 0; f < numFactors; f++) {
					userSgds.add(u, f, csgd * Q.get(j, f));
					itemSgds.add(j, f, csgd * P.get(u, f));
				}
			}

			// lambdas: code optimization: small loops outside, large loops inside
			for (int f = 0; f < numFactors; f++)
				for (int u = 0; u < numUsers; u++) {
					double puf = P.get(u, f);
					userSgds.add(u, f, regU * puf);

					loss += regU * puf * puf;
				}

			for (int f = 0; f < numFactors; f++)
				for (int j = 0; j < numItems; j++) {
					double qjf = Q.get(j, f);
					itemSgds.add(j, f, regI * qjf);

					loss += regI * qjf * qjf;
				}

			// social regularization
			if (regS != 0) {
				for (int u = 0; u < numUsers; u++) {
					SparseVec uv = socialMatrix.row(u);
					double[] sumNNs = new double[numFactors];
					for (int v : uv.getIndex()) {
						for (int f = 0; f < numFactors; f++)
							sumNNs[f] += socialMatrix.get(u, v) * P.get(v, f);
					}

					int numConns = uv.getUsed();
					if (numConns > 0) {
						for (int f = 0; f < numFactors; f++) {
							double diff = P.get(u, f) - sumNNs[f] / numConns;
							userSgds.add(u, f, regS * diff);

							loss += regS * diff * diff;
						}
					}

					// those who trusted user u
					SparseVector iuv = invSocialMatrix.getRow(u);
					int numVs = iuv.getUsed();
					for (int v : iuv.getIndex()) {
						double tvu = socialMatrix.get(v, u);

						SparseVec vv = socialMatrix.row(v);
						double[] sumDiffs = new double[numFactors];
						for (int w : vv.getIndex()) {
							for (int f = 0; f < numFactors; f++)
								sumDiffs[f] += socialMatrix.get(v, w) * P.get(w, f);
						}

						numConns = vv.getUsed();
						if (numConns > 0)
							for (int f = 0; f < numFactors; f++)
								userSgds.add(u, f, -regS * (tvu / numVs) * (P.get(v, f) - sumDiffs[f] / numConns));
					}
				}
			}

			// update user factors
			P.add(userSgds.scale(-lRate));
			Q.add(itemSgds.scale(-lRate));

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;
		}

	}

	@Override
	protected double predict(int u, int j) {
		double pred = DenseMat.rowMult(P, u, Q, j);
		return minRate + g(pred) * (maxRate - minRate);
	}

}
