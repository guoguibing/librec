package lib.rec.ext;

import happy.coding.io.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.rec.RecUtils;
import lib.rec.core.CLiMF;
import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DRMPlus extends CLiMF {

	protected double alpha, minSim;

	protected boolean isPosOnly;

	public DRMPlus(SparseMat rm, SparseMat tm, int fold) {
		super(rm, tm, fold);

		algoName = "DRMPlus";
		alpha = RecUtils.getMKey(params, "val.diverse.alpha");

		initStd = 0.01;
		isPosOnly = cf.isOn("is.similarity.pos");
		minSim = isPosOnly ? 0.0 : Double.NEGATIVE_INFINITY;
	}

	@Override
	protected void buildModel() {

		for (int iter = 1; iter <= maxIters; iter++) {

			loss = 0;
			errs = 0;
			for (int u = 0; u < numUsers; u++) {

				// all user u's ratings
				SparseVector uv = trainMatrix.row(u);
				List<Integer> items = Lists.toList(uv.getIndex());
				//double w = Math.sqrt(uv.getUsed());

				// compute sgd for user u
				double[] sgds = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {

					double sgd = -regU * P.get(u, f);

					for (int j : items) {
						double fuj = predict(u, j);
						double qjf = Q.get(j, f);

						sgd += g(-fuj) * qjf;

						for (int k : items) {
							if (k == j)
								continue;

							double fuk = predict(u, k);
							double qif = Q.get(k, f);

							double x = fuk - fuj;

							sgd += gd(x) / (1 - g(x)) * (qjf - qif);
						}
					}

					sgds[f] = sgd;
				}

				// compute sgds for items rated by user u
				Map<Integer, List<Double>> itemSgds = new HashMap<>();
				for (int j = 0; j < numItems; j++) {

					double fuj = predict(u, j);

					List<Double> jSgds = new ArrayList<>();
					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qjf = Q.get(j, f);

						double yuj = items.contains(j) ? 1.0 : 0.0;
						double sgd = yuj * g(-fuj) * puf - regI * qjf;
						double sum = 0;
						int cnt = 0;
						for (int k : items) {
							if (k == j)
								continue;

							double fuk = predict(u, k);
							double x = fuk - fuj;

							sgd += gd(-x) * (1.0 / (1 - g(x)) - 1.0 / (1 - g(-x))) * puf;

							double qkf = Q.get(k, f);
							double sji = DenseMat.rowMult(Q, j, Q, k);

							if (sji > minSim) {
								double sgd_d = 2 * (1 - sji) * (qjf - qkf) - qkf * Math.pow(qjf - qkf, 2);
								sum += 0.5 * alpha * sgd_d;
								cnt++;
							}
						}

						jSgds.add(sgd + cnt > 0 ? sum / Math.sqrt(cnt) : 0);
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

					if (items.contains(j)) {
						double fuj = predict(u, j);
						double ruj = trainMatrix.get(u, j);

						errs += (ruj - fuj) * (ruj - fuj);
						loss += Math.log(g(fuj));

						double sum_all = 0;
						int cnt = 0;
						for (int i : items) {
							double fui = predict(u, i);
							loss += Math.log(1 - g(fui - fuj));

							double sji = DenseMat.rowMult(Q, j, Q, i);

							if (sji > minSim) {
								double sum = 0;
								for (int f = 0; f < numFactors; f++)
									sum += Math.pow(Q.get(j, f) - Q.get(i, f), 2);

								sum_all += 0.5 * alpha * (1 - sji) * sum;
								cnt++;
							}
						}
						if (cnt > 0)
							loss += sum_all / Math.sqrt(cnt);
					}

					for (int f = 0; f < numFactors; f++) {
						double puf = P.get(u, f);
						double qjf = Q.get(j, f);

						loss += -0.5 * (regU * puf * puf + regI * qjf * qjf);
					}
				}

			}
			errs *= 0.5;

			if (postEachIter(iter))
				break;

		}// end of training

	}

	@Override
	public String toString() {
		return super.toString() + "," + (float) alpha + "," + isPosOnly;
	}
}
