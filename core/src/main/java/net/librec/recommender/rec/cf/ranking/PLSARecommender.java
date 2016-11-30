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
import net.librec.math.structure.DataMatrix;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.recommender.item.RecommendedItemList;
import net.librec.recommender.item.RecommendedList;
import net.librec.recommender.rec.ProbabilisticGraphicalRecommender;

/**
 * Thomas Hofmann, <strong>Latent semantic models for collaborative filtering</strong>, 
 * ACM Transactions on Information Systems.
 * 2004. <br>
 * 
 * @author Haidong Zhang
 *
 */

public class PLSARecommender extends ProbabilisticGraphicalRecommender {
	
	/*
	 * number of latent topics
	 */
	protected int numTopics;
	/*
	 * {user, item, {topic z, probability}}
	 */
	protected Table<Integer, Integer, Map<Integer, Double>> Q;
	/*
	 * Conditional Probability: P(z|u)
	 */
	protected DenseMatrix userTopicProbs, userTopicProbsSum;
	/*
	 * Conditional Probability: P(i|z)
	 */
	protected DenseMatrix topicItemProbs, topicItemProbsSum;
	
	@Override
	protected void setup() throws LibrecException {
		super.setup();
		numTopics = conf.getInt("rec.factory.number", 10);
		isRanking = true;
		
		userTopicProbs = new DenseMatrix(numUsers, numTopics);
		for (int u = 0; u < numUsers; u++) {
			double[] probs = Randoms.randProbs(numTopics);
			for (int z = 0; z < numTopics; z++) {
				userTopicProbs.set(u, z, probs[z]);
			}
		}
		
		topicItemProbs = new DenseMatrix(numTopics, numItems);
		for (int z = 0; z < numItems; z++) {
			double[] probs = Randoms.randProbs(numItems);
			for (int i = 0; i < numItems; i++) {
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
	
	@Override
	protected void eStep() {
		for(MatrixEntry me : trainMatrix) {
			int u = me.row();
			int i = me.column();
			
			Map<Integer, Double> QTopicProbs = Q.get(u, i);
			double sum = 0.0;
			for (int z = 0; z < numTopics; z++) {
				double value = userTopicProbs.get(u, z) * topicItemProbs.get(z, i);
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
		userTopicProbsSum.setAll(0.0);
		topicItemProbsSum.setAll(0.0);
		
		for(int z = 0; z < numTopics; z++) {
			for (MatrixEntry me : trainMatrix) {
				int u = me.row();
				int i = me.column();
				double num = me.get();
				
				double val = Q.get(u, i).get(z) * num;
				userTopicProbsSum.add(u, z, val);
				topicItemProbsSum.add(z, i, val);
			}
		}
		for (int u = 0; u < numUsers; u++) {
			double userProbsSum = userTopicProbsSum.sumOfRow(u);
			for (int z = 0; z < numTopics; z++) {
				userTopicProbs.set(u, z, userTopicProbsSum.get(u, z) / userProbsSum);
			}
		}
		for (int z = 0; z < numTopics; z++) {
			double itemProbsSum = topicItemProbs.sumOfColumn(z);
			for (int i = 0; i < numItems; i++) {
				topicItemProbs.set(z, i, topicItemProbsSum.get(z, i) / itemProbsSum);
			}
		}
	}

	@Override
	protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return DenseMatrix.product(userTopicProbs, userIdx, topicItemProbs, itemIdx);
	}

}
