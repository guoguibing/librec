package lib.rec.core;

import lib.rec.intf.SocialRecommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust
 * propagation for recommendation in social networks</strong>, RecSys 2010.
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold, String path) {
		super(trainMatrix, testMatrix, fold, path);

		algoName = "SocialMF";
	}
	
	@Override
	protected void initModel() {
		super.initModel();
		
		invSocialMatrix = socialMatrix.copy();
		socialMatrix.transpose(invSocialMatrix);
	}

	@Override
	protected void buildModel() {
		for (int iter = 0; iter < maxIters; iter++) {
			for (int u = 0; u < numUsers; u++) {

				for (int j = 0; j < numItems; j++) {
					//
				}
			}
		}
	}

}
