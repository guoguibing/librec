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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.data;

import java.io.Serializable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Data Structure, Lower Symmetric Matrix
 * 
 * @author guoguibing
 * 
 */
public class SymmMatrix implements Serializable {

	private static final long serialVersionUID = -6138247653152029007L;

	// matrix dimension
	protected int dim;
	// matrix data
	Table<Integer, Integer, Double> data;

	/**
	 * Construct a symmetric matrix
	 */
	public SymmMatrix(int dim) {
		this.dim = dim;
		data = HashBasedTable.create(); // do not specify the size here as a sparse matrix
	}

	/**
	 * Construct a symmetric matrix by deeply copying data from a given matrix
	 */
	public SymmMatrix(SymmMatrix mat) {
		dim = mat.dim;
		data = HashBasedTable.create(mat.data);
	}

	/**
	 * Make a deep copy of current matrix
	 */
	public SymmMatrix clone() {
		return new SymmMatrix(this);
	}

	/**
	 * Get a value at entry (row, col)
	 */
	public double get(int row, int col) {

		if (data.contains(row, col))
			return data.get(row, col);
		else if (data.contains(col, row))
			return data.get(col, row);

		return 0.0;
	}

	/**
	 * set a value to entry (row, col)
	 */
	public void set(int row, int col, double val) {
		if (row >= col)
			data.put(row, col, val);
		else
			data.put(col, row, val);
	}

	/**
	 * add a value to entry (row, col)
	 */
	public void add(int row, int col, double val) {
		if (row >= col)
			data.put(row, col, val + get(row, col));
		else
			data.put(col, row, val + get(col, row));
	}

	/**
	 * Retrieve a complete row of similar items
	 */
	public SparseVector row(int row) {
		SparseVector res = new SparseVector(dim);
		for (int col = 0; col < dim; col++) {
			double val = get(row, col);
			if (val != 0)
				res.set(col, val);
		}

		return res;
	}

	@Override
	public String toString() {
		return "Dimension: " + dim + " x " + dim + "\n" + data.toString();
	}

}
