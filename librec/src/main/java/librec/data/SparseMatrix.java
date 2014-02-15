package librec.data;

import happy.coding.io.Logs;
import happy.coding.math.Stats;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Data Structure: Sparse Matrix whose implementation is modified from M4J
 * library
 * 
 * <ul>
 * <li><a href="http://netlib.org/linalg/html_templates/node91.html">Compressed
 * Row Storage (CRS)</a></li>
 * <li><a href="http://netlib.org/linalg/html_templates/node92.html">Compressed
 * Col Storage (CCS)</a></li>
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

	// is CCS enabled
	protected boolean isCCSUsed = false;

	/**
	 * Construct a sparse matrix with both CRS and CCS structures
	 */
	public SparseMatrix(int rows, int cols, Table<Integer, Integer, Double> dataTable, Multimap<Integer, Integer> colMap) {
		numRows = rows;
		numCols = cols;

		construct(dataTable, colMap);
	}

	/**
	 * Construct a sparse matrix with only CRS structures
	 */
	public SparseMatrix(int rows, int cols, Table<Integer, Integer, Double> dataTable) {
		this(rows, cols, dataTable, null);
	}

	/**
	 * Define a sparse matrix without data, only use for {@code transpose}
	 * method
	 * 
	 */
	private SparseMatrix(int rows, int cols) {
		numRows = rows;
		numCols = cols;
	}

	public SparseMatrix(SparseMatrix mat) {
		this(mat, true);
	}

	/**
	 * Construct a sparse matrix from another sparse matrix
	 * 
	 * @param mat
	 *            the original sparse matrix
	 * @param deap
	 *            whether to copy the CCS structures
	 */
	public SparseMatrix(SparseMatrix mat, boolean deap) {
		numRows = mat.numRows;
		numCols = mat.numCols;

		copyCRS(mat.rowData, mat.rowPtr, mat.colInd);

		if (deap && mat.isCCSUsed)
			copyCCS(mat.colData, mat.colPtr, mat.rowInd);
	}

	private void copyCRS(double[] data, int[] ptr, int[] idx) {
		rowData = new double[data.length];
		for (int i = 0; i < rowData.length; i++)
			rowData[i] = data[i];

		rowPtr = new int[ptr.length];
		for (int i = 0; i < rowPtr.length; i++)
			rowPtr[i] = ptr[i];

		colInd = new int[idx.length];
		for (int i = 0; i < colInd.length; i++)
			colInd[i] = idx[i];
	}

	private void copyCCS(double[] data, int[] ptr, int[] idx) {
		colData = new double[data.length];
		for (int i = 0; i < colData.length; i++)
			colData[i] = data[i];

		colPtr = new int[ptr.length];
		for (int i = 0; i < colPtr.length; i++)
			colPtr[i] = ptr[i];

		rowInd = new int[idx.length];
		for (int i = 0; i < rowInd.length; i++)
			rowInd[i] = idx[i];
	}

	public SparseMatrix clone() {
		return new SparseMatrix(this);
	}

	public SparseMatrix transpose() {
		if (isCCSUsed) {
			SparseMatrix tr = new SparseMatrix(numCols, numRows);

			tr.copyCRS(this.rowData, this.rowPtr, this.colInd);
			tr.copyCCS(this.colData, this.colPtr, this.rowInd);

			return tr;
		} else {
			Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
			for (MatrixEntry me : this)
				dataTable.put(me.column(), me.row(), me.get());
			return new SparseMatrix(numCols, numRows, dataTable);
		}
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

	private void construct(Table<Integer, Integer, Double> dataTable, Multimap<Integer, Integer> colMap) {
		int nnz = dataTable.size();

		// CRS
		rowPtr = new int[numRows + 1];
		colInd = new int[nnz];
		rowData = new double[nnz];

		int j = 0;
		for (int i = 1; i <= numRows; ++i) {
			Set<Integer> cols = dataTable.row(i - 1).keySet();
			rowPtr[i] = rowPtr[i - 1] + cols.size();

			for (int col : cols) {
				colInd[j++] = col;
				if (col < 0 || col >= numCols)
					throw new IllegalArgumentException("colInd[" + j + "]=" + col
							+ ", which is not a valid column index");
			}

			Arrays.sort(colInd, rowPtr[i - 1], rowPtr[i]);
		}

		// CCS
		if (colMap != null) {
			colPtr = new int[numCols + 1];
			rowInd = new int[nnz];
			colData = new double[nnz];
			isCCSUsed = true;

			j = 0;
			for (int i = 1; i <= numCols; ++i) {
				// dataTable.col(i-1) is very time-consuming
				Collection<Integer> rows = colMap.get(i - 1);
				colPtr[i] = colPtr[i - 1] + rows.size();

				for (int row : rows) {
					rowInd[j++] = row;
					if (row < 0 || row >= numRows)
						throw new IllegalArgumentException("rowInd[" + j + "]=" + row
								+ ", which is not a valid row index");
				}

				Arrays.sort(rowInd, colPtr[i - 1], colPtr[i]);
			}
		}

		// set data
		for (Cell<Integer, Integer, Double> en : dataTable.cellSet()) {
			int row = en.getRowKey();
			int col = en.getColumnKey();
			double val = en.getValue();

			set(row, col, val);
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

		if (isCCSUsed) {
			index = getCCSIndex(row, col);
			colData[index] = val;
		}
	}

	public void add(int row, int col, double val) {
		int index = getCRSIndex(row, col);
		rowData[index] += val;

		if (isCCSUsed) {
			index = getCCSIndex(row, col);
			colData[index] += val;
		}
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
	public SparseVector column(int col) {

		SparseVector sv = new SparseVector(numRows);

		if (isCCSUsed) {
			for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
				int row = rowInd[j];
				double val = get(row, col);
				if (val != 0.0)
					sv.set(row, val);
			}
		} else {
			for (int row = 0; row < numRows; row++) {
				double val = get(row, col);
				if (val != 0.0)
					sv.set(row, val);
			}
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
	public int columnSize(int col) {

		int size = 0;

		if (isCCSUsed) {
			for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
				int row = rowInd[j];
				double val = get(row, col);
				if (val != 0.0)
					size++;
			}
		} else {
			for (int row = 0; row < numRows; row++) {
				double val = get(row, col);
				if (val != 0.0)
					size++;
			}
		}

		return size;
	}

	/**
	 * @return the sum of matrix data
	 */
	public double sum() {
		return Stats.sum(rowData);
	}

	/**
	 * @return the mean of matrix data
	 */
	public double mean() {
		return sum() / size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%d\t%d\t%d\n", new Object[] { numRows, numCols, size() }));

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

	public static void main(String[] args) {
		// test data matrix
		// {10, 0, 0, 0, -2, 0},
		// { 3, 9, 0, 0, 0, 3},
		// { 0, 7, 8, 7, 0, 0},
		// { 3, 0, 8, 7, 5, 0},
		// { 0, 8, 0, 9, 9, 13},
		// { 0, 4, 0, 0, 2, -1}
		// val = {10, -2, 3, 9, 3, 7, 8, 7, 3, 8, 7, 5, 8, 9, 9, 13, 4, 2, -1}
		// col_ind = {1, 5, 1, 2, 6, 2, 3, 4, 1, 3, 4, 5, 2, 4, 5, 6, 2, 5, 6}
		// row_pt = {1, 3, 6, 9, 13, 17, 20}

		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		dataTable.put(0, 0, 10.0);
		dataTable.put(0, 4, -2.0);
		dataTable.put(1, 0, 3.0);
		dataTable.put(1, 1, 9.0);
		dataTable.put(1, 5, 3.0);
		dataTable.put(2, 1, 7.0);
		dataTable.put(2, 2, 8.0);
		dataTable.put(2, 3, 7.0);
		dataTable.put(3, 0, 3.0);
		dataTable.put(3, 2, 8.);
		dataTable.put(3, 3, 7.);
		dataTable.put(3, 4, 5.);
		dataTable.put(4, 1, 8.);
		dataTable.put(4, 3, 9.);
		dataTable.put(4, 4, 9.);
		dataTable.put(4, 5, 13.);
		dataTable.put(5, 1, 4.);
		dataTable.put(5, 4, 2.);
		dataTable.put(5, 5, -1.);

		colMap.put(0, 0);
		colMap.put(0, 1);
		colMap.put(0, 3);

		colMap.put(1, 1);
		colMap.put(1, 2);
		colMap.put(1, 4);
		colMap.put(1, 5);

		colMap.put(2, 2);
		colMap.put(2, 3);

		colMap.put(3, 2);
		colMap.put(3, 3);
		colMap.put(3, 4);

		colMap.put(4, 0);
		colMap.put(4, 3);
		colMap.put(4, 4);
		colMap.put(4, 5);

		colMap.put(5, 1);
		colMap.put(5, 4);
		colMap.put(5, 5);

		SparseMatrix mat = new SparseMatrix(6, 6, dataTable);
		SparseMatrix mat2 = new SparseMatrix(6, 6, dataTable, colMap);

		Logs.debug(mat);
		Logs.debug(mat.transpose());
		Logs.debug(mat2.transpose());

		Logs.debug(mat.row(1));
		Logs.debug(mat2.row(1));

		Logs.debug(mat.column(1));
		Logs.debug(mat2.column(1));

	}
}
