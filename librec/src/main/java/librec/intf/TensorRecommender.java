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

package librec.intf;

import java.util.List;

import librec.data.Configuration;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseTensor;
import librec.util.Strings;

/**
 * Interface for tensor recommenders
 * 
 * @author Guo Guibing
 *
 */

@Configuration("factors, lRate, maxLRate, reg, iters, boldDriver")
public class TensorRecommender extends IterativeRecommender {

	/* for all tensors */
	protected static SparseTensor rateTensor;
	protected static int numDimensions, userDimension, itemDimension;
	protected static int[] dimensions;

	/* for a specific recommender */
	protected SparseTensor trainTensor, testTensor;

	static {
		rateTensor = rateDao.getRateTensor();
		numDimensions = rateTensor.numDimensions();
		dimensions = rateTensor.dimensions();

		userDimension = rateTensor.getUserDimension();
		itemDimension = rateTensor.getItemDimension();
	}

	public TensorRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
		super(trainMatrix, testMatrix, fold);

		// construct train and test data
		trainTensor = rateTensor.clone();
		testTensor = new SparseTensor(dimensions);
		testTensor.setUserDimension(userDimension);
		testTensor.setItemDimension(itemDimension);

		for (MatrixEntry me : testMatrix) {
			int u = me.row();
			int i = me.column();

			List<Integer> indices = rateTensor.getIndices(u, i);

			for (int index : indices) {
				int[] keys = rateTensor.keys(index);
				testTensor.set(rateTensor.value(index), keys);
				trainTensor.remove(keys);
			}
		}
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { numFactors, initLRate, maxLRate, reg, numIters, isBoldDriver });
	}

}
