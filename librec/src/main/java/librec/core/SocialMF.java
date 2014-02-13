package librec.core;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.SocialRecommender;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust
 * propagation for recommendation in social networks</strong>, RecSys 2010.
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SocialMF";
		initByNorm = false;
	}

	@Override
	protected void buildModel() {
		for (int iter = 1; iter <= maxIters; iter++) {

			loss = 0;
			errs = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			// rated items
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();
				if (ruj <= 0.0)
					continue;

				double pred = predict(u, j, false);
				double euj = g(pred) - normalize(ruj);

				errs += euj * euj;
				loss += euj * euj;

				double csgd = gd(pred) * euj;

				for (int f = 0; f < numFactors; f++) {
					PS.add(u, f, csgd * Q.get(j, f) + regU * P.get(u, f));
					QS.add(j, f, csgd * P.get(u, f) + regI * Q.get(j, f));

					loss += regU * P.get(u, f) * P.get(u, f);
					loss += regI * Q.get(j, f) * Q.get(j, f);
				}
			}

			// lambdas: code optimization: small loops outside, large loops inside
			/*
			 * for (int f = 0; f < numFactors; f++) for (int u = 0; u <
			 * numUsers; u++) { double puf = P.get(u, f); userSgds.add(u, f,
			 * regU * puf);
			 * 
			 * loss += regU * puf * puf; }
			 * 
			 * for (int f = 0; f < numFactors; f++) for (int j = 0; j <
			 * numItems; j++) { double qjf = Q.get(j, f); itemSgds.add(j, f,
			 * regI * qjf);
			 * 
			 * loss += regI * qjf * qjf; }
			 */

			// social regularization
			if (regS != 0) {
				for (int u = 0; u < numUsers; u++) {
					SparseVector uv = socialMatrix.row(u);
					int numConns = uv.getCount();
					if (numConns == 0)
						continue;

					double[] sumNNs = new double[numFactors];
					for (int v : uv.getIndex()) {
						for (int f = 0; f < numFactors; f++)
							sumNNs[f] += socialMatrix.get(u, v) * P.get(v, f);
					}

					for (int f = 0; f < numFactors; f++) {
						double diff = P.get(u, f) - sumNNs[f] / numConns;
						PS.add(u, f, regS * diff);

						loss += regS * diff * diff;
					}

					// those who trusted user u
					SparseVector iuv = socialMatrix.column(u);
					int numVs = iuv.getCount();
					for (int v : iuv.getIndex()) {
						double tvu = socialMatrix.get(v, u);

						SparseVector vv = socialMatrix.row(v);
						double[] sumDiffs = new double[numFactors];
						for (int w : vv.getIndex()) {
							for (int f = 0; f < numFactors; f++)
								sumDiffs[f] += socialMatrix.get(v, w) * P.get(w, f);
						}

						numConns = vv.getCount();
						if (numConns > 0)
							for (int f = 0; f < numFactors; f++)
								PS.add(u, f, -regS * (tvu / numVs) * (P.get(v, f) - sumDiffs[f] / numConns)); //TODO: check if numVs or numConns
					}
				}
			}

			// update user factors
			P.add(PS.scale(-lRate));
			Q.add(QS.scale(-lRate));

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;
		}

	}

	@Override
	protected double predict(int u, int j, boolean bounded) {
		double pred = DenseMatrix.rowMult(P, u, Q, j);

		if (bounded)
			return denormalize(g(pred));

		return pred;
	}

}
