package lib.rec.data;

import java.util.Arrays;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

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
	public SparseVec row(int row) {

		int[] row_ptr = super.getRowPointers();
		int[] col_idx = super.getColumnIndices();

		int start = row_ptr[row];
		int end = row_ptr[row + 1];

		SparseVec sv = new SparseVec(numColumns);

		for (int j = start; j < end; j++) {
			int col = col_idx[j];
			double val = get(row, col);
			if (val != 0.0)
				sv.set(col, val);
		}

		return sv;
	}

	/**
	 * query the size of a specific row
	 * 
	 * @param row
	 *            row id
	 * @return the size of non-zero elements of a row
	 */
	public int rowSize(int row) {

		int[] row_ptr = super.getRowPointers();
		int[] col_idx = super.getColumnIndices();

		int size = 0;
		for (int j = row_ptr[row]; j < row_ptr[row + 1]; j++) {
			int col = col_idx[j];
			if (get(row, col) != 0.0)
				size++;
		}

		return size;
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
	public SparseVec row(int row, int except) {

		int[] row_ptr = super.getRowPointers();
		int[] col_idx = super.getColumnIndices();

		SparseVec sv = new SparseVec(numColumns);

		for (int j = row_ptr[row]; j < row_ptr[row + 1]; j++) {
			int col = col_idx[j];
			if (col != except) {
				double val = super.get(row, col);
				if (val != 0.0)
					sv.set(col, val);
			}
		}

		return sv;
	}

	/**
	 * get a column sparse vector of a matrix. <br/>
	 * 
	 * <strong>Note:</strong> this method is time-consuming, hence if heavily calling this
	 * method will greately decrease performance. In this case, we suggest to
	 * first transpose the current matrix to obtain a new matrix, and then call
	 * the {@code row} method of the new matrix instead. In other words, using
	 * memory space to speedup programs.
	 * 
	 * @param col
	 *            column id
	 * @return a list of row indices whose corresponding matrix values are not 0
	 */
	public SparseVec col(int col) {

		SparseVec sv = new SparseVec(numRows);

		for (int row = 0; row < numRows; row++) {

			double val = get(row, col);
			if (val != 0.0)
				sv.set(row, val);
		}

		return sv;
	}

	/**
	 * query the size of a specific column
	 * 
	 * @param col
	 *            column id
	 * @return the size of non-zero elements of a column
	 */
	public int colSize(int col) {

		int size = 0;
		for (int row = 0; row < numRows; row++) {

			double val = get(row, col);
			if (val != 0.0)
				size++;
		}

		return size;
	}

	/**
	 * get a column sparse vector of a matrix by making use of the storage of
	 * compressed row storage
	 * 
	 * @deprecated This implementation is slower than the method {@code col},
	 *             hence all should use {@code col} method to retrieve a column
	 *             vector¡£
	 */
	public SparseVec colByStorage(int col) {

		int[] row_ptr = getRowPointers();
		int[] col_idx = getColumnIndices().clone();

		SparseVec sv = new SparseVec(numRows);

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

	/**
	 * transpose a matrix: useful if high frequency to call {@code col} method
	 * which is time-consuming
	 */
	public SparseMat transpose() {
		FlexCompRowMatrix t = new FlexCompRowMatrix(numColumns, numRows);

		for (MatrixEntry me : this)
			t.set(me.column(), me.row(), me.get());

		return new SparseMat(t);
	}

}
