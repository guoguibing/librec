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

package librec.rating;

import librec.data.DenseMatrix;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.SocialRecommender;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust propagation for recommendation in social
 * networks</strong>, RecSys 2010.
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		initByNorm = false;
	}

	@Override
	protected void buildModel() throws Exception {
		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;

			DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
			DenseMatrix QS = new DenseMatrix(numItems, numFactors);

			// rated items
			for (MatrixEntry me : trainMatrix) {

				int u = me.row();
				int j = me.column();
				double ruj = me.get();

				double pred = predict(u, j, false);
				double euj = g(pred) - normalize(ruj);

				loss += euj * euj;

				double csgd = gd(pred) * euj;

				for (int f = 0; f < numFactors; f++) {
					PS.add(u, f, csgd * Q.get(j, f) + regU * P.get(u, f));
					QS.add(j, f, csgd * P.get(u, f) + regI * Q.get(j, f));

					loss += regU * P.get(u, f) * P.get(u, f);
					loss += regI * Q.get(j, f) * Q.get(j, f);
				}
			}

			// social regularization
			for (int u = 0; u < numUsers; u++) {
				SparseVector uv = socialMatrix.row(u);
				int numConns = uv.getCount();
				if (numConns == 0)
					continue;

				double[] sumNNs = new double[numFactors];
				for (int v : uv.getIndex()) {
					for (int f = 0; f < numFactors; f++)
						sumNNs[f] += socialMatrix.get(u, v) * P.get(v, f);
				}

				for (int f = 0; f < numFactors; f++) {
					double diff = P.get(u, f) - sumNNs[f] / numConns;
					PS.add(u, f, regS * diff);

					loss += regS * diff * diff;
				}

				// those who trusted user u
				SparseVector iuv = socialMatrix.column(u);
				int numVs = iuv.getCount();
				for (int v : iuv.getIndex()) {
					double tvu = socialMatrix.get(v, u);

					SparseVector vv = socialMatrix.row(v);
					double[] sumDiffs = new double[numFactors];
					for (int w : vv.getIndex()) {
						for (int f = 0; f < numFactors; f++)
							sumDiffs[f] += socialMatrix.get(v, w) * P.get(w, f);
					}

					numConns = vv.getCount();
					if (numConns > 0)
						for (int f = 0; f < numFactors; f++)
							PS.add(u, f, -regS * (tvu / numVs) * (P.get(v, f) - sumDiffs[f] / numConns));
				}
			}

			// update user factors
			P = P.add(PS.scale(-lRate));
			Q = Q.add(QS.scale(-lRate));

			loss *= 0.5;

			if (isConverged(iter))
				break;
		}

	}

	@Override
	protected double predict(int u, int j, boolean bounded) {
		double pred = DenseMatrix.rowMult(P, u, Q, j);

		if (bounded)
			return denormalize(g(pred));

		return pred;
	}

}
