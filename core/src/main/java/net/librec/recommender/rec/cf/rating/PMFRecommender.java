/**
 * Copyright (C) 2016 LibRec
 * 
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.rec.cf.rating;

import net.librec.common.LibrecException;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.rec.MatrixFactorizationRecommender;


/**
 * <ul>
 * <li><strong>PMF:</strong> Ruslan Salakhutdinov and Andriy Mnih, Probabilistic Matrix Factorization, NIPS 2008.</li>
 * <li><strong>RegSVD:</strong> Arkadiusz Paterek, <strong>Improving Regularized Singular Value Decomposition
 * Collaborative Filtering, Proceedings of KDD Cup and Workshop, 2007.</li>
 * </ul>
 * 
 * 
 * @author guoguibin and zhanghaidong
 * 
 */
public class PMFRecommender extends MatrixFactorizationRecommender {
	
	protected double learnRate;
	protected double regU, regI;
	
	@Override
	protected void setup() throws LibrecException {
		super.setup();
		learnRate = conf.getDouble("rec.iterator.learn.rate", 0.001);
		regU = conf.getDouble("rec.recommender.lambda.user", 0.001);
		regI = conf.getDouble("rec.recommender.lambda.item", 0.001);
	}
	
	@Override
	protected void trainModel() throws LibrecException {
		for ( int iter = 1; iter <= numIterations; iter++ ) {
			double loss = 0.0;
			for ( MatrixEntry me : trainMatrix ) {
				int userId = me.row(); // user
				int itemId = me.column(); // item
				double realRating = me.get();

				double predictRating = predict(userId, itemId);
				double error = realRating - predictRating;

				loss += error * error;

				// update factors
				for (int factorId = 0; factorId < numFactors; factorId++) {
					double userFactor = userFactors.get(userId, factorId), itemFactor = itemFactors.get(itemId, factorId);

					userFactors.add(userId, factorId, learnRate * (error * itemFactor - regU * userFactor));
					itemFactors.add(itemId, factorId, learnRate * (error * userFactor - regI * itemFactor));

					loss += regU * userFactor * userFactor + regI * itemFactor * itemFactor;
				}
			}
		}
	}
}
