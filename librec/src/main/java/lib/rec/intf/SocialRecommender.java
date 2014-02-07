package lib.rec.intf;

import happy.coding.io.Strings;
import lib.rec.data.DataDAO;
import lib.rec.data.SparseMat;

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
	// invSocialMatrix: inverse social matrix, indicating a user is connected by a number of other users
	protected static SparseMat socialMatrix, trSocialMatrix;

	// social regularization
	protected double regS;

	// initialization
	static {
		socialDao = new DataDAO(cf.getPath("dataset.social"), rateDao.getUserIds());

		try {
			socialMatrix = socialDao.readData();
			numUsers = socialDao.numUsers();
			
			trSocialMatrix = socialMatrix.transpose();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public SocialRecommender(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		
		regS = cf.getDouble("val.reg.social");
	}
	
	@Override
	public String toString() {
		return Strings.toString(new Object[] { initLRate, regU, regI, regS, numFactors, maxIters, isBoldDriver }, ",");
	}

}
