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
import librec.intf.IterativeRecommender;

/**
 * Ruslan Salakhutdinov and Andriy Mnih, <strong>Probabilistic Matrix Factorization</strong>, NIPS 2008. <br/>
 * 
 * @author guoguibing
 * 
 */
public class PMF extends IterativeRecommender {

	protected DenseMatrix userDeltas, itemDeltas;

	public PMF(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		// disable bold driver
		isBoldDriver = false;

		// initByNorm = false;
	}

	@Override
	protected void initModel() throws Exception {
		super.initModel();

		userDeltas = new DenseMatrix(numUsers, numFactors);
		itemDeltas = new DenseMatrix(numItems, numFactors);
	}

	@Override
	protected void postModel() throws Exception {
		userDeltas = null;
		itemDeltas = null;
	}

	@Override
	protected void buildModel() throws Exception {

		// batch updates with momentums
		for (int iter = 1; iter <= numIters; iter++) {

			DenseMatrix userSgds = new DenseMatrix(numUsers, numFactors);
			DenseMatrix itemSgds = new DenseMatrix(numItems, numFactors);
			loss = 0;
			errs = 0;

			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int j = me.column();
				double rate = me.get();
				
				double pred = predict(u, j);
				double euj = rate - pred;
				loss += euj * euj;
				errs += euj * euj;

				for (int f = 0; f < numFactors; f++) {
					double qjf = Q.get(j, f);
					double puf = P.get(u, f);

					double sgd_u = 2 * euj * qjf - regU * puf;
					double sgd_j = 2 * euj * puf - regI * qjf;

					userSgds.add(u, f, sgd_u);
					itemSgds.add(j, f, sgd_j);

					loss += regU * puf * puf + regI * qjf * qjf;
				}
			}
			errs /= numRates;
			loss /= numRates;

			userDeltas = userDeltas.scale(momentum).add(userSgds.scale(lRate / numRates));
			itemDeltas = itemDeltas.scale(momentum).add(itemSgds.scale(lRate / numRates));

			P = P.add(userDeltas);
			Q = Q.add(itemDeltas);

			if (isConverged(iter))
				break;
		}
	}

}
