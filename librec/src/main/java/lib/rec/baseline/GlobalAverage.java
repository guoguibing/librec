package lib.rec.baseline;

import lib.rec.data.SparseMat;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by average rating of all users
 * 
 * @author guoguibing
 *
 */
public class GlobalAverage extends Recommender {

	public GlobalAverage(SparseMat rm, SparseMat tm, int fold) {
		super(rm, tm, fold);

		this.algoName = "GlobalAvg";
	}

	@Override
	protected double predict(int u, int j) {
		return globalMean;
	}

}
