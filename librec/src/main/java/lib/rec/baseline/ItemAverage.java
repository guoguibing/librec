package lib.rec.baseline;

import happy.coding.math.Stats;

import java.util.HashMap;
import java.util.Map;

import lib.rec.data.SparseMat;
import lib.rec.data.SparseVec;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by the average of target item's ratings
 * 
 * @author guoguibing
 *
 */
public class ItemAverage extends Recommender {

	private Map<Integer, Double> itemMeans;

	public ItemAverage(SparseMat rm, SparseMat tm, int fold) {
		super(rm, tm, fold);

		itemMeans = new HashMap<>();
		algoName = "ItemAvg";
	}

	@Override
	protected double predict(int u, int j) {
		if (itemMeans.containsKey(j))
			return itemMeans.get(j);

		SparseVec jv = trainMatrix.col(j);
		int numRated = jv.getUsed();
		double itemMean = numRated > 0 ? Stats.sum(jv.getData()) / numRated : globalMean;
		itemMeans.put(j, itemMean);

		return itemMean;
	}
}
