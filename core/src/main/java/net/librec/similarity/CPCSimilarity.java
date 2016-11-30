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

import java.util.List;

import net.librec.data.DataModel;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;

/**
 * Constrained Pearson Correlation (CPC)
 * 
 * @author zhanghaidong
 *
 */
public class CPCSimilarity extends AbstractRecommenderSimilarity {
	
	private double median;
	
	/**
	 * 
	 * @param dataModel
	 * @param isUser: calculate the similarity between users, or the similarity between items.
	 */
	public void buildSimilarityMatrix(DataModel dataModel, boolean isUser) {
		SparseMatrix trainMatrix = dataModel.getDataSplitter().getTrainData();
		double maximum = 0.0;
		double minimum = 100.0;
		for (MatrixEntry me : trainMatrix) {
			if (me.get() > maximum) {
				maximum = me.get();
			}
			if (me.get() < minimum) {
				minimum = me.get();
			}			
		}
		median = (maximum + minimum) / 2;
		
		super.buildSimilarityMatrix(dataModel, isUser);
	}
	
	/**
	 * Calculate Constrained Pearson Correlation (CPC) between two vectors
	 * 
	 * @return Constrained PCC Correlation (CPC)
	 */
	protected double getSimilarity(List<? extends Number> thisList, List<? extends Number> thatList){
		// compute similarity
		
		if (thisList == null || thatList == null || thisList.size() < 1 || thatList.size() < 1 || thisList.size() != thatList.size()) {
			return Double.NaN;
		}
		
		double innerProduct = 0.0, thisPower2 = 0.0, thatPower2 = 0.0;
		for (int i = 0; i < thisList.size(); i++) {
			double thisDiff = thisList.get(i).doubleValue() - median;
			double thatDiff = thatList.get(i).doubleValue() - median;

			innerProduct += thisDiff * thatDiff;
			thisPower2 += thisDiff * thisDiff;
			thatPower2 += thatDiff * thatDiff;
		}
		return innerProduct / Math.sqrt(thisPower2 * thatPower2);
	}
}
