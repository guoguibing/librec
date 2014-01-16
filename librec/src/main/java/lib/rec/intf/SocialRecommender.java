package lib.rec.intf;

import java.util.List;

import lib.rec.DataDAO;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

/**
 * Abstract class for social recommender where social information is enabled.
 * 
 * @author guoguibing
 * 
 */
public abstract class SocialRecommender extends IterativeRecommender {

	// social data dao
	protected DataDAO socialDao; 

	// socialMatrix: social rate matrix, indicating a user is connecting to a number of other users  
	// invSocialMatrix: inverse social matrix, indicating a user is connected by a number of other users
	protected CompRowMatrix socialMatrix, invSocialMatrix;
	// a list of social scales
	protected static List<Double> socialScales;

	public SocialRecommender(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold, String path) {
		super(trainMatrix, testMatrix, fold);

		socialDao = new DataDAO(path, rateDao.getUserIds());

		try {
			socialMatrix = socialDao.readData();
			socialScales = socialDao.getScales();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
