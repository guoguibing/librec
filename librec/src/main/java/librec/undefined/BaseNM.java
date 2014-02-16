package librec.undefined;

import happy.coding.math.Randoms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.SymmMatrix;
import librec.intf.IterativeRecommender;

public class BaseNM extends IterativeRecommender {

	protected SymmMatrix itemCorrs, last_S;
	protected boolean isPosOnly;
	protected double minSim;

	public BaseNM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BaseNM";

		isPosOnly = cf.isOn("is.similarity.pos");
		minSim = isPosOnly ? 0.0 : Double.NEGATIVE_INFINITY;
	}

	@Override
	protected void updates() {
		super.updates();
		if (itemCorrs != null)
			last_S = itemCorrs.clone();
	}

	@Override
	protected void undos(int iter) {
		super.undos(iter);
		if (last_S != null)
			itemCorrs = last_S.clone();
	}

	@Override
	protected void initModel() {

		// user, item biases
		userBiases = new DenseVector(numUsers);
		itemBiases = new DenseVector(numItems);

		userBiases.init(initMean, initStd);
		itemBiases.init(initMean, initStd);

		// item correlation matrix
		itemCorrs = new SymmMatrix(numItems);

		// ignore items without any training ratings: can greatly reduce memory usage
		Set<Integer> items = new HashSet<>();
		for (int i = 0; i < numItems; i++)
			if (trainMatrix.column(i).getCount() == 0)
				items.add(i);

		for (int i = 0; i < numItems; i++) {
			if (items.contains(i))
				continue;

			itemCorrs.set(i, i, 0.0);

			for (int j = i + 1; j < numItems; j++) {
				if (items.contains(j))
					continue;

				double val = isPosOnly ? Randoms.uniform(0.0, 0.01) : Randoms.gaussian(initMean, initStd);
				itemCorrs.set(i, j, val);
			}
		}
	}

	protected void buildModel() {
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
				SparseVector uv = trainMatrix.row(u, j);
				List<Integer> items = new ArrayList<>();
				for (int i : uv.getIndex()) {
					if (itemCorrs.get(j, i) > minSim)
						items.add(i);
				}
				double w = Math.sqrt(items.size());

				// obtain the prediction
				double bu = userBiases.get(u), bj = itemBiases.get(j);
				double pred = globalMean + bu + bj;

				double sum_sji = 0;
				for (int i : items) {
					double sji = itemCorrs.get(j, i);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					pred += sji * (rui - bui) / w;
					sum_sji += sji / w;
				}

				double euj = ruj - pred;
				errs += euj * euj;
				loss += euj * euj;

				// update similarity frist since bu and bj are used here
				for (int i : items) {
					double sji = itemCorrs.get(j, i);
					double rui = uv.get(i);
					double bui = globalMean + bu + itemBiases.get(i);

					double delta = lRate * (euj * (rui - bui) / w - regU * sji);
					itemCorrs.add(j, i, delta);

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

			if (isConverged(iter))
				break;

		}// end of training
	}

	@Override
	protected double predict(int u, int j) {
		double bu = userBiases.get(u);
		double pred = globalMean + bu + itemBiases.get(j);

		// get a number of similar items except item j
		SparseVector uv = trainMatrix.row(u, j);
		int[] items = uv.getIndex();

		int k = 0;
		double sum = 0;
		for (int i : items) {
			double sji = itemCorrs.get(j, i);

			if (sji != 0 && sji > minSim) {
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
