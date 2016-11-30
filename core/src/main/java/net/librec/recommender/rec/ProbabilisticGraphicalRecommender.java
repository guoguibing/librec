/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.rec;

import net.librec.common.LibrecException;
import net.librec.recommender.AbstractRecommender;

/**
 * Created by Keqiang Wang
 */
public abstract class ProbabilisticGraphicalRecommender extends AbstractRecommender {
	/**
	 * the number of users
	 */
	protected int numUsers;
	/**
	 * the number of items
	 */
	protected int numItems;
    /**
     * the number of iterations
     */
    protected int numIterations;
    /**
     * burn-in period
     */
    protected int burnIn;
	/**
	 * the last loss
	 */
	protected double lastLoss = Double.MIN_VALUE;

	/**
	 * size of statistics
	 */
	protected int numStats = 0;

    /**
     * sample lag (if -1 only one sample taken)
     */
    protected int sampleLag;

    /**
     * setup
     * init member method
     *
     * @throws LibrecException
     */
    protected void setup() throws LibrecException {
        super.setup();
        numIterations = conf.getInt("rec.iterator.maximum");
		numUsers = trainMatrix.numRows();
		numItems = trainMatrix.numColumns();
        burnIn = conf.getInt("rec.pgm.burn-in", 100);
        sampleLag = conf.getInt("rec.pgm.sample-lag", 10);
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            // E-step: infer parameters
            eStep();

            // M-step: update hyper-parameters
            mStep();

            // get statistics after burn-in
            if ((iter > burnIn) && (iter % sampleLag == 0)) {
                readoutParams();
                
                if (isConverged(iter))
					break;
            }
        }
        // retrieve posterior probability distributions
        estimateParams();
    }

    /**
     * parameters estimation: used in the training phase
     */
    protected abstract void eStep();

    /**
     * update the hyper-parameters
     */
    protected abstract void mStep();

    /**
     * read out parameters for each iteration
     */
    protected void readoutParams() {
    	
    }

	/**
	 * employing early stopping criteria
	 * 
	 * @param iter
	 *            current iteration
	 */
	protected boolean isConverged(int iter) throws LibrecException {
		return false;
	}
	
    /**
     * estimate the model parameters
     */
    protected void estimateParams() {
    	
    }
}
