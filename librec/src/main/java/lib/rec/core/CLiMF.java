package lib.rec.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.rec.data.SparseMatrix;
import lib.rec.data.SparseVector;
import lib.rec.intf.IterativeRecommender;

/**
 * Shi et al., <strong>Climf: learning to maximize reciprocal rank with
 * collaborative less-is-more filtering.</strong>, RecSys 2012.
 * 
 * @author guoguibing
 * 
 */
public class CLiMF extends IterativeRecommender {

	public CLiMF(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "CLiMF";
	}

	@Override
	protected void buildModel() {

		for (int iter = 1; iter <= maxIters; iter++) {

			loss = 0;
			errs = 0;

			for (int u = 0; u < numUsers; u++) {

				// all user u's ratings
				SparseVector uv = trainMatrix.row(u);

				// compute sgd for user u
				double[] sgds = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {

					double sgd = -regU * P.get(u, f);

					for (int j : uv.getIndex()) {
						double fuj = predict(u, j);
						double qjf = Q.get(j, f);

						sgd += g(-fuj) * qjf;

						for (int k : uv.getIndex()) {
							if (k == j)
								continue;

							double fuk = predict(u, k);
							double qkf = Q.get(k, f);

							double x = fuk - fuj;

							sgd += gd(x) / (1 - g(x)) * (qjf - qkf);
						}
					}

					sgds[f] = sgd;
				}

				// compute sgds for items rated by user u
				Map<Integer, List<Double>> itemSgds = new HashMap<>();
				//for (int j : items) {
				for (int j = 0; j < numItems; j++) {

					double fuj = predict(u, j);
					List<Double> jSgds = new ArrayList<>();
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qjf = Q.get(j, f);

						double yuj = uv.contains(j) ? 1.0 : 0.0;
						double sgd = yuj * g(-fuj) * puf - regI * qjf;

						for (int k : uv.getIndex()) {
							if (k == j)
								continue;

							double fuk = predict(u, k);
							double x = fuk - fuj;

							sgd += gd(-x) * (1.0 / (1 - g(x)) - 1.0 / (1 - g(-x))) * puf;
						}
						jSgds.add(sgd);
					}

					itemSgds.put(j, jSgds);
				}

				// update factors
				for (int f = 0; f < numFactors; f++)
					P.add(u, f, lRate * sgds[f]);

				for (int j = 0; j < numItems; j++) {
					List<Double> jSgds = itemSgds.get(j);
					for (int f = 0; f < numFactors; f++)
						Q.add(j, f, lRate * jSgds.get(f));
				}

				// compute loss
				for (int j = 0; j < numItems; j++) {

					if (uv.contains(j)) {
						double fuj = predict(u, j);
						double ruj = uv.get(j);

						errs += (ruj - fuj) * (ruj - fuj);
						loss += Math.log(g(fuj));

						for (int k : uv.getIndex()) {
							double fuk = predict(u, k);
							loss += Math.log(1 - g(fuk - fuj));
						}
					}

					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qjf = Q.get(j, f);

						loss += -0.5 * (regU * puf * puf + regI * qjf * qjf);
					}
				}

			}
			errs *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training

	}

}
