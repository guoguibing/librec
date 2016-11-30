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
package net.librec.similarity;

import java.util.ArrayList;
import java.util.List;

import net.librec.data.DataModel;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.SymmMatrix;

/**
 * Calculate Recommender Similarity, such as cosine, Pearson, Jaccard similarity, etc.
 * 
 * @author zhanghaidong
 *
 */

public abstract class AbstractRecommenderSimilarity implements RecommenderSimilarity {
	/**
	 * Similarity Matrix
	 */
	protected SymmMatrix similarityMatrix;
	
	/**
	 * 
	 * @param dataModel
	 * @param isUser: calculate the similarity between users, or the similarity between items.
	 */
	public void buildSimilarityMatrix(DataModel dataModel, boolean isUser) {
		SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
		int numUsers = trainMatrix.numRows();
		int numItems = trainMatrix.numColumns();
		int count = isUser ? numUsers : numItems;
		
		similarityMatrix = new SymmMatrix(count);
		
		for (int i = 0; i < count; i++) {
			SparseVector thisVector = isUser ? trainMatrix.row(i) : trainMatrix.column(i);
			if (thisVector.getCount() == 0) {
				continue;
			}
			// user/item itself exclusive
			for (int j = i + 1; j < count; j++) {
				SparseVector thatVector = isUser ? trainMatrix.row(j) : trainMatrix.column(j);
				if (thatVector.getCount() == 0) {
					continue;
				}
				
				double sim = getCorrelation(thisVector, thatVector);
				if (!Double.isNaN(sim)) {
					similarityMatrix.set(i, j, sim);
				}
			}
		}
	}
	
	/**
	 * Find the common rated items by this user and that user, 
	 * or the common users have rated this item or that item. And then return the similarity. 
	 * @param thisVector: the rated items by this user, or users that have rated this item . 
	 * @param thatVector: the rated items by that user, or users that have rated that item.
	 * @return
	 */
	public double getCorrelation(SparseVector thisVector, SparseVector thatVector) {
		// compute similarity
		List<Double> thisList = new ArrayList<Double>();
		List<Double> thatList = new ArrayList<Double>();

		for (Integer idx : thatVector.getIndex()) {
			if (thisVector.contains(idx)) {
				thisList.add(thisVector.get(idx));
				thatList.add(thatVector.get(idx));
			}
		}
		double sim = getSimilarity(thisList, thatList);
		return sim;
	}
	
	/**
	 * calculate the similarity between thisList and thatList
	 * @param thisList
	 * @param thatList
	 * @return
	 */
	protected abstract double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList);
	
	
	/**
	 * return the similarity matrix.
	 */
	public SymmMatrix getSimilarityMatrix() {
		return similarityMatrix;
	}
}
