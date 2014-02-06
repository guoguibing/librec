package lib.rec.intf;

import happy.coding.io.Strings;

import java.util.List;

import lib.rec.data.DataDAO;
import lib.rec.data.SparseMat;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

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
	protected static SparseMat socialMatrix;
	protected static FlexCompRowMatrix invSocialMatrix;

	// a list of social scales
	protected static List<Double> socialScales;

	// social regularization
	protected double regS;

	// initialization
	static {
		socialDao = new DataDAO(cf.getPath("dataset.social"), rateDao.getUserIds());

		try {
			socialMatrix = socialDao.readData();
			socialScales = socialDao.getScales();

			numUsers = socialDao.numUsers();
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
