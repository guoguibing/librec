package lib.rec.baseline;

import happy.coding.math.Stats;

import java.util.HashMap;
import java.util.Map;

import lib.rec.MatrixUtils;
import lib.rec.intf.Recommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Baseline: predict by the average of target user's ratings
 * 
 * @author guoguibing
 * 
 */
public class UserAverage extends Recommender {

	private Map<Integer, Double> userMeans;

	public UserAverage(CompRowMatrix rm, CompRowMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "UserAvg";
		userMeans = new HashMap<>();
	}

	@Override
	protected double predict(int u, int j) {
		if (userMeans.containsKey(u))
			return userMeans.get(u);

		SparseVector uv = MatrixUtils.row(trainMatrix, u);
		int numRated = uv.getUsed();
		double userMean = numRated > 0 ? Stats.sum(uv.getData()) / numRated : globalMean;
		userMeans.put(u, userMean);

		return userMean;
	}
}
