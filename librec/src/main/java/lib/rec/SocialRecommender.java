package lib.rec;

import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Abstract class for social recommender where social information is enabled.
 * 
 * @author guoguibing
 * 
 */
public abstract class SocialRecommender extends Recommender {

	protected CompRowMatrix socialMatrix;

	public SocialRecommender(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		readSocialData(cf.getPath("dataset.social"));
	}

	/**
	 * Read Social Data
	 * 
	 * @param path
	 */
	protected void readSocialData(String path) {
		// TODO Auto-generated method stub

	}

}
