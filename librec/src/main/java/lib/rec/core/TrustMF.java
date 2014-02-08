package lib.rec.core;

import lib.rec.data.DenseMatrix;
import lib.rec.data.SparseMatrix;
import lib.rec.intf.SocialRecommender;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI
 * 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialRecommender {

	protected DenseMatrix B, W;

	public TrustMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "TrustMF";
	}

	@Override
	protected void initModel() {
		super.initModel();

		B = new DenseMatrix(numUsers, numFactors);
		W = new DenseMatrix(numUsers, numFactors);

		B.init(initMean, initStd);
		W.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0)
				B.setRow(u, 0.0);

			if (socialMatrix.colSize(u) == 0)
				W.setRow(u, 0.0);
		}
	}

	@Override
	protected void buildModel() {
		// TODO Auto-generated method stub
		super.buildModel();
	}

	/**
	 * Build TrusterMF model
	 */
	protected void TrusterMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			// 
		}
	}

	/**
	 * Build TrusteeMF model
	 */
	protected void TrusteeMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			// 
		}
	}

	@Override
	protected double predict(int u, int j) {
		double pred = DenseMatrix.rowMult(P, u, Q, j);
		return minRate + g(pred) * (maxRate - minRate);
	}

}
