package librec.ext;

import librec.data.DenseMatrix;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.Recommender;

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
	private DenseMatrix devMatrix, cardMatrix;

	public SlopeOne(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "SlopeOne";
	}

	@Override
	protected void initModel() {
		devMatrix = new DenseMatrix(numItems, numItems);
		cardMatrix = new DenseMatrix(numItems, numItems);
	}

	@Override
	protected void buildModel() {

		// compute items' differences
		for (int u = 0; u < numUsers; u++) {
			SparseVector uv = trainMatrix.row(u);
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
		SparseVector uv = trainMatrix.row(u, j);
		double preds = 0, cards = 0;
		for (int i : uv.getIndex()) {
			double card = cardMatrix.get(j, i);
			if (card > 0) {
				preds += (devMatrix.get(j, i) + uv.get(i)) * card;
				cards += card;
			}
		}

		return cards > 0 ? preds / cards : globalMean;
	}

}
