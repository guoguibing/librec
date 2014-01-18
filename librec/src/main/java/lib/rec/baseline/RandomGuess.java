package lib.rec.baseline;

import happy.coding.math.Randoms;
import lib.rec.data.SparseMat;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by a random value in (minRate, maxRate)
 * 
 * @author guoguibing
 *
 */
public class RandomGuess extends Recommender {

	public RandomGuess(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "Random";
	}

	@Override
	protected double predict(int u, int j) {
		return Randoms.uniform(minRate, maxRate);
	}

}
