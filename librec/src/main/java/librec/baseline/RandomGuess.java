package librec.baseline;

import happy.coding.math.Randoms;
import librec.data.SparseMatrix;
import librec.intf.Recommender;

/**
 * Baseline: predict by a random value in (minRate, maxRate)
 * 
 * @author guoguibing
 *
 */
public class RandomGuess extends Recommender {

	public RandomGuess(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "Random";
	}

	@Override
	protected double predict(int u, int j) {
		return Randoms.uniform(minRate, maxRate);
	}

}
