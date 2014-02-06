package lib.rec.core;

import lib.rec.data.SparseMat;

/**
 * Yang et al., <strong>Social Collaborative Filtering by Trust</strong>, IJCAI
 * 2013.
 * 
 * @author guoguibing
 * 
 */
public class TrustMF extends SocialMF {

	public TrustMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "TrustMF";
	}
	
	@Override
	protected void buildModel() {
		// TODO Auto-generated method stub
		super.buildModel();
	}

}
