package lib.rec.data;

import java.util.Arrays;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class SparseMat extends CompRowMatrix {

	public SparseMat(Matrix A) {
		super(A);
	}

	public SparseMat(int numRows, int numColumns, int[][] nz) {
		super(numRows, numColumns, nz);
	}
	
	/**
	 * get a row sparse vector of a matrix
	 * 
	 * @param row
	 *            row id
	 * @return a sparse vector of {index, value}
	 * 
	 */
	public SparseVector row(int row) {

		int[] row_ptr = getRowPointers();
		int[] col_idx = getColumnIndices();

		int start = row_ptr[row];
		int end = row_ptr[row + 1];

		SparseVector sv = new SparseVector(numColumns);

		for (int j = start; j < end; j++) {
			int col = col_idx[j];
			double val = get(row, col);
			if (val != 0.0)
				sv.set(col, val);
		}

		return sv;
	}

	/**
	 * get a row sparse vector of a matrix with a specific index exclusive
	 * 
	 * @param row
	 *            row id
	 * 
	 * @param excep
	 *            exclusive index
	 * 
	 * @return a sparse vector of {index, value}
	 * 
	 */
	public SparseVector row(int row, int except) {

		int[] row_ptr = getRowPointers();
		int[] col_idx = getColumnIndices();

		int start = row_ptr[row];
		int end = row_ptr[row + 1];

		SparseVector sv = new SparseVector(numColumns);

		for (int j = start; j < end; j++) {
			int col = col_idx[j];
			if (col != except) {
				double val = get(row, col);
				if (val != 0.0)
					sv.set(col, val);
			}
		}

		return sv;
	}

	/**
	 * get a column sparse vector of a matrix
	 * 
	 * @param col
	 *            column id
	 * @return a list of row indices whose corresponding matrix values are not 0
	 */
	public SparseVector col(int col) {

		SparseVector sv = new SparseVector(numRows);

		for (int row = 0; row < numRows; row++) {

			double val = get(row, col);
			if (val != 0.0)
				sv.set(row, val);
		} 

		return sv;
	}

	/**
	 * get a column sparse vector of a matrix by making use of the storage of
	 * compressed row storage
	 * 
	 * @deprecated This implementation is slower than the method {@code col},
	 *             hence all should use {@code col} method to retrieve a column
	 *             vector¡£
	 */
	public SparseVector colByStorage(int col) {

		int[] row_ptr = getRowPointers();
		int[] col_idx = getColumnIndices().clone();

		SparseVector sv = new SparseVector(numRows);

		// slower as col_idx is much greater than num of rows
		for (int idx = 0; idx < col_idx.length; idx++) {

			int j = col_idx[idx];
			if (j == col) {
				// find row id
				int row = Arrays.binarySearch(row_ptr, idx);

				if (row < 0)
					row = -(row + 1) - 1;

				double val = get(row, col);
				if (val != 0.0)
					sv.set(row, val);
			}
		}
		return sv;
	}
	
	@Override
	public SparseMat copy() {
		return (SparseMat) super.copy();
	}

}
