// Copyright (C) 2014-2015 Guibing Guo
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
package librec.ranking;

import happy.coding.io.Logs;
import happy.coding.io.Strings;

import java.util.List;

import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Latent Dirichlet Allocation for implicit feedback: Tom Griffiths, <strong>Gibbs sampling in the generative model of
 * Latent Dirichlet Allocation</strong>, 2002.
 * 
 * @author Guibing Guo
 *
 */
@Configuration("factors, alpha, beta, iters, burn.in, sample.lag")
public class LDA extends IterativeRecommender {

	/**
	 * number of users, items, topics;
	 */
	protected int M, V, K;

	/**
	 * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
	 */
	protected double alpha;

	/**
	 * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
	 */
	protected double beta;

	/**
	 * entry[u][i]: topic assignment as sparse structure
	 */
	protected Table<Integer, Integer, Integer> z;

	/**
	 * entry[i][t]: number of instances of item i assigned to topic t.
	 */
	protected DenseMatrix itemTopicMatrix;

	/**
	 * entry[u][t]: number of items of user u assigned to topic t.
	 */
	protected DenseMatrix userTopicMatrix;

	/**
	 * entry[t]: total number of items assigned to topic t.
	 */
	protected DenseVector topicItemsVector;

	/**
	 * entry[u]: total number of items rated by user u.
	 */
	protected DenseVector userItemsVector;

	/**
	 * cumulative statistics of theta, phi
	 */
	protected DenseMatrix thetasum, phisum;

	/**
	 * posterior probabilities of parameters
	 * 
	 */
	protected DenseMatrix theta, phi;

	/**
	 * burn-in period
	 */
	protected int burnIn;

	/**
	 * sample lag (if -1 only one sample taken)
	 */
	protected int sampleLag;

	/**
	 * size of statistics
	 */
	protected int numstats;

	public LDA(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {

		// for simplicity
		M = numUsers;
		V = numItems;
		K = numFactors;

		burnIn = cf.getInt("num.burn.in");
		sampleLag = cf.getInt("num.sample.lag");

		alpha = cf.getDouble("val.init.alpha");
		beta = cf.getDouble("val.init.beta");

		thetasum = new DenseMatrix(M, K);
		phisum = new DenseMatrix(K, V);
		numstats = 0;

		// initialise count variables.
		itemTopicMatrix = new DenseMatrix(V, K);
		userTopicMatrix = new DenseMatrix(M, K);
		topicItemsVector = new DenseVector(K);
		userItemsVector = new DenseVector(M);

		// The z_u,i are initialised to values in [0, K-1] to determine the initial state of the Markov chain.
		z = HashBasedTable.create();
		for (int u = 0; u < M; u++) {
			List<Integer> Ru = trainMatrix.getColumns(u);
			for (int i : Ru) {
				int t = (int) (Math.random() * K); // 0 ~ k-1

				// assign a topic t to pair (u, i)
				z.put(u, i, t);
				// number of instances of item i assigned to topic t
				itemTopicMatrix.add(i, t, 1);
				// number of items of user u assigned to topic t.
				userTopicMatrix.add(u, t, 1);
				// total number of words assigned to topic t.
				topicItemsVector.add(t, 1);
			}
			// total number of items of user u
			userItemsVector.set(u, Ru.size());
		}

	}

	@Override
	protected void buildModel() throws Exception {

		for (int iter = 1; iter <= numIters; iter++) {

			// Gibbs sampling from full conditional distribution
			for (Cell<Integer, Integer, Integer> entry : z.cellSet()) {
				int u = entry.getRowKey();
				int i = entry.getColumnKey();
				int t = entry.getValue(); // topic

				itemTopicMatrix.add(i, t, -1);
				userTopicMatrix.add(u, t, -1);
				topicItemsVector.add(t, -1);
				userItemsVector.add(u, -1);

				// do multinomial sampling via cumulative method:
				double[] p = new double[K];
				for (int k = 0; k < K; k++) {
					p[k] = (itemTopicMatrix.get(i, k) + beta) / (topicItemsVector.get(k) + V * beta)
							* (userTopicMatrix.get(u, k) + alpha) / (userItemsVector.get(u) + K * alpha);
				}
				// cumulate multinomial parameters
				for (int k = 1; k < p.length; k++) {
					p[k] += p[k - 1];
				}
				// scaled sample because of unnormalised p[], randomly sampled a new topic t
				double rand = Math.random() * p[K - 1];
				for (t = 0; t < p.length; t++) {
					if (rand < p[t])
						break;
				}

				// add newly estimated z_i to count variables
				itemTopicMatrix.add(i, t, 1);
				userTopicMatrix.add(u, t, 1);
				topicItemsVector.add(t, 1);
				userItemsVector.add(u, 1);

				z.put(u, i, t);
			}

			// get statistics after burn-in
			if ((iter > burnIn) && (sampleLag > 0) && (iter % sampleLag == 0)) {
				readoutParams();
			}

			if (iter % 100 == 0)
				Logs.debug("{}{} runs at iter {}/{}", algoName, foldInfo, iter, numIters);
		}

		// get results
		if (sampleLag > 0) {
			theta = thetasum.scale(1.0 / numstats);
			phi = phisum.scale(1.0 / numstats);
		} else {
			// read out parameters
			readoutParams();
			theta = thetasum;
			phi = phisum;
		}
	}

	/**
	 * Add to the statistics the values of theta and phi for the current state.
	 */
	private void readoutParams() {
		double val = 0;
		for (int u = 0; u < M; u++) {
			for (int k = 0; k < K; k++) {
				val = (userTopicMatrix.get(u, k) + alpha) / (userItemsVector.get(u) + K * alpha);
				thetasum.add(u, k, val);
			}
		}
		for (int k = 0; k < K; k++) {
			for (int i = 0; i < V; i++) {
				val = (itemTopicMatrix.get(i, k) + beta) / (topicItemsVector.get(k) + V * beta);
				phisum.add(k, i, val);
			}
		}
		numstats++;
	}

	@Override
	protected double ranking(int u, int j) throws Exception {

		return DenseMatrix.product(theta, u, phi, j);
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { K, alpha, beta, numIters, burnIn, sampleLag }, ", ");
	}
}
