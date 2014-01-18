package lib.rec.ext;

import java.util.ArrayList;
import java.util.List;

import lib.rec.RecUtils;
import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DMF extends BaseMF {

	// diversity parameter
	private double alpha;

	public DMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "DMF";
		alpha = RecUtils.getMKey(params, "val.diverse.alpha");
	}

	@Override
	protected void buildModel() {
		last_loss = 0;

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

				// update bias factors
				double bu = userBiases.get(u);
				double sgd = euj - regU * bu;
				userBiases.add(u, lRate * sgd);

				loss += regU * bu * bu;

				double bj = itemBiases.get(j);
				sgd = euj - regI * bj;
				itemBiases.add(j, lRate * sgd);

				loss += regI * bj * bj;

				// rated items by user u
				SparseVector uv = trainMatrix.row(u, j);
				List<Integer> items = new ArrayList<>();
				for (int i : uv.getIndex()) {
					if (i != j) {
						double sji = DenseMat.rowMult(P, j, Q, i);
						if (sji > minSim)
							items.add(i);
					}
				}
				double w = Math.sqrt(items.size());

				// compute P's gradients
				double[] sgds = new double[numFactors];
				for (int f = 0; f < numFactors; f++) {
					double pjf = P.get(j, f);
					sgds[f] = -regU * pjf;

					double sum_q = 0.0, sum_s = 0.0;
					for (int i : items) {
						double qif = Q.get(i, f);
						double pif = P.get(i, f);
						sum_q += qif;

						double sji = DenseMat.rowMult(P, j, Q, i);
						sum_s += 2 * (1 - sji) * (pjf - pif) - qif * Math.pow(pjf - pif, 2);
					}

					if (w > 0)
						sgds[f] += euj * (sum_q / w) + 0.5 * alpha * (sum_s / w);

					loss += regU * pjf * pjf;
				}

				// update Q's factors
				for (int i : items) {
					for (int f = 0; f < numFactors; f++) {
						double pjf = P.get(j, f);
						double qif = Q.get(i, f);

						sgd = euj * pjf - regI * qif;

						sgd += -0.5 * alpha * pjf * Math.pow(pjf - P.get(i, f), 2);
						Q.add(i, f, lRate * sgd);

						loss += regI * qif * qif;
					}
				}

				// update P's factors
				for (int f = 0; f < numFactors; f++)
					P.add(j, f, lRate * sgds[f]);

			}

			errs *= 0.5;
			loss *= 0.5;

			if (postEachIter(iter))
				break;

		}// end of training

	}

	@Override
	public String toString() {
		return super.toString() + "," + (float) alpha;
	}

}
