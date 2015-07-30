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
import java.util.Arrays;

import librec.util.Randoms;
import librec.util.Strings;

/**
 * Data Structure: dense matrix <br>
 * 
 * A big reason that we do not adopt original DenseMatrix from M4J libraray is because the latter using one-dimensional
 * array to store data, which will often cause OutOfMemory exception due to the limit of maximum length of a
 * one-dimensional Java array.
 * 
 * @author guoguibing
 * 
 */
public class DenseMatrix implements Serializable {

	private static final long serialVersionUID = -2069621030647530185L;

	// dimension
	protected int numRows, numColumns;
	// read data
	protected double[][] data;

	/**
	 * Construct a dense matrix with specified dimensions
	 * 
	 * @param numRows
	 *            number of rows
	 * @param numColumns
	 *            number of columns
	 */
	public DenseMatrix(int numRows, int numColumns) {
		this.numRows = numRows;
		this.numColumns = numColumns;

		data = new double[numRows][numColumns];
	}

	/**
	 * Construct a dense matrix by copying data from a given 2D array
	 * 
	 * @param array
	 *            data array
	 */
	public DenseMatrix(double[][] array) {
		this(array.length, array[0].length);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				data[i][j] = array[i][j];
	}

	/**
	 * Construct a dense matrix by a shallow copy of a data array
	 */
	public DenseMatrix(double[][] array, int numRows, int numColumns) {
		this.numRows = numRows;
		this.numColumns = numColumns;

		this.data = array;
	}

	/**
	 * Construct a dense matrix by copying data from a given matrix
	 * 
	 * @param mat
	 *            input matrix
	 */
	public DenseMatrix(DenseMatrix mat) {
		this(mat.data);
	}

	/**
	 * Make a deep copy of current matrix
	 */
	public DenseMatrix clone() {
		return new DenseMatrix(this);
	}

	/**
	 * Construct an identity matrix
	 * 
	 * @param dim
	 *            dimension
	 * @return an identity matrix
	 */
	public static DenseMatrix eye(int dim) {
		DenseMatrix mat = new DenseMatrix(dim, dim);
		for (int i = 0; i < mat.numRows; i++)
			mat.set(i, i, 1.0);

		return mat;
	}

	/**
	 * Initialize a dense matrix with small Guassian values <br/>
	 * 
	 * <strong>NOTE:</strong> small initial values make it easier to train a model; otherwise a very small learning rate
	 * may be needed (especially when the number of factors is large) which can cause bad performance.
	 */
	public void init(double mean, double sigma) {
		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				data[i][j] = Randoms.gaussian(mean, sigma);
	}

	/**
	 * initialize a dense matrix with small random values in (0, range)
	 */
	public void init(double range) {

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				data[i][j] = Randoms.uniform(0, range);
	}

	/**
	 * initialize a dense matrix with small random values in (0, 1)
	 */
	public void init() {
		init(1.0);
	}

	/**
	 * @return number of rows
	 */
	public int numRows() {
		return numRows;
	}

	/**
	 * @return number of columns
	 */
	public int numColumns() {
		return numColumns;
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
	 *            whether to copy data or only shallow copy for executing speedup purpose
	 * @return a vector of a specific row
	 */
	public DenseVector row(int rowId, boolean deep) {
		return new DenseVector(data[rowId], deep);
	}

	/**
	 * @param column
	 *            column id
	 * @return a copy of column data as a dense vector
	 */
	public DenseVector column(int column) {
		DenseVector vec = new DenseVector(numRows);

		for (int i = 0; i < numRows; i++)
			vec.set(i, data[i][column]);

		return vec;
	}

	/**
	 * Compute mean of a column of the current matrix
	 * 
	 * @param column
	 *            column id
	 * @return mean of a column of the current matrix
	 */
	public double columnMean(int column) {
		double sum = 0.0;

		for (int i = 0; i < numRows; i++)
			sum += data[i][column];

		return sum / numRows;
	}

	/**
	 * @return the matrix norm-2
	 */
	public double norm() {
		double res = 0;

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				res += data[i][j] * data[i][j];

		return Math.sqrt(res);
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
		assert m.numColumns == n.numColumns;

		double res = 0;
		for (int j = 0, k = m.numColumns; j < k; j++)
			res += m.get(mrow, j) * n.get(nrow, j);

		return res;
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

		double res = 0;
		for (int j = 0, k = m.numRows; j < k; j++)
			res += m.get(j, mcol) * n.get(j, ncol);

		return res;
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
	 * @return dot product of row of the first matrix and column of the second matrix
	 */
	public static double product(DenseMatrix m, int mrow, DenseMatrix n, int ncol) {
		assert m.numColumns == n.numRows;

		double res = 0;
		for (int j = 0; j < m.numColumns; j++)
			res += m.get(mrow, j) * n.get(j, ncol);

		return res;
	}

	/**
	 * @return Kronecker product of two arbitrary matrices
	 */
	public static DenseMatrix kroneckerProduct(DenseMatrix M, DenseMatrix N) {
		DenseMatrix res = new DenseMatrix(M.numRows * N.numRows, M.numColumns * N.numColumns);
		for (int i = 0; i < M.numRows; i++) {
			for (int j = 0; j < M.numColumns; j++) {
				double Mij = M.get(i, j);
				// Mij*N
				for (int ni = 0; ni < N.numRows; ni++) {
					for (int nj = 0; nj < N.numColumns; nj++) {
						int row = i * N.numRows + ni;
						int col = j * N.numColumns + nj;

						res.set(row, col, Mij * N.get(ni, nj));
					}
				}
			}
		}

		return res;
	}

	/**
	 * @return Khatri-Rao product of two matrices
	 */
	public static DenseMatrix khatriRaoProduct(DenseMatrix M, DenseMatrix N) throws Exception {
		if (M.numColumns != N.numColumns)
			throw new Exception("The number of columns of two matrices is not equal!");

		DenseMatrix res = new DenseMatrix(M.numRows * N.numRows, M.numColumns);
		for (int j = 0; j < M.numColumns; j++) {
			for (int i = 0; i < M.numRows; i++) {
				double Mij = M.get(i, j);

				// Mij* Nj
				for (int ni = 0; ni < N.numRows; ni++) {
					int row = ni + i * N.numRows;

					res.set(row, j, Mij * N.get(ni, j));
				}
			}
		}
		return res;
	}

	/**
	 * @return Hadamard product of two matrices
	 */
	public static DenseMatrix hadamardProduct(DenseMatrix M, DenseMatrix N) throws Exception {
		if (M.numRows != N.numRows || M.numColumns != N.numColumns)
			throw new Exception("The dimensions of two matrices are not consistent!");

		DenseMatrix res = new DenseMatrix(M.numRows, M.numColumns);

		for (int i = 0; i < M.numRows; i++) {
			for (int j = 0; j < M.numColumns; j++) {
				res.set(i, j, M.get(i, j) * N.get(i, j));
			}
		}

		return res;
	}

	/**
	 * @return the result of {@code A^T A}
	 */
	public DenseMatrix transMult() {
		DenseMatrix res = new DenseMatrix(numColumns, numColumns);

		for (int i = 0; i < numColumns; i++) {
			// inner product of row i and row k
			for (int k = 0; k < numColumns; k++) {

				double val = 0;
				for (int j = 0; j < numRows; j++) {
					val += get(j, i) * get(j, k);
				}

				res.set(i, k, val);
			}
		}

		return res;
	}

	/**
	 * Matrix multiplication with a dense matrix
	 * 
	 * @param mat
	 *            a dense matrix
	 * @return a dense matrix with results of matrix multiplication
	 */
	public DenseMatrix mult(DenseMatrix mat) {
		assert this.numColumns == mat.numRows;

		DenseMatrix res = new DenseMatrix(this.numRows, mat.numColumns);
		for (int i = 0; i < res.numRows; i++) {
			for (int j = 0; j < res.numColumns; j++) {

				double product = 0;
				for (int k = 0; k < this.numColumns; k++)
					product += data[i][k] * mat.data[k][j];

				res.set(i, j, product);
			}
		}

		return res;
	}

	/**
	 * Matrix multiplication with a sparse matrix
	 * 
	 * @param mat
	 *            a sparse matrix
	 * @return a dense matrix with results of matrix multiplication
	 */
	public DenseMatrix mult(SparseMatrix mat) {
		assert this.numColumns == mat.numRows;

		DenseMatrix res = new DenseMatrix(this.numRows, mat.numColumns);

		for (int j = 0; j < res.numColumns; j++) {
			SparseVector col = mat.column(j); // only one-time computation

			for (int i = 0; i < res.numRows; i++) {

				double product = 0;
				for (VectorEntry ve : col)
					product += data[i][ve.index()] * ve.get();

				res.set(i, j, product);
			}
		}

		return res;
	}

	/**
	 * Do {@code matrix x vector} between current matrix and a given vector
	 * 
	 * @return a dense vector with the results of {@code matrix x vector}
	 */
	public DenseVector mult(DenseVector vec) {
		assert this.numColumns == vec.size;

		DenseVector res = new DenseVector(this.numRows);
		for (int i = 0; i < this.numRows; i++)
			res.set(i, row(i, false).inner(vec));

		return res;
	}

	public DenseVector mult(SparseVector vec) {
		DenseVector res = new DenseVector(this.numRows);
		for (int i = 0; i < this.numRows; i++) {

			double product = 0;
			for (VectorEntry ve : vec)
				product += data[i][ve.index()] * ve.get();

			res.set(i, product);
		}

		return res;
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
		assert sm.numColumns == dm.numRows;

		DenseMatrix res = new DenseMatrix(sm.numRows, dm.numColumns);

		for (int i = 0; i < res.numRows; i++) {
			SparseVector row = sm.row(i);
			for (int j = 0; j < res.numColumns; j++) {

				double product = 0;
				for (int k : row.getIndex())
					product += row.get(k) * dm.data[k][j];

				res.set(i, j, product);
			}
		}

		return res;

	}

	/**
	 * Get the value at entry [row, column]
	 */
	public double get(int row, int column) {
		return data[row][column];
	}

	/**
	 * Set a value to entry [row, column]
	 */
	public void set(int row, int column, double val) {
		data[row][column] = val;
	}

	/**
	 * Set a value to all entries
	 */
	public void setAll(double val) {
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numColumns; col++) {
				data[row][col] = val;
			}
		}
	}

	/**
	 * @return the sum of data entries in a row
	 */
	public double sumOfRow(int row) {
		double res = 0;
		for (int col = 0; col < numColumns; col++)
			res += data[row][col];

		return res;
	}

	/**
	 * @return the sum of data entries in a column
	 */
	public double sumOfColumn(int col) {
		double res = 0;
		for (int row = 0; row < numRows; row++)
			res += data[row][col];

		return res;
	}

	/**
	 * @return the sum of all data entries
	 */
	public double sum() {
		double res = 0;
		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numColumns; col++) {
				res += data[row][col];
			}
		}

		return res;
	}

	/**
	 * Add a value to entry [row, column]
	 */
	public void add(int row, int column, double val) {
		data[row][column] += val;
	}

	/**
	 * @return a new matrix by scaling the current matrix
	 */
	public DenseMatrix scale(double val) {
		DenseMatrix mat = new DenseMatrix(numRows, numColumns);
		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				mat.data[i][j] = this.data[i][j] * val;

		return mat;
	}

	/**
	 * Do {@code A + B} matrix operation
	 * 
	 * @return a matrix with results of {@code C = A + B}
	 */
	public DenseMatrix add(DenseMatrix mat) {
		assert numRows == mat.numRows;
		assert numColumns == mat.numColumns;

		DenseMatrix res = new DenseMatrix(numRows, numColumns);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				res.data[i][j] = data[i][j] + mat.data[i][j];

		return res;
	}

	/**
	 * Do {@code A + B} matrix operation
	 * 
	 * @return a matrix with results of {@code C = A + B}
	 */
	public DenseMatrix add(SparseMatrix mat) {
		assert numRows == mat.numRows;
		assert numColumns == mat.numColumns;

		DenseMatrix res = this.clone();

		for (MatrixEntry me : mat)
			res.add(me.row(), me.column(), me.get());

		return res;
	}

	/**
	 * Do {@code A + c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
	 * 
	 * @return a new matrix with results of {@code C = A + c}
	 */
	public DenseMatrix add(double val) {

		DenseMatrix res = new DenseMatrix(numRows, numColumns);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				res.data[i][j] = data[i][j] + val;

		return res;
	}

	/**
	 * Do {@code A + B} matrix operation
	 * 
	 * @return a matrix with results of {@code C = A + B}
	 */
	public DenseMatrix minus(DenseMatrix mat) {
		assert numRows == mat.numRows;
		assert numColumns == mat.numColumns;

		DenseMatrix res = new DenseMatrix(numRows, numColumns);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				res.data[i][j] = data[i][j] - mat.data[i][j];

		return res;
	}

	/**
	 * Do {@code A + B} matrix operation
	 * 
	 * @return a matrix with results of {@code C = A + B}
	 */
	public DenseMatrix minus(SparseMatrix mat) {
		assert numRows == mat.numRows;
		assert numColumns == mat.numColumns;

		DenseMatrix res = this.clone();

		for (MatrixEntry me : mat)
			res.add(me.row(), me.column(), -me.get());

		return res;
	}

	/**
	 * Do {@code A + c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
	 * 
	 * @return a new matrix with results of {@code C = A + c}
	 */
	public DenseMatrix minus(double val) {

		DenseMatrix res = new DenseMatrix(numRows, numColumns);

		for (int i = 0; i < numRows; i++)
			for (int j = 0; j < numColumns; j++)
				res.data[i][j] = data[i][j] - val;

		return res;
	}

	/**
	 * @return the Cholesky decomposition of the current matrix
	 */
	public DenseMatrix cholesky() {
		if (this.numRows != this.numColumns)
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

	/**
	 * @return a transposed matrix of current matrix
	 */
	public DenseMatrix transpose() {
		DenseMatrix mat = new DenseMatrix(numColumns, numRows);

		for (int i = 0; i < mat.numRows; i++)
			for (int j = 0; j < mat.numColumns; j++)
				mat.set(i, j, this.data[j][i]);

		return mat;
	}

	/**
	 * @return a covariance matrix of the current matrix
	 */
	public DenseMatrix cov() {
		DenseMatrix mat = new DenseMatrix(numColumns, numColumns);

		for (int i = 0; i < numColumns; i++) {
			DenseVector xi = this.column(i);
			xi = xi.minus(xi.mean());

			mat.set(i, i, xi.inner(xi) / (xi.size - 1));

			for (int j = i + 1; j < numColumns; j++) {
				DenseVector yi = this.column(j);
				double val = xi.inner(yi.minus(yi.mean())) / (xi.size - 1);

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
	 * @deprecated use {@code inv} instead which is slightly faster
	 */
	public DenseMatrix inverse() {
		if (numRows != numColumns)
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

	/**
	 * NOTE: this implementation (adopted from PREA package) is slightly faster than {@code inverse}, especially when
	 * {@code numRows} is large.
	 * 
	 * @return the inverse matrix of current matrix
	 */
	public DenseMatrix inv() {
		if (this.numRows != this.numColumns)
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
	 * @return Moore¨CPenrose pseudoinverse based on singular value decomposition (SVD)
	 */
	public DenseMatrix pinv() {

		if (numRows < numColumns) {
			DenseMatrix res = this.transpose().pinv();
			if (res != null)
				res = res.transpose();
			return res;
		}
		
		SVD svd = this.svd();
		DenseMatrix U = svd.getU(), S = svd.getS(), V = svd.getV();

		// compute S^+
		DenseMatrix SPlus = S.clone();
		for (int i = 0; i < SPlus.numRows; i++) {
			double val = SPlus.get(i, i);
			if (val != 0)
				SPlus.set(i, i, 1.0 / val);
		}

		return V.mult(SPlus).mult(U.transpose());
	}

	public SVD svd() {
		return new SVD(this);
	}

	/**
	 * set one value to a specific row
	 * 
	 * @param row
	 *            row id
	 * @param val
	 *            value to be set
	 */
	public void setRow(int row, double val) {
		Arrays.fill(data[row], val);
	}

	/**
	 * set values of one dense vector to a specific row
	 * 
	 * @param row
	 *            row id
	 * @param vals
	 *            values of a dense vector
	 */
	public void setRow(int row, DenseVector vals) {
		for (int j = 0; j < numColumns; j++)
			data[row][j] = vals.data[j];
	}

	/**
	 * clear and reset all entries to 0
	 */
	public void clear() {
		for (int i = 0; i < numRows; i++)
			setRow(i, 0.0);
	}

	@Override
	public String toString() {
		return Strings.toString(data);
	}

}
