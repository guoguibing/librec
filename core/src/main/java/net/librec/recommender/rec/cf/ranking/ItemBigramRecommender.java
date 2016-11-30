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
import java.util.List;
import java.util.Map;

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.recommender.rec.ProbabilisticGraphicalRecommender;

/**
 * 
 * Hanna M. Wallach, <strong>Topic Modeling: Beyond Bag-of-Words</strong>, ICML 2006.
 * 
 */
public class ItemBigramRecommender extends ProbabilisticGraphicalRecommender {

	private Map<Integer, List<Integer>> userItemsMap;
	
	/**
	 * k: current topic; j: previously rated item; i: current item
	 */
	private int[][][] topicPreItemCurItemNum;
	private DenseMatrix topicItemProbs;
	private double[][][] topicPreItemCurItemProbs, topicPreItemCurItemSumProbs;
	
	private DenseMatrix beta;
	
	@Override
	protected void setup() throws LibrecException {
		super.setup();
		
		userItemsMap = new HashMap<>();
		
		for(int u = 0; u < numUsers; u++) {
			List<Integer> unsortedItems = trainMatrix.getColumns(u);
			int size = unsortedItems.size();
			
			
		}
	}
	
	@Override
	protected void eStep() {
		
	}

	@Override
	protected void mStep() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected double predict(int userIdx, int itemIdx) throws LibrecException {
		// TODO Auto-generated method stub
		return 0;
	}

}
