package lib.rec.baseline;

import java.util.HashMap;
import java.util.Map;

import lib.rec.MatrixUtils;
import lib.rec.Recommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Ranking-based Baseline: items are weighted by the number of ratings they
 * received.
 * 
 * @author guoguibing
 * 
 */
public class MostPopular extends Recommender {

	private Map<Integer, Integer> itemPops;

	public MostPopular(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		// force to set as the ranking prediction method
		isRankingPred = true;
		algoName = "MostPop";
		itemPops = new HashMap<>();
	}

	@Override
	protected double ranking(int u, int j) {
		if (!itemPops.containsKey(j)) {
			int numRates = MatrixUtils.col(trainMatrix, j).getUsed();
			itemPops.put(j, numRates);
		}
		return itemPops.get(j);
	}

}
