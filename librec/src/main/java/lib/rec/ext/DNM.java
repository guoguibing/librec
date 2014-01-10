package lib.rec.ext;

import happy.coding.io.KeyValPair;
import happy.coding.io.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.rec.MatrixUtils;
import lib.rec.RecUtils;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DNM extends BaseNM {

	// diversity parameter
	private double alpha;

	public DNM(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "DNM";
		alpha = RecUtils.getMKey(params, "val.diverse.alpha");
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

				// a set of rated and similar items
				SparseVector uv = MatrixUtils.row(trainMatrix, u, j);
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
					double rui = trainMatrix.get(u, i);
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
					double rui = trainMatrix.get(u, i);
					double bi = itemBiases.get(i);
					double bui = globalMean + bu + bi;

					double delta = lRate * (euj * (rui - bui) / w - 0.5 * alpha * Math.pow(bj - bi, 2) - regU * sji);
					itemCorrs.add(j, i, delta);
					itemCorrs.add(i, j, delta);

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

	protected List<Integer> ranking2(int u, Collection<Integer> candItems, Collection<Integer> ignoreItems) {

		List<Integer> items = new ArrayList<>();

		Map<Integer, Double> itemScores = new HashMap<>();
		for (Integer j : candItems) {
			if (!ignoreItems.contains(j)) {
				double pred = predict(u, j, true);
				if (pred > Double.MIN_VALUE)
					itemScores.put(j, pred);
			}
		}

		if (itemScores.size() > 0) {

			List<KeyValPair<Integer>> sorted = Lists.sortMap(itemScores, true);

			// first: select the first item with the highest prediction
			items.add(sorted.get(0).getKey());

			// sequentially add other items
			int num = (numRecs <= 0 || sorted.size() < numRecs) ? sorted.size() : numRecs;
			double mmr = 0.15;
			for (int i = 1; i < num; i++) {
				int maxItem = -1;
				double maxScore = Double.MIN_VALUE;

				// the rest of candidate items
				for (KeyValPair<Integer> kv : sorted) {
					int item = kv.getKey();
					double pred = kv.getVal().doubleValue();
					if (!items.contains(item)) {
						double maxSim = Double.MIN_VALUE;
						// find the item with maximum similarity
						for (int j : items) {
							double sji = itemCorrs.get(j, item); //corrs.get(j, item);
							if (sji > maxSim)
								maxSim = sji;
						}
						double score = pred - mmr * maxSim;

						if (score > maxScore) {
							maxScore = score;
							maxItem = item;
						}
					}

				}
				items.add(maxItem);
			}

		}

		return items;
	}

	@Override
	public String toString() {
		return super.toString() + "," + (float) alpha;
	}

}
