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

import java.util.List;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table.Cell;

/**
 * Latent Dirichlet Allocation for implicit feedback: Tom Griffiths, <strong>Gibbs sampling in the generative model of
 * Latent Dirichlet Allocation</strong>, 2002.
 * 
 * @author Guibing Guo
 *
 */
public class LDA extends GraphicRecommender {

	public LDA(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
		
		isRankingPred = true;
	}

	@Override
	protected void initModel() throws Exception {

		thetaSum = new DenseMatrix(numUsers, numFactors);
		phiSum = new DenseMatrix(numFactors, numItems);

		// initialize count variables.
		Nit = new DenseMatrix(numItems, numFactors);
		Nut = new DenseMatrix(numUsers, numFactors);
		Ni = new DenseVector(numFactors);
		Nu = new DenseVector(numUsers);

		// The z_u,i are initialized to values in [0, K-1] to determine the initial state of the Markov chain.
		z = HashBasedTable.create();
		for (int u = 0; u < numUsers; u++) {
			List<Integer> Ru = trainMatrix.getColumns(u);
			for (int i : Ru) {
				int t = (int) (Math.random() * numFactors); // 0 ~ k-1

				// assign a topic t to pair (u, i)
				z.put(u, i, t);
				// number of instances of item i assigned to topic t
				Nit.add(i, t, 1);
				// number of items of user u assigned to topic t.
				Nut.add(u, t, 1);
				// total number of words assigned to topic t.
				Ni.add(t, 1);
			}
			// total number of items of user u
			Nu.set(u, Ru.size());
		}

	}
	
	protected void inferParams() {

		// Gibbs sampling from full conditional distribution
		for (Cell<Integer, Integer, Integer> entry : z.cellSet()) {
			int u = entry.getRowKey();
			int i = entry.getColumnKey();
			int t = entry.getValue(); // topic

			Nit.add(i, t, -1);
			Nut.add(u, t, -1);
			Ni.add(t, -1);
			Nu.add(u, -1);

			// do multinomial sampling via cumulative method:
			double[] p = new double[numFactors];
			for (int k = 0; k < numFactors; k++) {
				p[k] = (Nit.get(i, k) + beta) / (Ni.get(k) + numItems * beta)
						* (Nut.get(u, k) + alpha) / (Nu.get(u) + numFactors * alpha);
			}
			// cumulating multinomial parameters
			for (int k = 1; k < p.length; k++) {
				p[k] += p[k - 1];
			}
			// scaled sample because of unnormalized p[], randomly sampled a new topic t
			double rand = Math.random() * p[numFactors - 1];
			for (t = 0; t < p.length; t++) {
				if (rand < p[t])
					break;
			}

			// add newly estimated z_i to count variables
			Nit.add(i, t, 1);
			Nut.add(u, t, 1);
			Ni.add(t, 1);
			Nu.add(u, 1);

			z.put(u, i, t);
		}
	}

	/**
	 * Add to the statistics the values of theta and phi for the current state.
	 */
	protected void readoutParams() {
		double val = 0;
		for (int u = 0; u < numUsers; u++) {
			for (int k = 0; k < numFactors; k++) {
				val = (Nut.get(u, k) + alpha) / (Nu.get(u) + numFactors * alpha);
				thetaSum.add(u, k, val);
			}
		}
		for (int k = 0; k < numFactors; k++) {
			for (int i = 0; i < numItems; i++) {
				val = (Nit.get(i, k) + beta) / (Ni.get(k) + numItems * beta);
				phiSum.add(k, i, val);
			}
		}
		numStats++;
	}
	
	@Override
	protected void postProbDistr() {
		theta = thetaSum.scale(1.0 / numStats);
		phi = phiSum.scale(1.0 / numStats);
	}

	@Override
	protected double ranking(int u, int j) throws Exception {

		return DenseMatrix.product(theta, u, phi, j);
	}

}
