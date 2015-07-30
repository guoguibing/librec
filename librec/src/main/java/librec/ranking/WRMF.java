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

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.DiagMatrix;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.IterativeRecommender;
import librec.util.Logs;
import librec.util.Strings;

/**
 * <h3>WRMF: Weighted Regularized Matrix Factorization.</h3>
 * 
 * This implementation refers to the method proposed by Hu et al. at ICDM 2008.
 * 
 * <ul>
 * <li><strong>Binary ratings:</strong> Pan et al., One-class Collaborative Filtering, ICDM 2008.</li>
 * <li><strong>Real ratings:</strong> Hu et al., Collaborative filtering for implicit feedback datasets, ICDM 2008.</li>
 * </ul>
 * 
 * @author guoguibing
 * 
 */
@Configuration("binThold, alpha, factors, regU, regI, numIters")
public class WRMF extends IterativeRecommender {

	private float alpha;

	public WRMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true; // item recommendation

		alpha = algoOptions.getFloat("-alpha");
		// checkBinary();
	}

	@Override
	protected void buildModel() throws Exception {

		// To be consistent with the symbols in the paper
		DenseMatrix X = P, Y = Q;

		// Updating by using alternative least square (ALS)
		// due to large amount of entries to be processed (SGD will be too slow)
		for (int iter = 1; iter <= numIters; iter++) {

			// Step 1: update user factors;
			DenseMatrix Yt = Y.transpose();
			DenseMatrix YtY = Yt.mult(Y);
			for (int u = 0; u < numUsers; u++) {
				if (verbose && (u + 1) % 200 == 0)
					Logs.debug("{}{} runs at iteration = {}, user = {}/{}", algoName, foldInfo, iter, u + 1, numUsers);

				// diagonal matrix C^u for each user
				DiagMatrix Cu = DiagMatrix.eye(numItems); // all entries on the diagonal will be 1
				SparseVector pu = trainMatrix.row(u);

				for (VectorEntry ve : pu) {
					int i = ve.index();
					Cu.add(i, i, alpha * ve.get()); // changes some entries to 1 + alpha * r_{u, i}
				}

				// binarize real values
				for (VectorEntry ve : pu)
					ve.set(ve.get() > 0 ? 1 : 0);

				// Cu - I
				DiagMatrix CuI = Cu.minus(1);
				// YtY + Yt * (Cu - I) * Y
				DenseMatrix YtCuY = YtY.add(Yt.mult(CuI).mult(Y));
				// (YtCuY + lambda * I)^-1
				DenseMatrix Wu = (YtCuY.add(DiagMatrix.eye(numFactors).scale(regU))).inv();
				// Yt * Cu
				DenseMatrix YtCu = Yt.mult(Cu);

				DenseVector xu = Wu.mult(YtCu).mult(pu);

				// udpate user factors
				X.setRow(u, xu);
			}

			// Step 2: update item factors;
			DenseMatrix Xt = X.transpose();
			DenseMatrix XtX = Xt.mult(X);
			for (int i = 0; i < numItems; i++) {
				if (verbose && (i + 1) % 200 == 0)
					Logs.debug("{}{} runs at iteration = {}, item = {}/{}", algoName, foldInfo, iter, i + 1, numItems);

				// diagonal matrix C^i for each item
				DiagMatrix Ci = DiagMatrix.eye(numUsers);
				SparseVector pi = trainMatrix.column(i);

				for (VectorEntry ve : pi) {
					int u = ve.index();
					Ci.add(u, u, alpha * ve.get());
				}

				// binarize real values
				for (VectorEntry ve : pi)
					ve.set(ve.get() > 0 ? 1 : 0);

				// Ci - I
				DiagMatrix CiI = Ci.minus(1); // more efficient than
												// DiagMatrix.eye(numUsers)
				// XtX + Xt * (Ci - I) * X
				DenseMatrix XtCiX = XtX.add(Xt.mult(CiI).mult(X));
				// (XtCiX + lambda * I)^-1
				DenseMatrix Wi = (XtCiX.add(DiagMatrix.eye(numFactors).scale(regI))).inv();
				// Xt * Ci
				DenseMatrix XtCi = Xt.mult(Ci);

				DenseVector yi = Wi.mult(XtCi).mult(pi);

				// udpate item factors
				Y.setRow(i, yi);
			}
		}
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, alpha, numFactors, regU, regI, numIters }, ",");
	}

}
