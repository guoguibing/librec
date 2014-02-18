package librec.intf;

import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.DataDAO;
import librec.data.SparseMatrix;

/**
 * Recommenders in which social information is used
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
		// to support multiple tests in one time in future
		regS = cf.getRange("val.reg.social").get(0);

		String socialPath = cf.getPath("dataset.social");
		Logs.debug("Social dataset: {}", Strings.last(socialPath, 38));

		socialDao = new DataDAO(socialPath, rateDao.getUserIds());

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
		return Strings.toString(new Object[] { initLRate, (float) regU, (float) regI, (float) regS, numFactors,
				maxIters, isBoldDriver }, ",");
	}

}
