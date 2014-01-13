package lib.rec.core;

import lib.rec.SocialRecommender;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Jamali and Ester, <strong>A matrix factorization technique with trust
 * propagation for recommendation in social networks</strong>, RecSys 2010.
 * 
 * TODO: add implementations here ...
 * 
 * @author guoguibing
 * 
 */
public class SocialMF extends SocialRecommender {

	public SocialMF(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold, String path) {
		super(trainMatrix, testMatrix, fold, path);

		algoName = "SocialMF";
	}

}
