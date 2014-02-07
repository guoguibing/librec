package lib.rec.core;

import lib.rec.data.DenseMat;
import lib.rec.data.SparseMat;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI
 * 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialMF {

	protected DenseMat B, W;

	public TrustMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "TrustMF";
	}

	@Override
	protected void initModel() {
		super.initModel();

		B = new DenseMat(numUsers, numFactors);
		W = new DenseMat(numUsers, numFactors);

		B.init(initMean, initStd);
		W.init(initMean, initStd);

		for (int u = 0; u < numUsers; u++) {
			if (socialMatrix.rowSize(u) == 0)
				B.setRow(u, 0.0);

			if (trSocialMatrix.rowSize(u) == 0)
				W.setRow(u, 0.0);
		}

	}

	@Override
	protected void buildModel() {
		// TODO Auto-generated method stub
		super.buildModel();
	}

	protected void TrusterMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			// 
		}
	}

	protected void TrusteeMF() {
		for (int iter = 1; iter <= maxIters; iter++) {
			// 
		}
	}

}
