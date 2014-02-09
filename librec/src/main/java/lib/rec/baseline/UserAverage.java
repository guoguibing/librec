package lib.rec.baseline;

import java.util.HashMap;
import java.util.Map;

import lib.rec.data.SparseMatrix;
import lib.rec.data.SparseVector;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by the average of target user's ratings
 * 
 * @author guoguibing
 * 
 */
public class UserAverage extends Recommender {

	private Map<Integer, Double> userMeans;

	public UserAverage(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "UserAvg";
		userMeans = new HashMap<>();
	}

	@Override
	protected double predict(int u, int j) {
		if (!userMeans.containsKey(u)) {
			SparseVector uv = trainMatrix.row(u);
			userMeans.put(u, uv.getCount() > 0 ? uv.mean() : globalMean);
		}

		return userMeans.get(u);
	}
}
