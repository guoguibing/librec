// Copyright (C) 2014 Guibing Guo
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.baseline;

import java.util.HashMap;
import java.util.Map;

import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.Recommender;

/**
 * Baseline: predict by the average of target user's ratings
 * 
 * @author guoguibing
 * 
 */
public class UserAverage extends Recommender {

	private Map<Integer, Double> userMeans;

	public UserAverage(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "UserAvg";
		userMeans = new HashMap<>();
	}

	@Override
	protected double predict(int u, int j) {
		if (!userMeans.containsKey(u)) {
			SparseVector uv = trainMatrix.row(u);
			userMeans.put(u, uv.getCount() > 0 ? uv.mean() : globalMean);
		}

		return userMeans.get(u);
	}
}
