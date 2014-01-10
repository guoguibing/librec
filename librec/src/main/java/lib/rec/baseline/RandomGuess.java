package lib.rec.baseline;

import lib.rec.Recommender;
import happy.coding.math.Randoms;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Baseline: predict by a random value in (minRate, maxRate)
 * 
 * @author guoguibing
 *
 */
public class RandomGuess extends Recommender {

	public RandomGuess(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "Random";
	}

	@Override
	protected double predict(int u, int j) {
		return Randoms.uniform(minRate, maxRate);
	}

}
