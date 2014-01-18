package lib.rec.baseline;

import happy.coding.io.KeyValPair;
import happy.coding.io.Lists;
import happy.coding.math.Stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lib.rec.MatrixUtils;
import lib.rec.intf.Recommender;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class ItemKNN extends Recommender {

	// user: nearest neighborhood
	private FlexCompRowMatrix itemCorrs;
	private DenseVector itemMeans;
	private int knn;

	public ItemKNN(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "ItemKNN";
		knn = cf.getInt("num.neighbors");
	}

	@Override
	protected void initModel() {
		itemCorrs = buildCorrs(false);
		itemMeans = new DenseVector(numItems);
		for (int i = 0; i < numItems; i++) {
			SparseVector vs = MatrixUtils.col(trainMatrix, i);
			double mean = vs.getUsed() > 0 ? Stats.sum(vs.getData()) / vs.getUsed() : globalMean;
			itemMeans.set(i, mean);
		}
	}

	@Override
	protected double predict(int u, int j) {

		// find a number of similar users
		Map<Integer, Double> nns = new HashMap<>();

		SparseVector dv = MatrixUtils.nn(itemCorrs, j);
		for (int i : dv.getIndex()) {
			double sim = dv.get(i);
			double rate = trainMatrix.get(u, i);

			if (sim > 0 && rate > 0)
				nns.put(i, sim);
		}

		// topN similar users
		if (knn > 0 && knn < nns.size()) {
			List<KeyValPair<Integer>> sorted = Lists.sortMap(nns, true);
			List<KeyValPair<Integer>> subset = sorted.subList(0, knn);
			nns.clear();
			for (KeyValPair<Integer> kv : subset)
				nns.put(kv.getKey(), kv.getVal().doubleValue());
		}

		if (nns.size() == 0)
			return globalMean;

		double sum = 0, ws = 0;
		for (Entry<Integer, Double> en : nns.entrySet()) {
			int i = en.getKey();
			double sim = en.getValue();
			double rate = trainMatrix.get(u, i);

			sum += sim * (rate - itemMeans.get(i));
			ws += Math.abs(sim);
		}

		return ws > 0 ? itemMeans.get(j) + sum / ws : globalMean;
	}

	@Override
	public String toString() {
		return super.toString() + "," + knn + "," + cf.getString("similarity") + "," + cf.getInt("num.shrinkage");
	}
}
