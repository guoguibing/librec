package lib.rec.ext;

import happy.coding.math.Randoms;

import java.util.ArrayList;
import java.util.List;

import lib.rec.MatrixUtils;
import lib.rec.core.RegSVD;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class BaseNM extends RegSVD {

	protected FlexCompRowMatrix itemCorrs;
	protected boolean isPosOnly;
	protected double minSim;

	public BaseNM(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BaseNM";

		isPosOnly = cf.isOn("is.similarity.pos");
		minSim = isPosOnly ? 0.0 : Double.MIN_VALUE;
	}

	@Override
	public void initModel() {

		// user, item biases
		userBiases = new DenseVector(numUsers);
		itemBiases = new DenseVector(numItems);

		MatrixUtils.init(userBiases, initMean, initStd);
		MatrixUtils.init(itemBiases, initMean, initStd);

		// item correlation matrix
		itemCorrs = new FlexCompRowMatrix(numItems, numItems);
		for (int i = 0; i < numItems; i++) {
			itemCorrs.set(i, i, 0.0);

			for (int j = i + 1; j < numItems; j++) {
				double val = isPosOnly ? Randoms.uniform(0.0, 0.01) : Randoms.gaussian(initMean, initStd);
				itemCorrs.set(i, j, val);
			}
		}
	}

	@Override
	public void buildModel() {
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

				// a set of similar items
				SparseVector uv = MatrixUtils.row(trainMatrix, u, j);
				List<Integer> items = new ArrayList<>();
				for (int i : uv.getIndex()) {
					if (MatrixUtils.get(itemCorrs, j, i) > minSim)
						items.add(i);
				}
				double w = Math.sqrt(items.size());

				// obtain the prediction
				double bu = userBiases.get(u), bj = itemBiases.get(j);
				double pred = globalMean + bu + bj;

				double sum_sji = 0;
				for (int i : items) {
					double sji = MatrixUtils.get(itemCorrs, j, i);
					double rui = trainMatrix.get(u, i);
					double bui = globalMean + bu + itemBiases.get(i);

					pred += sji * (rui - bui) / w;
					sum_sji += sji / w;
				}

				double euj = ruj - pred;
				errs += euj * euj;
				loss += euj * euj;

				// update similarity frist since bu and bj are used here
				for (int i : items) {
					double sji = MatrixUtils.get(itemCorrs, j, i);
					double rui = trainMatrix.get(u, i);
					double bui = globalMean + bu + itemBiases.get(i);

					double delta = lRate * (euj * (rui - bui) / w - regU * sji);
					MatrixUtils.add(itemCorrs, j, i, delta);

					loss += regU * sji * sji;
				}

				// update factors
				double sgd = euj * (1 - sum_sji) - regU * bu;
				userBiases.add(u, lRate * sgd);
				loss += regU * bu * bu;

				sgd = euj * (1 - sum_sji) - regI * bj;
				itemBiases.add(j, lRate * sgd);
				loss += regI * bj * bj;

			}

			errs *= 0.5;
			loss *= 0.5;

			if (postEachIter(iter))
				break;

		}// end of training

	}

	@Override
	protected double predict(int u, int j) {
		double bu = userBiases.get(u);
		double pred = globalMean + bu + itemBiases.get(j);

		// get a number of similar items except item j
		SparseVector uv = MatrixUtils.row(trainMatrix, u, j);
		int[] items = uv.getIndex();

		int k = 0;
		double sum = 0;
		for (int i : items) {
			double sji = MatrixUtils.get(itemCorrs, j, i);

			if (sji > minSim) {
				double rui = trainMatrix.get(u, i);
				double bui = globalMean + bu + itemBiases.get(i);

				sum += sji * (rui - bui);
				k++;
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
