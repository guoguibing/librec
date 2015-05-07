// Copyright (C) 2014 Guibing Guo
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

package librec.ext;

import happy.coding.io.Lists;

import java.util.HashMap;
import java.util.Map;

import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.Recommender;

/**
 * Pennock et al., <strong>Collaborative Filtering by Personality Diagnosis: A Hybrid Memory- and Model-based
 * Approach</strong>, UAI 2000.
 * 
 * <p>
 * Related Work:
 * <ul>
 * <a href= "http://www.cs.carleton.edu/cs_comps/0607/recommend/recommender/pd.html">A brief introduction to Personality
 * Diagnosis</a></li>
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class PD extends Recommender {

	// Gaussian noise: 2.5 suggested in the paper
	private float sigma;

	// prior probability
	private double prior;

	public PD(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		prior = 1.0 / numUsers;
		sigma = algoOptions.getFloat("-sigma");
	}

	@Override
	protected double predict(int a, int j) {
		Map<Double, Double> scaleProbs = new HashMap<>();
		SparseVector pa = trainMatrix.row(a);
		SparseVector qj = trainMatrix.column(j);

		for (double h : ratingScale) {

			double prob = 0.0;
			for (VectorEntry ve : qj) {
				// other users who rated item j
				int i = ve.index();
				double rij = ve.get();

				SparseVector pi = trainMatrix.row(i);
				double prod = 1.0;
				for (VectorEntry ae : pa) {
					int l = ae.index();
					double ral = ae.get();
					double ril = pi.get(l);
					if (ril > 0)
						prod *= gaussian(ral, ril, sigma);
				}
				prob += gaussian(h, rij, sigma) * prod;
			}

			prob *= prior;
			scaleProbs.put(h, prob);
		}

		return Lists.sortMap(scaleProbs, true).get(0).getKey();
	}

	@Override
	public String toString() {
		return super.toString() + "," + sigma;
	}

}
