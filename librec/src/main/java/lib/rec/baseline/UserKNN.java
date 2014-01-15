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

public class UserKNN extends Recommender {

	// user: nearest neighborhood
	private FlexCompRowMatrix userCorrs;
	private DenseVector userMeans;
	private int knn;

	public UserKNN(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "UserKNN";
		knn = cf.getInt("num.neighbors");
	}

	@Override
	protected void initModel() {
		userCorrs = buildCorrs(true);
		userMeans = new DenseVector(numUsers);
		for (int u = 0; u < numUsers; u++) {
			SparseVector vs = MatrixUtils.row(trainMatrix, u);
			double mean = vs.getUsed() > 0 ? Stats.sum(vs.getData()) / vs.getUsed() : globalMean;
			userMeans.set(u, mean);
		}
	}

	@Override
	protected double predict(int u, int j) {

		// find a number of similar users
		Map<Integer, Double> nns = new HashMap<>();

		SparseVector dv = userCorrs.getRow(u);
		for (int v : dv.getIndex()) {
			double sim = dv.get(v);
			double rate = trainMatrix.get(v, j);

			if (sim > 0 && rate > 0)
				nns.put(v, sim);
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

		// mean rate of user u
		double sum = 0, ws = 0;
		for (Entry<Integer, Double> en : nns.entrySet()) {
			int v = en.getKey();
			double sim = en.getValue();
			double rate = trainMatrix.get(v, j);

			sum += sim * (rate - userMeans.get(v));
			ws += Math.abs(sim);
		}

		return ws > 0 ? userMeans.get(u) + sum / ws : globalMean;
	}

	@Override
	public String toString() {
		return super.toString() + "," + knn + "," + cf.getString("similarity") + "," + cf.getInt("num.shrinkage");
	}
}
