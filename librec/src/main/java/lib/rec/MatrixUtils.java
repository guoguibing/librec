package lib.rec;

import happy.coding.math.Randoms;

import java.util.Arrays;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Utility class for Matrix
 * 
 * @author guoguibing
 * 
 */
public class MatrixUtils {

	/**
	 * initialize a dense matrix with small Guassian values <br/>
	 * 
	 * <strong>NOTE:</strong> small initial values make it easier to train a
	 * model; otherwise a very small learning rate may be needed (especially
	 * when the number of factors is large) which can cause bad performance.
	 */
	public static void init(DenseMatrix m, double mean, double sigma) {

		double[] data = m.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.gaussian(mean, sigma);
	}

	/**
	 * initialize a dense matrix with small random values
	 */
	public static void init(DenseMatrix m) {

		double[] data = m.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.uniform(0.0, 0.01);
	}

	/**
	 * initialize a dense vector with Gaussian values
	 */
	public static void init(DenseVector v, double mean, double sigma) {
		double[] data = v.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.gaussian(mean, sigma);
	}

	/**
	 * get a row sparse vector of a matrix
	 * 
	 * @param m
	 *            matrix
	 * @param row
	 *            row id
	 * @return a sparse vector of {index, value}
	 * 
	 */
	public static SparseVector row(CompRowMatrix m, int row) {

		int[] row_ptr = m.getRowPointers();
		int[] col_idx = m.getColumnIndices();

		int start = row_ptr[row];
		int end = row_ptr[row + 1];

		SparseVector sv = new SparseVector(m.numColumns());

		for (int j = start; j < end; j++) {
			int col = col_idx[j];
			double val = m.get(row, col);
			if (val != 0.0)
				sv.set(col, val);
		}

		return sv;
	}

	/**
	 * get a row sparse vector of a matrix with a specific index exclusive
	 * 
	 * @param m
	 *            matrix
	 * @param row
	 *            row id
	 * 
	 * @param excep
	 *            exclusive index
	 * 
	 * @return a sparse vector of {index, value}
	 * 
	 */
	public static SparseVector row(CompRowMatrix m, int row, int except) {

		int[] row_ptr = m.getRowPointers();
		int[] col_idx = m.getColumnIndices();

		int start = row_ptr[row];
		int end = row_ptr[row + 1];

		SparseVector sv = new SparseVector(m.numColumns());

		for (int j = start; j < end; j++) {
			int col = col_idx[j];
			if (col != except) {
				double val = m.get(row, col);
				if (val != 0.0)
					sv.set(col, val);
			}
		}

		return sv;
	}

	public static DenseVector row(DenseMatrix m, int row) {

		DenseVector dv = new DenseVector(m.numColumns());

		for (int j = 0; j < m.numColumns(); j++) {
			double val = m.get(row, j);
			if (val != 0.0)
				dv.set(j, val);
		}

		return dv;
	}

	/**
	 * get a column sparse vector of a matrix
	 * 
	 * @param m
	 *            matrix
	 * @param col
	 *            column id
	 * @return a list of row indices whose corresponding matrix values are not 0
	 */
	public static SparseVector col(CompRowMatrix m, int col) {

		SparseVector sv = new SparseVector(m.numRows());

		for (int row = 0, n = m.numRows(); row < n; row++) {

			double val = m.get(row, col);
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
	public static SparseVector colByStorage(CompRowMatrix m, int col) {

		int[] row_ptr = m.getRowPointers();
		int[] col_idx = m.getColumnIndices().clone();

		SparseVector sv = new SparseVector(m.numRows());

		// slower as col_idx is much greater than num of rows
		for (int idx = 0; idx < col_idx.length; idx++) {

			int j = col_idx[idx];
			if (j == col) {
				// find row id
				int row = Arrays.binarySearch(row_ptr, idx);

				if (row < 0)
					row = -(row + 1) - 1;

				double val = m.get(row, col);
				if (val != 0.0)
					sv.set(row, val);
			}
		}
		return sv;
	}

	/**
	 * row x row of two matrix
	 * 
	 * @param m
	 *            the first matrix
	 * @param mrow
	 *            row of the first matrix
	 * @param n
	 *            the second matrix
	 * @param nrow
	 *            row of the second matrix
	 * @return inner product of two row vectors
	 */
	public static double rowMult(DenseMatrix m, int mrow, DenseMatrix n, int nrow) {

		assert m.numColumns() == n.numColumns();

		double result = 0;

		double[] mdata = m.getData();
		double[] ndata = n.getData();

		// NOTE: the data storage in DenseMatrix is column-wise
		int mRows = m.numRows(), nRows = n.numRows();
		for (int j = 0, k = m.numColumns(); j < k; j++)
			result += mdata[mrow + j * mRows] * ndata[nrow + j * nRows];

		return result;
	}

	/**
	 * set one value to the whole row of a matrix
	 * 
	 * @param m
	 *            matrix
	 * @param row
	 *            row id
	 * @param val
	 *            value to be set
	 */
	public static void setOneValue(DenseMatrix m, int row, double val) {
		for (int c = 0, cm = m.numColumns(); c < cm; c++)
			m.set(row, c, val);
	}

	/**
	 * get a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param m
	 *            symmetric matrix
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 * @return a value at (row, col) if row<col; otherwise at (col, row)
	 */
	public static double get(FlexCompRowMatrix m, int row, int col) {
		return row < col ? m.get(row, col) : m.get(col, row);
	}

	/**
	 * set a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param m
	 *            symmetric matrix
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public static void set(FlexCompRowMatrix m, int row, int col, double val) {
		if (row < col)
			m.set(row, col, val);
		else
			m.set(col, row, val);
	}

	/**
	 * add a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param m
	 *            symmetric matrix
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public static void add(FlexCompRowMatrix m, int row, int col, double val) {
		if (row < col)
			m.add(row, col, val);
		else
			m.add(col, row, val);
	}

	/**
	 * find a set of items similar to item i
	 * 
	 * @param m
	 *            upper symmetric correlation matrix
	 * @param i
	 *            item id
	 * @return a sparse vector
	 */
	public static SparseVector nn(FlexCompRowMatrix m, int i) {
		SparseVector nv = m.getRow(i);
		for (int j = 0; j < i; j++) {
			double val = m.get(j, i);
			if (val != 0)
				nv.set(j, val);
		}

		return nv;
	}

}
