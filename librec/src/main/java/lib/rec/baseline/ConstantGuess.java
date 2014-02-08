package lib.rec.baseline;

import lib.rec.data.SparseMatrix;
import lib.rec.intf.Recommender;

/**
 * Baseline: predict by a constant rating
 * 
 * @author guoguibing
 * 
 */
public class ConstantGuess extends Recommender {

	private double constant;

	public ConstantGuess(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "Constant";
		constant = (minRate + maxRate) / 2.0; //can also use given constant
	}

	@Override
	protected double predict(int u, int j) {
		return constant;
	}

}
