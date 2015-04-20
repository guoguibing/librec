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

package librec.ranking;

import happy.coding.io.Strings;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.DiagMatrix;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.data.VectorEntry;
import librec.intf.IterativeRecommender;

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
public class WRMF extends IterativeRecommender {
	private float alpha;
	// To be consistent with the symbols in the paper
	DenseMatrix X, Y;
	private static ForkJoinPool forkJoinPool;

	public WRMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true; // item recommendation

		alpha = algoOptions.getFloat("-alpha");

		// paramter is the number of core to used 
		forkJoinPool = new ForkJoinPool(4);
		// checkBinary();
	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {
			forkJoinPool.invoke(new userTask());
			if (isConverged(iter))
				break;
		}

	}

	protected class userTask extends RecursiveAction {

		private static final long serialVersionUID = 1L;

		@Override
		protected void compute() {
			// Step 1: update user factors;
			DenseMatrix Yt = Y.transpose();
			DenseMatrix YtY = Yt.mult(Y);
			// run in parallel
			List<RecursiveAction> forks = new LinkedList<>();
			for (int u = 0; u < numUsers; u++) {
				// for each test user
				UpdateUser temp = new UpdateUser(u, Yt, YtY);
				forks.add(temp);
				temp.fork();

			}
			for (RecursiveAction recursiveAction : forks) {
				recursiveAction.join();
			}

			// Step 2: update item factors;
			DenseMatrix Xt = X.transpose();
			DenseMatrix XtX = Xt.mult(X);
			for (int i = 0; i < numItems; i++) {
				// for each test user
				updateItem temp = new updateItem(i, Xt, XtX);
				forks.add(temp);
				temp.fork();
			}
			for (RecursiveAction recursiveAction : forks) {
				recursiveAction.join();
			}
		}
	}

	private class UpdateUser extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		private final int u;
		private DenseMatrix Yt;
		private DenseMatrix YtY;

		UpdateUser(int u, DenseMatrix Yt, DenseMatrix YtY) {
			this.u = u;
			this.Yt = Yt;
			this.YtY = YtY;
		}

		@Override
		protected void compute() {
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
			DenseMatrix YtCuY = YtY.add(Yt.mult(CuI,Y));
			// (YtCuY + lambda * I)^-1
			DenseMatrix Wu = (YtCuY.add(DiagMatrix.eye(numFactors).scale(regU))).inv();
			// Yt * Cu
			DenseVector YtCuPu = Yt.mult(Cu, pu);

			DenseVector xu = Wu.mult(YtCuPu);

			// udpate user factors
			X.setRow(u, xu);
		}
	}

	private class updateItem extends RecursiveAction {

		private static final long serialVersionUID = 1L;
		private final int i;
		private DenseMatrix Xt;
		private DenseMatrix XtX;

		updateItem(int i, DenseMatrix Xt, DenseMatrix XtX) {
			this.i = i;
			this.Xt = Xt;
			this.XtX = XtX;
		}

		@Override
		protected void compute() {
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
			DiagMatrix CiI = Ci.minus(1); // more efficient than DiagMatrix.eye(numUsers)
			// XtX + Xt * (Ci - I) * X
			DenseMatrix XtCiX = XtX.add(Xt.mult(CiI,X));
			// (XtCiX + lambda * I)^-1
			DenseMatrix Wi = (XtCiX.add(DiagMatrix.eye(numFactors).scale(regI))).inv();
			// Xt * Ci
			DenseVector XtCiPi = Xt.mult(Ci, pi);

			DenseVector yi = Wi.mult(XtCiPi);

			// udpate item factors
			Y.setRow(i, yi);
		}
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { binThold, alpha, numFactors, regU, regI, numIters }, ",");
	}

}
