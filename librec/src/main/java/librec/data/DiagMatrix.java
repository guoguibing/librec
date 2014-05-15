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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Data Structure: Diagonal Matrix
 * 
 * @author guoguibing
 * 
 */
public class DiagMatrix extends SparseMatrix {

	public DiagMatrix(int rows, int cols, Table<Integer, Integer, Double> dataTable) {
		super(rows, cols, dataTable);
	}

	public DiagMatrix add(DiagMatrix that) {

		Table<Integer, Integer, Double> vals = HashBasedTable.create();
		int n = this.numCols;
		
		for (int i = 0; i < n; i++)
			vals.put(i, i, this.get(i, i) + that.get(i, i));

		return new DiagMatrix(n, n, vals);
	}
	
	public DiagMatrix minus(DiagMatrix that) {
		
		Table<Integer, Integer, Double> vals = HashBasedTable.create();
		int n = this.numCols;
		
		for (int i = 0; i < n; i++)
			vals.put(i, i, this.get(i, i) - that.get(i, i));
		
		return new DiagMatrix(n, n, vals);
	}

	public static DiagMatrix eye(int n) {
		Table<Integer, Integer, Double> vals = HashBasedTable.create();
		for (int i = 0; i < n; i++)
			vals.put(i, i, 1.0);

		return new DiagMatrix(n, n, vals);
	}
}
