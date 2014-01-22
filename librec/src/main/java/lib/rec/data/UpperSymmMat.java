package lib.rec.data;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

/**
 * Upper Symmetric and Sparse Matrix: most useful for item/user correlations
 * 
 * @author guoguibing
 * 
 */
public class UpperSymmMat extends FlexCompRowMatrix {

	/**
	 * Constructor for Upper Symmetric and SparseMatrix
	 * 
	 * @param dim
	 *            dimension size
	 */
	public UpperSymmMat(int dim) {
		super(dim, dim);
	}

	public UpperSymmMat(Matrix m) {
		super(m);
	}

	@Override
	public UpperSymmMat copy() {
		return new UpperSymmMat(this);
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
		return row < col ? super.get(row, col) : super.get(col, row);
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
			super.set(row, col, val);
		else
			super.set(col, row, val);
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
			super.add(row, col, val);
		else
			super.add(col, row, val);
	}

	/**
	 * find a complete row of items
	 * 
	 * @param i
	 *            row id
	 * @return a sparse vector
	 */
	public SparseVec row(int i) {
		SparseVec nv = new SparseVec(super.getRow(i));
		for (int j = 0; j <= i; j++) {
			double val = super.get(j, i);
			if (val != 0)
				nv.set(j, val);
		}

		return nv;
	}
}
