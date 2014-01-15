package lib.rec.baseline;

import lib.rec.intf.Recommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Baseline: predict by average rating of all users
 * 
 * @author guoguibing
 *
 */
public class GlobalAverage extends Recommender {

	public GlobalAverage(CompRowMatrix rm, CompRowMatrix tm, int fold) {
		super(rm, tm, fold);

		this.algoName = "GlobalAvg";
	}

	@Override
	protected double predict(int u, int j) {
		return globalMean;
	}

}
