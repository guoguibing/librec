package librec.data;

import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.math.Randoms;

import java.util.Arrays;

/**
 * Data Structure: dense matrix
 * 
 * A big reason that we do not adopt original DenseMatrix from M4J libraray is
 * because the latter using one-dimensional array to store data, which will
 * often cause OutOfMemory exception due to the over the maximum length of
 * one-dimensional Java array.
 * 
 * @author guoguibing
 * 
 */
public class DenseMatrix {

	protected int numRows, numCols;
	protected double[][] data;

	public DenseMatrix(int numRows, int numColumns) {
		this.numRows = numRows;
		this.numCols = numColumns;

		data = new double[numRows][numColumns];
	}

	public DenseMatrix(double[][] array) {
		this(array.length, array[0].length);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				data[i][j] = array[i][j];
		// data = Arrays.copyOf(array, array.length); does not work, as it
		// produces a shallow copy
	}

	public DenseMatrix(DenseMatrix mat) {
		this(mat.data);
	}

	public DenseMatrix clone() {
		return new DenseMatrix(this);
	}

	public static DenseMatrix eye(int dim) {
		DenseMatrix mat = new DenseMatrix(dim, dim);
		for (int i = 0; i < mat.numRows; i++)
			mat.set(i, i, 1.0);

		return mat;
	}

	/**
	 * initialize a dense matrix with small Guassian values <br/>
	 * 
	 * <strong>NOTE:</strong> small initial values make it easier to train a
	 * model; otherwise a very small learning rate may be needed (especially
	 * when the number of factors is large) which can cause bad performance.
	 */
	public void init(double mean, double sigma) {
		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				data[i][j] = Randoms.gaussian(mean, sigma);
	}

	/**
	 * initialize a dense matrix with small random values in (0, range)
	 */
	public void init(double range) {

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				data[i][j] = Randoms.uniform(0, range);
	}

	/**
	 * initialize a dense matrix with small random values in (0, 1)
	 */
	public void init() {
		init(1.0);
	}

	public double[][] getData() {
		return data;
	}

	public int numRows() {
		return numRows;
	}

	public int numCols() {
		return numCols;
	}

	/**
	 * @param rowId
	 *            row id
	 * @return a copy of row data as a dense vector
	 */
	public DenseVector row(int rowId) {
		return row(rowId, true);
	}

	/**
	 * 
	 * @param rowId
	 *            row id
	 * @param deep
	 *            whether to copy data or only shallow copy
	 * @return a vector of a specific row
	 */
	public DenseVector row(int rowId, boolean deep) {
		return new DenseVector(data[rowId], deep);
	}

	/**
	 * @param col
	 *            column id
	 * @return a copy of column data as a dense vector
	 */
	public DenseVector column(int col) {
		DenseVector vec = new DenseVector(numRows);

		for (int i = 0; i < numRows; i++)
			vec.set(i, get(i, col));

		return vec;
	}

	/**
	 * Compute mean of a column of the current matrix
	 * 
	 * @param col
	 *            column id
	 * @return mean of a column of the current matrix
	 */
	public double columnMean(int col) {
		double sum = 0.0;

		for (int i = 0; i < numRows; i++)
			sum += data[i][col];

		return sum / numRows;
	}

	/**
	 * @return the matrix norm-2
	 */
	public double norm() {
		double result = 0;

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				result += data[i][j] * data[i][j];

		return Math.sqrt(result);
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

		assert m.numCols == n.numCols;

		double result = 0;

		for (int j = 0, k = m.numCols; j < k; j++)
			result += m.get(mrow, j) * n.get(nrow, j);

		return result;
	}

	/**
	 * column x column of two matrix
	 * 
	 * @param m
	 *            the first matrix
	 * @param mcol
	 *            column of the first matrix
	 * @param n
	 *            the second matrix
	 * @param ncol
	 *            column of the second matrix
	 * @return inner product of two column vectors
	 */
	public static double colMult(DenseMatrix m, int mcol, DenseMatrix n, int ncol) {

		assert m.numRows == n.numRows;

		double result = 0;
		for (int j = 0, k = m.numRows; j < k; j++)
			result += m.get(j, mcol) * n.get(j, ncol);

		return result;
	}

	/**
	 * dot product of row x col between two matrices
	 * 
	 * @param m
	 *            the first matrix
	 * @param mrow
	 *            row id of the first matrix
	 * @param n
	 *            the second matrix
	 * @param ncol
	 *            column id of the second matrix
	 * @return dot product of row of the first matrix and column of the second
	 *         matrix
	 */
	public static double product(DenseMatrix m, int mrow, DenseMatrix n, int ncol) {
		assert m.numCols == n.numRows;

		double result = 0;
		for (int j = 0; j < m.numCols; j++)
			result += m.get(mrow, j) * n.get(j, ncol);

		return result;
	}

	/**
	 * Matrix multiplication with a dense matrix
	 * 
	 * @param mat
	 *            a dense matrix
	 * @return a dense matrix with results of matrix multiplication
	 */
	public DenseMatrix mult(DenseMatrix mat) {
		assert this.numCols == mat.numRows;

		DenseMatrix result = new DenseMatrix(this.numRows, mat.numCols);

		for (int i = 0; i < result.numRows; i++) {
			for (int j = 0; j < result.numCols; j++) {

				double product = 0;
				for (int k = 0; k < this.numCols; k++)
					product += data[i][k] * mat.data[k][j];

				result.set(i, j, product);
			}
		}

		return result;
	}

	/**
	 * Matrix multiplication with a sparse matrix
	 * 
	 * @param mat
	 *            a sparse matrix
	 * @return a dense matrix with results of matrix multiplication
	 */
	public DenseMatrix mult(SparseMatrix mat) {
		assert this.numCols == mat.numRows;

		DenseMatrix result = new DenseMatrix(this.numRows, mat.numCols);

		for (int i = 0; i < result.numRows; i++) {
			for (int j = 0; j < result.numCols; j++) {

				double product = 0;
				SparseVector col = mat.column(j);
				for (int k : col.getIndex())
					product += data[i][k] * col.get(k);

				result.set(i, j, product);
			}
		}

		return result;
	}

	public DenseVector mult(DenseVector vec) {
		assert this.numCols == vec.size;

		DenseVector result = new DenseVector(this.numRows);
		for (int i = 0; i < this.numRows; i++)
			result.set(i, row(i, false).inner(vec));

		return result;
	}

	/**
	 * Matrix multiplication of a sparse matrix by a dense matrix
	 * 
	 * @param sm
	 *            a sparse matrix
	 * @param dm
	 *            a dense matrix
	 * @return a dense matrix with the results of matrix multiplication
	 */
	public static DenseMatrix mult(SparseMatrix sm, DenseMatrix dm) {
		assert sm.numCols == dm.numRows;

		DenseMatrix result = new DenseMatrix(sm.numRows, dm.numCols);

		for (int i = 0; i < result.numRows; i++) {
			SparseVector row = sm.row(i);
			for (int j = 0; j < result.numCols; j++) {

				double product = 0;
				for (int k : row.getIndex())
					product += row.get(k) * dm.data[k][j];

				result.set(i, j, product);
			}
		}

		return result;

	}

	public double get(int row, int col) {
		return data[row][col];
	}

	public void set(int row, int col, double val) {
		data[row][col] = val;
	}

	public void add(int row, int col, double val) {
		data[row][col] += val;
	}

	public DenseMatrix scale(double val) {
		DenseMatrix mat = new DenseMatrix(numRows, numCols);
		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				mat.data[i][j] = this.data[i][j] * val;

		return mat;
	}

	public DenseMatrix add(DenseMatrix mat) {
		assert numRows == mat.numRows;
		assert numCols == mat.numCols;

		DenseMatrix result = new DenseMatrix(numRows, numCols);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numCols; j++)
				result.data[i][j] = data[i][j] + mat.data[i][j];

		return result;
	}

	/**
	 * @return the Cholesky decomposition of the current matrix
	 */
	public DenseMatrix cholesky() {
		if (this.numRows != this.numCols)
			throw new RuntimeException("Matrix is not square");

		int n = numRows;
		DenseMatrix L = new DenseMatrix(n, n);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= i; j++) {
				double sum = 0.0;
				for (int k = 0; k < j; k++)
					sum += L.get(i, k) * L.get(j, k);

				double val = i == j ? Math.sqrt(data[i][i] - sum) : (data[i][j] - sum) / L.get(j, j);
				L.set(i, j, val);
			}
			if (Double.isNaN(L.get(i, i)))
				return null;
		}

		return L.transpose();
	}

	// generate a new transposed matrix
	public DenseMatrix transpose() {
		DenseMatrix mat = new DenseMatrix(numCols, numRows);

		for (int i = 0; i < mat.numRows; i++)
			for (int j = 0; j < mat.numCols; j++)
				mat.set(i, j, get(j, i));

		return mat;
	}

	/**
	 * @return a covariance matrix of the current matrix
	 */
	public DenseMatrix cov() {
		DenseMatrix mat = new DenseMatrix(numCols, numCols);

		for (int i = 0; i < numCols; i++) {
			for (int j = i; j < numCols; j++) {
				DenseVector xi = this.column(i);
				DenseVector yi = this.column(j);

				double val = xi.sub(xi.mean()).inner(yi.sub(yi.mean()));
				val /= xi.size - 1;

				mat.set(i, j, val);
				mat.set(j, i, val);
			}
		}

		return mat;
	}

	/**
	 * Compute the inverse of a matrix by LU decomposition
	 * 
	 * @return the inverse matrix of current matrix
	 */
	public DenseMatrix inverse() {
		if (numRows != numCols)
			throw new RuntimeException("Only square matrix can do inversion");

		int n = numRows;
		DenseMatrix mat = new DenseMatrix(this);

		if (n == 1) {
			mat.set(0, 0, 1.0 / mat.get(0, 0));
			return mat;
		}

		int row[] = new int[n];
		int col[] = new int[n];
		double temp[] = new double[n];
		int hold, I_pivot, J_pivot;
		double pivot, abs_pivot;

		// set up row and column interchange vectors
		for (int k = 0; k < n; k++) {
			row[k] = k;
			col[k] = k;
		}
		// begin main reduction loop
		for (int k = 0; k < n; k++) {
			// find largest element for pivot
			pivot = mat.get(row[k], col[k]);
			I_pivot = k;
			J_pivot = k;
			for (int i = k; i < n; i++) {
				for (int j = k; j < n; j++) {
					abs_pivot = Math.abs(pivot);
					if (Math.abs(mat.get(row[i], col[j])) > abs_pivot) {
						I_pivot = i;
						J_pivot = j;
						pivot = mat.get(row[i], col[j]);
					}
				}
			}
			if (Math.abs(pivot) < 1.0E-10)
				throw new RuntimeException("Matrix is singular !");

			hold = row[k];
			row[k] = row[I_pivot];
			row[I_pivot] = hold;
			hold = col[k];
			col[k] = col[J_pivot];
			col[J_pivot] = hold;

			// reduce about pivot
			mat.set(row[k], col[k], 1.0 / pivot);
			for (int j = 0; j < n; j++) {
				if (j != k) {
					mat.set(row[k], col[j], mat.get(row[k], col[j]) * mat.get(row[k], col[k]));
				}
			}
			// inner reduction loop
			for (int i = 0; i < n; i++) {
				if (k != i) {
					for (int j = 0; j < n; j++) {
						if (k != j) {

							double val = mat.get(row[i], col[j]) - mat.get(row[i], col[k]) * mat.get(row[k], col[j]);
							mat.set(row[i], col[j], val);
						}
					}
					mat.set(row[i], col[k], -mat.get(row[i], col[k]) * mat.get(row[k], col[k]));
				}
			}
		}
		// end main reduction loop

		// unscramble rows
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++)
				temp[col[i]] = mat.get(row[i], j);

			for (int i = 0; i < n; i++)
				mat.set(i, j, temp[i]);

		}

		// unscramble columns
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++)
				temp[row[j]] = mat.get(i, col[j]);

			for (int j = 0; j < n; j++)
				mat.set(i, j, temp[j]);
		}

		return mat;
	}

	public DenseMatrix inv() {
		if (this.numRows != this.numCols)
			throw new RuntimeException("Dimensions disagree");

		int n = this.numRows;

		DenseMatrix mat = DenseMatrix.eye(n);

		if (n == 1) {
			mat.set(0, 0, 1 / this.get(0, 0));
			return mat;
		}

		DenseMatrix b = new DenseMatrix(this);

		for (int i = 0; i < n; i++) {
			// find pivot:
			double mag = 0;
			int pivot = -1;

			for (int j = i; j < n; j++) {
				double mag2 = Math.abs(b.get(j, i));
				if (mag2 > mag) {
					mag = mag2;
					pivot = j;
				}
			}

			// no pivot (error):
			if (pivot == -1 || mag == 0)
				return mat;

			// move pivot row into position:
			if (pivot != i) {
				double temp;
				for (int j = i; j < n; j++) {
					temp = b.get(i, j);
					b.set(i, j, b.get(pivot, j));
					b.set(pivot, j, temp);
				}

				for (int j = 0; j < n; j++) {
					temp = mat.get(i, j);
					mat.set(i, j, mat.get(pivot, j));
					mat.set(pivot, j, temp);
				}
			}

			// normalize pivot row:
			mag = b.get(i, i);
			for (int j = i; j < n; j++)
				b.set(i, j, b.get(i, j) / mag);

			for (int j = 0; j < n; j++)
				mat.set(i, j, mat.get(i, j) / mag);

			// eliminate pivot row component from other rows:
			for (int k = 0; k < n; k++) {
				if (k == i)
					continue;

				double mag2 = b.get(k, i);

				for (int j = i; j < n; j++)
					b.set(k, j, b.get(k, j) - mag2 * b.get(i, j));

				for (int j = 0; j < n; j++)
					mat.set(k, j, mat.get(k, j) - mag2 * mat.get(i, j));
			}
		}

		return mat;
	}

	/**
	 * set one value to the whole row
	 * 
	 * @param row
	 *            row id
	 * @param val
	 *            value to be set
	 */
	public void setRow(int row, double val) {
		Arrays.fill(data[row], val);
	}

	@Override
	public String toString() {
		return Strings.toString(data);
	}

	public static void main(String[] args) throws Exception {
		double[][] data = { { 4, 2, 0.6 }, { 4.2, 2.1, .59 }, { 3.9, 2.0, .58 }, { 4.3, 2.1, .62 }, { 4.1, 2.2, .63 } };

		// expected cov results: {{0.025, 0.0075, 0.00175}, {0.0075, 0.0070,
		// 0.00135}, {0.00175, 0.00135, 0.00043}}

		DenseMatrix mat = new DenseMatrix(data);
		Logs.debug(mat);
		data[1][2] = 100000;
		Logs.debug(mat);

		Logs.debug(mat.cov());

		// matrix inversion
		data = new double[][] { { 1, 0, 4 }, { 1, 1, 6 }, { -3, 0, -10 } };
		mat = new DenseMatrix(data);

		// expected invert results: {{-5, 0, -2}, {-4, 1, -1}, {1.5, 0, 0.5}}
		Logs.debug(mat);
		Logs.debug(mat.inv());
		Logs.debug(mat.inverse());
	}
}
