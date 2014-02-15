package librec.ongoing;

import java.util.ArrayList;
import java.util.List;

import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.main.RecUtils;

public class DNM extends BaseNM {

	// diversity parameter
	private double alpha;

	public DNM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "DNM";
		alpha = RecUtils.getMKey(params, "val.diverse.alpha");
	}

	@Override
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

				// a set of rated and similar items
				SparseVector uv = trainMatrix.row( u, j);
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
					double bi = itemBiases.get(i);
					double bui = globalMean + bu + bi;

					pred += sji * (rui - bui) / w;
					sum_sji += sji / w;

					loss += -alpha * (1 - sji) * Math.pow(bj - bi, 2) / w;
				}

				double euj = ruj - pred;
				errs += euj * euj;
				loss += euj * euj;

				// update similarity
				for (int i : items) {
					double sji = itemCorrs.get(j, i);
					double rui = uv.get(i);
					double bi = itemBiases.get(i);
					double bui = globalMean + bu + bi;

					double delta = lRate * (euj * (rui - bui) / w - 0.5 * alpha * Math.pow(bj - bi, 2) - regU * sji);
					itemCorrs.set(j, i, delta);

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
	public String toString() {
		return super.toString() + "," + (float) alpha;
	}

}
