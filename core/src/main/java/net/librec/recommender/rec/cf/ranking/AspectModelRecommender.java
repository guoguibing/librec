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
package net.librec.recommender.rec.cf.ranking;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.ProbabilisticGraphicalRecommender;

/**
 * 
* <h3> Latent class models for collaborative filtering</h3>
 * <p>
 * This implementation refers to the method proposed by Thomas et al. at IJCAI 1999.
 * <p>
 * <strong>Tempered EM:</strong> Thomas Hofmann, <strong>Latent class models for collaborative filtering
 * </strong>, IJCAI. 1999, 99: 688-693.
 * 
 * 
 * @author Haidong Zhang
 *
 */

public class AspectModelRecommender  extends ProbabilisticGraphicalRecommender {
	
	/*
	 * number of topics
	 */
	protected int numTopics;
	/*
	 * Conditional distribution: P(u|z)
	 */
	protected DenseMatrix topicUserProbs, topicUserProbsSum;
	/*
	 * Conditional distribution: P(i|z)
	 */
	protected DenseMatrix topicItemProbs, topicItemProbsSum;
	/*
	 * topic distribution: P(z)
	 */
	protected DenseVector topicProbs, topicProbsSum;
	/*
	 * {user, item, {topic z, probability}}
	 */
	protected Table<Integer, Integer, Map<Integer, Double>> Q;
	/*
	 * small value
	 */
	protected static double smallValue = 0.0000001;
	
	@Override
	protected void setup() throws LibrecException {
		super.setup();
		numTopics = conf.getInt("rec.factory.number", 10);
		isRanking = true;
		
		// Initialize topic distribution
		topicProbs = new DenseVector(numTopics);
		topicProbsSum = new DenseVector(numTopics);
		double[] probs = Randoms.randProbs(numTopics);
		for (int z = 0; z < numTopics; z++) {
			topicProbs.set(z, probs[z]);
		}
		
		topicUserProbs = new DenseMatrix(numTopics, numUsers);
		for(int z = 0; z < numTopics; z++) {
			probs = Randoms.randProbs(numUsers);
			for(int u = 0; u < numUsers; u++) {
				topicUserProbs.set(z, u, probs[u]);
			}
		}
		
		topicItemProbs = new DenseMatrix(numTopics, numItems);
		for(int z = 0; z < numTopics; z++) {
			probs = Randoms.randProbs(numItems);
			for(int i = 0; i < numItems; i++) {
				topicItemProbs.set(z, i, probs[i]);
			}
		}
		
		// initialize Q
		Q = HashBasedTable.create();
        for (MatrixEntry me : trainMatrix) {
            int u = me.row();
            int i = me.column();
            Q.put(u, i, new HashMap<Integer, Double>());
        }
	}

	/*
	 * 
	 */
	@Override
	protected void eStep() {
		
		for(MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			
			Map<Integer, Double> QTopicProbs = Q.get(u, i);
			double sum = 0.0;
			for (int z = 0; z < numTopics; z++) {
				double value = topicUserProbs.get(z, u) * topicItemProbs.get(z, i) * topicProbs.get(z);
				QTopicProbs.put(z, value);
				sum += value;
			}
			
			// Normalize along with the latent states
			for (int z = 0; z < numTopics; z++) {
				QTopicProbs.put(z, QTopicProbs.get(z) / sum);
			}			
		}	
	}

	@Override
	protected void mStep() {
		topicProbsSum.setAll(0.0);
		topicUserProbsSum.setAll(0.0);
		topicItemProbsSum.setAll(0.0);
		
		for(int z = 0; z < numTopics; z++) {
			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int i = me.column();
				double num = me.get();
				
				double val = Q.get(u, i).get(z) * num;
				topicProbsSum.add(z, val);
				topicUserProbsSum.add(z, u, val);
				topicItemProbsSum.add(z, i, val);
			}
		}
		topicProbs = topicProbsSum.scale(1.0 / topicProbsSum.sum());
		for (int z = 0; z < numTopics; z++) {
			double userProbsSum = topicUserProbs.sumOfColumn(z);
			for (int u = 0; u < numUsers; u++) {
				topicUserProbs.set(z, u, topicUserProbsSum.get(z, u) / userProbsSum);
			}
			double itemProbsSum = topicItemProbs.sumOfColumn(z);
			for (int i = 0; i < numItems; i++) {
				topicItemProbs.set(z, i, topicItemProbsSum.get(z, i) / itemProbsSum);
			}
		}		
	}



	@Override
	protected double predict(int userIdx, int itemIdx) throws LibrecException {
		double predictRating = 0.0;
		for (int z = 0; z < numTopics; z++) {
			predictRating += topicUserProbs.get(z, userIdx) * topicItemProbs.get(z, itemIdx) * topicProbs.get(z);
		}
		return predictRating;
	}

}
