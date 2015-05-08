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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.RatingContext;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

/**
 * Hanna M. Wallach, <strong>Topic Modeling: Beyond Bag-of-Words</strong>, ICML 2006.
 * 
 * @author Guo Guibing
 *
 */
public class ItemBigram extends GraphicRecommender {

	private Map<Integer, List<Integer>> userItems;

	/**
	 * k: current topic; j: previously rated item; i: current item
	 */
	private int[][][] Nkji;
	private DenseMatrix Nkj;
	private double[][][] Pkji, PkjiSum;

	private DenseMatrix beta;

	public ItemBigram(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		// build the training data, sorting by date
		userItems = new HashMap<>();
		for (int u = 0; u < numUsers; u++) {
			List<Integer> unsortedItems = trainMatrix.getColumns(u);
			int size = unsortedItems.size();

			List<RatingContext> rcs = new ArrayList<>(size);
			for (Integer i : unsortedItems) {
				rcs.add(new RatingContext(u, i, timestamps.get(u, i)));
			}
			Collections.sort(rcs);

			List<Integer> sortedItems = new ArrayList<>(size);
			for (RatingContext rc : rcs) {
				sortedItems.add(rc.getItem());
			}

			userItems.put(u, sortedItems);
		}

		// count variables
		Nuk = new DenseMatrix(numUsers, numFactors);
		Nu = new DenseVector(numUsers);
		Nkji = new int[numFactors][numItems][numItems];
		Nkj = new DenseMatrix(numFactors, numItems);

		// Logs.debug("Nkji consumes {} bytes", Strings.toString(Memory.bytes(Nkji)));

		// parameters
		PukSum = new DenseMatrix(numUsers, numFactors);
		PkjiSum = new double[numFactors][numItems][numItems];
		Pkji = new double[numFactors][numItems][numItems];

		// hyper-parameters
		alpha = new DenseVector(numFactors);
		alpha.setAll(initAlpha);

		beta = new DenseMatrix(numFactors, numItems);
		beta.setAll(initBeta);

		// initialization
		for (Entry<Integer, List<Integer>> en : userItems.entrySet()) {
			int user = en.getKey();
			List<Integer> items = en.getValue();
			
			for(int item: items){
				// TODO: add codes here 
			}
		}
	}

}
