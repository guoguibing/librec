// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.ranking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;
import librec.util.Lists;
import librec.util.Randoms;
import librec.util.Stats;
import librec.util.Strings;

/**
 * 
 * AoBPR: BPR with Adaptive Oversampling<br>
 * 
 * Rendle and Freudenthaler, <strong>Improving pairwise learning for item recommendation from implicit
 * feedback</strong>, WSDM 2014.
 * 
 * @author zhouge
 * 
 */
public class AoBPR extends IterativeRecommender {

	private static int loopNumber;
	private static int lamda_Item;

	private double[] var;
	private int[][] factorRanking;
	private double[] RankingPro;

	public AoBPR(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
		initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		//set for this alg
		lamda_Item = (int) (algoOptions.getFloat("-lambda") * numItems);
		//lamda_Item=500;
		loopNumber = (int) (numItems * Math.log(numItems));

		var = new double[numFactors];
		factorRanking = new int[numFactors][numItems];

		RankingPro = new double[numItems];
		double sum = 0;
		for (int i = 0; i < numItems; i++) {
			RankingPro[i] = Math.exp(-(i + 1) / lamda_Item);
			sum += RankingPro[i];
		}
		for (int i = 0; i < numItems; i++) {
			RankingPro[i] /= sum;
		}
	}

	@Override
	protected void buildModel() throws Exception {
		int countIter = 0;

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			for (int s = 0, smax = numUsers * 100; s < smax; s++) {

				//update Ranking every |I|log|I| 
				if (countIter % loopNumber == 0) {
					updateRankingInFactor();
					countIter = 0;
				}
				countIter++;

				// randomly draw (u, i, j)
				int u = 0, i = 0, j = 0;

				while (true) {
					//random draw an u and i by uniformly
					u = Randoms.uniform(numUsers);
					SparseVector pu = trainMatrix.row(u);
					if (pu.getCount() == 0)
						continue;
					int[] is = pu.getIndex();
					i = is[Randoms.uniform(is.length)];

					do {
						//randoms get a r by exp(-r/lamda)
						int randomJIndex = 0;
						do {
							randomJIndex = Randoms.discrete(RankingPro);
						} while (randomJIndex > numItems);

						//randoms get a f by p(f|c)
						double[] pfc = new double[numFactors];
						double sumfc = 0;
						for (int index = 0; index < numFactors; index++) {
							double temp = Math.abs(P.get(u, index));
							sumfc += temp * var[index];
							pfc[index] = temp * var[index];
						}
						for (int index = 0; index < numFactors; index++) {
							pfc[index] /= sumfc;
						}
						int f = Randoms.discrete(pfc);

						//get the r-1 in f item
						if (P.get(u, f) > 0) {
							j = factorRanking[f][randomJIndex];
						} else {
							j = factorRanking[f][numItems - randomJIndex - 1];
						}
					} while (pu.contains(j));

					break;
				}

				// update parameters
				double xui = predict(u, i);
				double xuj = predict(u, j);
				double xuij = xui - xuj;

				double vals = -Math.log(g(xuij));
				loss += vals;

				double cmg = g(-xuij);

				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qif = Q.get(i, f);
					double qjf = Q.get(j, f);

					P.add(u, f, lRate * (cmg * (qif - qjf) - regU * puf));
					Q.add(i, f, lRate * (cmg * puf - regI * qif));
					Q.add(j, f, lRate * (cmg * (-puf) - regI * qjf));

					loss += regU * puf * puf + regI * qif * qif + regI * qjf * qjf;
				}
			}

			if (isConverged(iter))
				break;

		}
	}

	public void updateRankingInFactor() {
		//echo for each factors
		for (int factorIndex = 0; factorIndex < numFactors; factorIndex++) {
			DenseVector factorVector = Q.column(factorIndex).clone();
			List<Entry<Integer, Double>> sort = sortByDenseVectorValue(factorVector);
			double[] valueList = new double[numItems];
			for (int i = 0; i < numItems; i++) {
				factorRanking[factorIndex][i] = sort.get(i).getKey();
				valueList[i] = sort.get(i).getValue();
			}
			//get 
			var[factorIndex] = Stats.var(valueList);
		}
	}

	public List<Entry<Integer, Double>> sortByDenseVectorValue(DenseVector vector) {
		Map<Integer, Double> keyValPair = new HashMap<>();
		for (int i = 0, length = vector.getData().length; i < length; i++) {
			keyValPair.put(i, vector.get(i));
		}
		return Lists.sortMap(keyValPair, true);
	}

	@Override
	public String toString() {
		return Strings
				.toString(new Object[] { binThold, numFactors, initLRate, regU, regI, numIters, lamda_Item }, ",");
	}
}
