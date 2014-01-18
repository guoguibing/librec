package lib.rec.baseline;

import java.util.HashMap;
import java.util.Map;

import lib.rec.data.SparseMat;
import lib.rec.intf.Recommender;

/**
 * Ranking-based Baseline: items are weighted by the number of ratings they
 * received.
 * 
 * @author guoguibing
 * 
 */
public class MostPopular extends Recommender {

	private Map<Integer, Integer> itemPops;

	public MostPopular(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		// force to set as the ranking prediction method
		isRankingPred = true;
		algoName = "MostPop";
		itemPops = new HashMap<>();
	}

	@Override
	protected double ranking(int u, int j) {
		if (!itemPops.containsKey(j)) {
			int numRates = trainMatrix.col(j).getUsed();
			itemPops.put(j, numRates);
		}
		return itemPops.get(j);
	}

}
