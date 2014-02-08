package lib.rec.intf;

import happy.coding.io.Strings;
import lib.rec.data.DataDAO;
import lib.rec.data.SparseMatrix;

/**
 * Abstract class for social recommender where social information is enabled.
 * 
 * @author guoguibing
 * 
 */
public abstract class SocialRecommender extends IterativeRecommender {

	// social data dao
	protected static DataDAO socialDao;

	// socialMatrix: social rate matrix, indicating a user is connecting to a number of other users  
	// trSocialMatrix: inverse social matrix, indicating a user is connected by a number of other users
	protected static SparseMatrix socialMatrix;

	// social regularization
	protected static double regS;

	// initialization
	static {
		regS = cf.getDouble("val.reg.social");
		socialDao = new DataDAO(cf.getPath("dataset.social"), rateDao.getUserIds());

		try {
			socialMatrix = socialDao.readData();
			numUsers = socialDao.numUsers();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public SocialRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}
	
	@Override
	public String toString() {
		return Strings.toString(new Object[] { initLRate, regU, regI, regS, numFactors, maxIters, isBoldDriver }, ",");
	}

}
