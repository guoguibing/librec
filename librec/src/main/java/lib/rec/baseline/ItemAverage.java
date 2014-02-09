package lib.rec.baseline;

import java.util.HashMap;
import java.util.Map;

import lib.rec.data.SparseMatrix;
import lib.rec.data.SparseVector;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by the average of target item's ratings
 * 
 * @author guoguibing
 * 
 */
public class ItemAverage extends Recommender {

	private Map<Integer, Double> itemMeans;

	public ItemAverage(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		itemMeans = new HashMap<>();
		algoName = "ItemAvg";
	}

	@Override
	protected double predict(int u, int j) {
		if (!itemMeans.containsKey(j)) {
			SparseVector jv = trainMatrix.column(j);
			double mean = jv.getCount() > 0 ? jv.mean() : globalMean;
			itemMeans.put(j, mean);
		}

		return itemMeans.get(j);
	}
}
