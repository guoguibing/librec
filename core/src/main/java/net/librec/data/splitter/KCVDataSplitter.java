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
package net.librec.data.splitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;

/**
 * K-fold Cross Validation Data Splitter
 *
 * @author WangYuFeng and Liuxz
 */
public class KCVDataSplitter extends AbstractDataSplitter {
	private SparseMatrix preferenceMatrix;
	private SparseMatrix assignMatrix;
	private int cvNumber;
	private int cvIndex;

	public KCVDataSplitter(DataConvertor dataConvertor, Configuration conf) {
		this.dataConvertor = dataConvertor;
		this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
		this.conf = conf;
		this.cvNumber = conf.getInt("data.splitter.cv.number");
		splitFolds(cvNumber);
	}

	private void splitFolds(int kFold) {
		if (kFold > 0) {
			assignMatrix = new SparseMatrix(preferenceMatrix);
			int numRates = preferenceMatrix.getData().length;
			int numFold = kFold > numRates ? numRates : kFold;
			List<Integer> fold = Collections.synchronizedList((new ArrayList<Integer>(numRates)));
			double indvCount = (numRates + 0.0) / numFold;
			for (int i = 0; i < numRates; i++) {
				fold.add((int) (i / indvCount) + 1);
			}
			Collections.shuffle(fold);
			int[] row_ptr = preferenceMatrix.getRowPointers();
			int[] col_idx = preferenceMatrix.getColumnIndices();

			int i = 0;
			for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
				for (int idx = row_ptr[u], end = row_ptr[u + 1]; idx < end; idx++) {
					int j = col_idx[idx];
					assignMatrix.set(u, j, fold.get(i++).intValue());
				}
			}
		}
	}

	@Override
	public void splitData() throws LibrecException {
		this.cvIndex = conf.getInt("data.splitter.cv.index");
		splitData(this.cvIndex);
	}

	/**
	 * preserve the k-th validation as the test set and the rest as train set
	 *
	 * @param k
	 *            the index of validation
	 * @throws LibrecException
	 */
	public void splitData(int k) throws LibrecException {
		if (k > 0 || k <= cvNumber) {
			preferenceMatrix = dataConvertor.getPreferenceMatrix();

			trainMatrix = new SparseMatrix(preferenceMatrix);
			testMatrix = new SparseMatrix(preferenceMatrix);

			for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
				SparseVector items = preferenceMatrix.row(u);
				for (int j : items.getIndex()) {
					if (assignMatrix.get(u, j) == k)
						trainMatrix.set(u, j, 0.0);
					else
						testMatrix.set(u, j, 0.0);
				}
			}
			SparseMatrix.reshape(trainMatrix);
			SparseMatrix.reshape(testMatrix);
		}
	}

}
