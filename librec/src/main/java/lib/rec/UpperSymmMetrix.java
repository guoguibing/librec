package lib.rec;

import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class UpperSymmMetrix extends FlexCompRowMatrix {

	public UpperSymmMetrix(int dimension) {
		super(dimension, dimension);
	}

	/**
	 * get a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 * @return a value at (row, col) if row<col; otherwise at (col, row)
	 */
	public double get(int row, int col) {
		return row < col ? get(row, col) : get(col, row);
	}

	/**
	 * set a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public void set(int row, int col, double val) {
		if (row < col)
			set(row, col, val);
		else
			set(col, row, val);
	}

	/**
	 * add a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public void add(int row, int col, double val) {
		if (row < col)
			add(row, col, val);
		else
			add(col, row, val);
	}

	/**
	 * find a complete row of items
	 * 
	 * @param i
	 *            row id
	 * @return a sparse vector
	 */
	public SparseVector row(int i) {
		SparseVector nv = getRow(i);
		for (int j = 0; j < i; j++) {
			double val = get(j, i);
			if (val != 0)
				nv.set(j, val);
		}

		return nv;
	}
}
