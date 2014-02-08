package lib.rec.data;

import happy.coding.io.Logs;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Data Structure: Sparse Matrix whose implementation is modified from M4J
 * library
 * 
 * <ul>
 * <li>Compressed Row Storage (CRS):
 * http://netlib.org/linalg/html_templates/node91.html</li>
 * <li>Compressed Col Storage (CCS):
 * http://netlib.org/linalg/html_templates/node92.html</li>
 * </ul>
 * 
 * @author guoguibing
 * 
 */
public class SparseMatrix implements Iterable<MatrixEntry> {

	protected int numRows, numCols;

	// Compressed Row Storage (CRS)
	protected double[] rowData;
	protected int[] rowPtr, colInd;

	// Compressed Col Storage (CCS)
	protected double[] colData;
	protected int[] colPtr, rowInd;

	// row/col non-zero ind
	protected int[][] rnzs, cnzs;

	public SparseMatrix(int rows, int cols, int[][] rnz, int[][] cnz) {
		numRows = rows;
		numCols = cols;
		rnzs = rnz;
		cnzs = cnz;

		construct(rnz, cnz);
	}

	public SparseMatrix(SparseMatrix mat) {
		this(mat.numRows, mat.numCols, mat.rnzs, mat.cnzs);

		for (MatrixEntry me : mat)
			this.set(me.row(), me.column(), me.get());
	}

	public SparseMatrix clone() {
		return new SparseMatrix(this);
	}

	public int[] getRowPointers() {
		return rowPtr;
	}

	public int[] getColumnIndices() {
		return colInd;
	}

	/**
	 * @return the cardinary of current matrix
	 */
	public int size() {
		int size = 0;

		for (MatrixEntry me : this)
			if (me.get() != 0)
				size++;

		return size;
	}

	private void construct(int[][] rnz, int[][] cnz) {
		int nnz = 0;
		for (int i = 0; i < rnz.length; ++i)
			nnz += rnz[i].length;

		// CRS
		rowPtr = new int[numRows + 1];
		colInd = new int[nnz];
		rowData = new double[nnz];

		if (rnz.length != numRows)
			throw new IllegalArgumentException("rnz.length != numRows");

		// CRS
		for (int i = 1; i <= numRows; ++i) {
			rowPtr[i] = rowPtr[i - 1] + rnz[i - 1].length;

			for (int j = rowPtr[i - 1], k = 0; j < rowPtr[i]; ++j, ++k) {
				colInd[j] = rnz[i - 1][k];
				if (rnz[i - 1][k] < 0 || rnz[i - 1][k] >= numCols)
					throw new IllegalArgumentException("rnz[" + (i - 1) + "][" + k + "]=" + rnz[i - 1][k]
							+ ", which is not a valid column index");
			}

			Arrays.sort(colInd, rowPtr[i - 1], rowPtr[i]);
		}

		// CCS
		int cnnz = 0;
		for (int i = 0; i < cnz.length; ++i)
			cnnz += cnz[i].length;

		if (cnnz != nnz)
			throw new IllegalArgumentException("rnz.length != cnz.length");

		colPtr = new int[numCols + 1];
		rowInd = new int[nnz];
		colData = new double[nnz];

		if (cnz.length != numCols)
			throw new IllegalArgumentException("cnz.length != numColumns");

		for (int i = 1; i <= numCols; ++i) {
			colPtr[i] = colPtr[i - 1] + cnz[i - 1].length;

			for (int j = colPtr[i - 1], k = 0; j < colPtr[i]; ++j, ++k) {
				rowInd[j] = cnz[i - 1][k];
				if (cnz[i - 1][k] < 0 || cnz[i - 1][k] >= numRows)
					throw new IllegalArgumentException("cnz[" + (i - 1) + "][" + k + "]=" + cnz[i - 1][k]
							+ ", which is not a valid row index");
			}

			Arrays.sort(rowInd, colPtr[i - 1], colPtr[i]);
		}
	}

	public int numRows() {
		return numRows;
	}

	public int numColumns() {
		return numCols;
	}

	public double[] getData() {
		return rowData;
	}

	public void set(int row, int col, double val) {
		int index = getCRSIndex(row, col);
		rowData[index] = val;

		index = getCCSIndex(row, col);
		colData[index] = val;
	}

	public void add(int row, int col, double val) {
		int index = getCRSIndex(row, col);
		rowData[index] += val;

		getCCSIndex(row, col);
		colData[index] += val;
	}

	public double get(int row, int col) {

		int index = Arrays.binarySearch(colInd, rowPtr[row], rowPtr[row + 1], col);

		if (index >= 0)
			return rowData[index];
		else
			return 0;
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

		SparseVector sv = new SparseVector(numCols);

		for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
			int col = colInd[j];
			double val = get(row, col);
			if (val != 0.0)
				sv.set(col, val);
		}

		return sv;
	}

	public SparseVector row(int row, int except) {

		SparseVector sv = new SparseVector(numCols);

		for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
			int col = colInd[j];
			if (col != except) {
				double val = get(row, col);
				if (val != 0.0)
					sv.set(col, val);
			}
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

		int size = 0;
		for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
			int col = colInd[j];
			if (get(row, col) != 0.0)
				size++;
		}

		return size;
	}

	/**
	 * get a col sparse vector of a matrix
	 * 
	 * @param col
	 *            col id
	 * @return a sparse vector of {index, value}
	 * 
	 */
	public SparseVector col(int col) {

		SparseVector sv = new SparseVector(numRows);

		for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
			int row = rowInd[j];
			double val = get(row, col);
			if (val != 0.0)
				sv.set(row, val);
		}

		return sv;
	}

	/**
	 * query the size of a specific col
	 * 
	 * @param col
	 *            col id
	 * @return the size of non-zero elements of a row
	 */
	public int colSize(int col) {

		int size = 0;
		for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
			int row = rowInd[j];
			double val = get(row, col);
			if (val != 0.0)
				size++;
		}

		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");

		for (MatrixEntry me : this)
			if (me.get() != 0)
				sb.append(String.format("%d\t%d\t%f\n", new Object[] { me.row() + 1, me.column() + 1, me.get() }));

		return sb.toString();
	}

	/**
	 * Finds the insertion index of CRS
	 */
	private int getCRSIndex(int row, int col) {
		int i = Arrays.binarySearch(colInd, rowPtr[row], rowPtr[row + 1], col);

		if (i >= 0 && colInd[i] == col)
			return i;
		else
			throw new IndexOutOfBoundsException("Entry (" + (row + 1) + ", " + (col + 1)
					+ ") is not in the matrix structure");
	}

	/**
	 * Finds the insertion index of CCS
	 */
	private int getCCSIndex(int row, int col) {
		int i = Arrays.binarySearch(rowInd, colPtr[col], colPtr[col + 1], row);

		if (i >= 0 && rowInd[i] == row)
			return i;
		else
			throw new IndexOutOfBoundsException("Entry (" + (row + 1) + ", " + (col + 1)
					+ ") is not in the matrix structure");
	}

	public Iterator<MatrixEntry> iterator() {
		return new MatrixIterator();
	}

	/**
	 * Entry of a compressed row matrix
	 */
	private class SparseMatrixEntry implements MatrixEntry {

		private int row, cursor;

		/**
		 * Updates the entry
		 */
		public void update(int row, int cursor) {
			this.row = row;
			this.cursor = cursor;
		}

		public int row() {
			return row;
		}

		public int column() {
			return colInd[cursor];
		}

		public double get() {
			return rowData[cursor];
		}

		public void set(double value) {
			rowData[cursor] = value;
		}
	}

	private class MatrixIterator implements Iterator<MatrixEntry> {

		private int row, cursor;

		private SparseMatrixEntry entry = new SparseMatrixEntry();

		public MatrixIterator() {
			// Find first non-empty row
			nextNonEmptyRow();
		}

		/**
		 * Locates the first non-empty row, starting at the current. After the
		 * new row has been found, the cursor is also updated
		 */
		private void nextNonEmptyRow() {
			while (row < numRows && rowPtr[row] == rowPtr[row + 1])
				row++;
			cursor = rowPtr[row];
		}

		public boolean hasNext() {
			return cursor < rowData.length;
		}

		public MatrixEntry next() {
			entry.update(row, cursor);

			// Next position is in the same row
			if (cursor < rowPtr[row + 1] - 1)
				cursor++;

			// Next position is at the following (non-empty) row
			else {
				row++;
				nextNonEmptyRow();
			}

			return entry;
		}

		public void remove() {
			entry.set(0);
		}

	}

	// example: http://netlib.org/linalg/html_templates/node91.html
	public static void main(String[] args) {
		int[][] rnz = { { 0, 4 }, { 5, 0, 1 }, { 1, 2, 3 }, { 0, 2, 3, 4 }, { 1, 3, 4, 5 }, { 1, 4, 5 } };
		int[][] cnz = { { 0, 1, 3 }, { 1, 2, 4, 5 }, { 2, 3 }, { 2, 3, 4 }, { 0, 3, 4, 5 }, { 1, 4, 5 } };

		SparseMatrix mat = new SparseMatrix(6, 6, rnz, cnz);
		mat.set(0, 0, 10);
		mat.set(0, 4, -2);
		mat.set(1, 0, 3);
		mat.set(1, 1, 9);
		mat.set(1, 5, 3);
		mat.set(2, 1, 7);
		mat.set(2, 2, 8);
		mat.set(2, 3, 7);
		mat.set(3, 0, 3);
		mat.set(3, 2, 8);
		mat.set(3, 3, 7);
		mat.set(3, 4, 5);
		mat.set(4, 1, 8);
		mat.set(4, 3, 9);
		mat.set(4, 4, 9);
		mat.set(4, 5, 13);
		mat.set(5, 1, 4);
		mat.set(5, 4, 2);
		mat.set(5, 5, -1);

		Logs.debug(mat);
		Logs.debug(new SparseMatrix(mat));

		Logs.debug(mat.row(1));
		Logs.debug(mat.rowSize(1));

		Logs.debug(mat.col(1));
		Logs.debug(mat.colSize(1));
	}
}
