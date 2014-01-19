package lib.rec.ext;

import java.util.ArrayList;
import java.util.List;

import lib.rec.data.DenseMat;
import lib.rec.data.DenseVec;
import lib.rec.data.SparseMat;
import lib.rec.data.SparseVec;
import lib.rec.intf.IterativeRecommender;
import no.uib.cipr.matrix.MatrixEntry;

public class BaseMF extends IterativeRecommender {

	protected boolean isPosOnly;
	protected double minSim;

	public BaseMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BaseMF";

		isPosOnly = cf.isOn("is.similarity.pos");
		minSim = isPosOnly ? 0.0 : Double.NEGATIVE_INFINITY;
	}

	@Override
	protected void initModel() {

		// re-use it as another item-factor matrix
		P = new DenseMat(numItems, numFactors);
		Q = new DenseMat(numItems, numFactors);

		// initialize model
		if (isPosOnly) {
			P.init();
			Q.init();
		} else {
			P.init(initMean, initStd);
			Q.init(initMean, initStd);
		}

		// set to 0 for items without any ratings
		for (int j = 0, jm = numItems; j < jm; j++) {
			if (trainMatrix.col(j).getUsed() == 0) {
				P.setRow(j, 0.0);
				Q.setRow(j, 0.0);
			}
		}

		userBiases = new DenseVec(numUsers);
		itemBiases = new DenseVec(numItems);

		// initialize user bias
		userBiases.init(initMean, initStd);
		itemBiases.init(initMean, initStd);
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

				double ruj = me.get(); // rate
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
				SparseVec uv = trainMatrix.row(u, j);
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

					double sum = 0.0;
					for (int i : items)
						sum += Q.get(i, f);

					sgds[f] = euj * (w > 0.0 ? sum / w : 0.0) - regU * pjf;

					loss += regU * pjf * pjf;
				}

				// update Q's factors
				for (int i : items) {
					for (int f = 0; f < numFactors; f++) {
						double pjf = P.get(j, f);
						double qif = Q.get(i, f);

						sgd = euj * pjf - regI * qif;
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
	protected double predict(int u, int j) {

		double pred = userBiases.get(u) + itemBiases.get(j);

		int k = 0;
		double sum = 0.0f;
		SparseVec uv = trainMatrix.row(u);
		for (int i : uv.getIndex()) {
			if (i != j) {
				double sji = DenseMat.rowMult(P, j, Q, i);
				if (sji > minSim) {
					sum += sji;
					k++;
				}
			}
		}
		if (k > 0)
			pred += sum / Math.sqrt(k);

		return pred;
	}

	@Override
	public String toString() {
		return super.toString() + "," + isPosOnly;
	}

}
