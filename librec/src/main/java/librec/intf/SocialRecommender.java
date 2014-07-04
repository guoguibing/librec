// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

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
		return Strings.toString(new Object[] { initLRate, maxLRate, (float) regU, (float) regI, (float) regS,
				numFactors, numIters, isBoldDriver }, ",");
	}

	@Override
	protected boolean isTestable(int u, int j) {
		switch (view) {
		case "cold-start":
			return trainMatrix.rowSize(u) < 5 ? true : false;
		case "trust-degree":
			int min_deg = cf.getInt("min.trust.degree");
			int max_deg = cf.getInt("max.trust.degree");
			if (min_deg == -1)
				min_deg = 0;
			if (max_deg == -1)
				max_deg = Integer.MAX_VALUE;

			// size could be indegree + outdegree
			int out_deg = socialMatrix.rowSize(u);
			int deg = out_deg;

			boolean cond = (deg >= min_deg) && (deg <= max_deg);

			return cond ? true : false;

		case "all":
		default:
			return true;
		}
	}

}
