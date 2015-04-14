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

import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

/**
 * Regularized SVD
 * 
 * <p>
 * Arkadiusz Paterek, <strong>Improving Regularized Singular Value Decomposition Collaborative Filtering</strong>,
 * Proceedings of KDD Cup and Workshop, 2007.
 * </p>
 * 
 * @author guoguibing
 * 
 */
public class RegSVD extends IterativeRecommender {

	public RegSVD(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			loss = 0;
			errs = 0;
			for (MatrixEntry me : trainMatrix) {

				int u = me.row(); // user
				int j = me.column(); // item
				double ruj = me.get();

				double pred = predict(u, j, false);
				double euj = ruj - pred;

				errs += euj * euj;
				loss += euj * euj;

				// update factors
				for (int f = 0; f < numFactors; f++) {
					double puf = P.get(u, f);
					double qjf = Q.get(j, f);

					double delta_u = euj * qjf - regU * puf;
					double delta_j = euj * puf - regI * qjf;

					P.add(u, f, lRate * delta_u);
					Q.add(j, f, lRate * delta_j);

					loss += regU * puf * puf + regI * qjf * qjf;
				}

			}

			errs *= 0.5;
			loss *= 0.5;

			if (isConverged(iter))
				break;

		}// end of training

	}

}
