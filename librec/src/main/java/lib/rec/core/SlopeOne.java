package lib.rec.core;

import lib.rec.MatrixUtils;
import lib.rec.Recommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * 
 * Weighted Slope One: Lemire and Maclachlan, <strong>Slope One Predictors for
 * Online Rating-Based Collaborative Filtering</strong>, SDM 2005. <br/>
 * 
 * @author guoguibing
 * 
 */
public class SlopeOne extends Recommender {

	// matrices for item-item differences with number of occurrences/cardinary 
	private FlexCompRowMatrix devMatrix, cardMatrix;

	public SlopeOne(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SlopeOne";
	}

	@Override
	protected void initModel() {
		devMatrix = new FlexCompRowMatrix(numItems, numItems);
		cardMatrix = new FlexCompRowMatrix(numItems, numItems);
	}

	@Override
	protected void buildModel() {

		// compute items' differences
		for (int u = 0; u < numUsers; u++) {
			SparseVector uv = MatrixUtils.row(trainMatrix, u);
			int[] items = uv.getIndex();

			for (int i : items) {
				double rui = uv.get(i);
				for (int j : items) {
					if (i != j) {
						double ruj = uv.get(j);
						devMatrix.add(i, j, rui - ruj);
						cardMatrix.add(i, j, 1);
					}
				}
			}
		}

		// normalize differences
		for (int i = 0; i < numItems; i++) {
			for (int j = 0; j < numItems; j++) {
				double card = cardMatrix.get(i, j);
				if (card > 0) {
					double sum = devMatrix.get(i, j);
					devMatrix.set(i, j, sum / card);
				}
			}
		}
	}

	@Override
	protected double predict(int u, int j) {
		SparseVector uv = MatrixUtils.row(trainMatrix, u, j);
		int[] items = uv.getIndex();
		double preds = 0, cards = 0;
		for (int i : items) {
			double card = cardMatrix.get(j, i);
			if (card > 0) {
				preds += (devMatrix.get(j, i) + uv.get(i)) * card;
				cards += card;
			}
		}

		return cards > 0 ? preds / cards : globalMean;
	}

}
