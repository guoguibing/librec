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

package net.librec.math.structure;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.algorithm.SVD;
import net.librec.util.StringUtil;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Data Structure: dense matrix <br>
 * <p>
 * A big reason that we do not adopt original DenseMatrix from M4J libraray is because the latter using one-dimensional
 * array to store data, which will often cause OutOfMemory exception due to the limit of maximum length of a
 * one-dimensional Java array.
 *
 * @author guoguibing
 */
public class DenseMatrix implements DataMatrix, Serializable {

    private static final long serialVersionUID = -2069621030647530185L;

    /** dimension */
    public int numRows, numColumns, topN;
    /** read data */
    public double[][] data;


    /**
     * Construct a dense matrix with specified dimensions
     *
     * @param numRows    number of rows
     * @param numColumns number of columns
     */
    public DenseMatrix(int numRows, int numColumns) {
        this.numRows = numRows;
        this.numColumns = numColumns;

        data = new double[numRows][numColumns];
    }

    /**
     * Construct a dense matrix with specified dimensions
     *
     * @param numRows    number of rows
     * @param numColumns number of columns
     * @param topN numnber of top N
     */
    public DenseMatrix(int numRows, int numColumns, int topN) {
        this.numRows = numRows;
        this.numColumns = numColumns;
        this.topN = topN;
        data = new double[numRows][numColumns];
    }

    /**
     * Construct a dense matrix by copying data from a given 2D array
     *
     * @param array data array
     */
    public DenseMatrix(double[][] array) {
        this(array.length, array[0].length);

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] = array[i][j];
    }

    /**
     * Construct a dense matrix by a shallow copy of a data array
     *
     * @param array       the data array
     * @param numColumns  number of columns
     * @param numRows     number of rows
     */
    public DenseMatrix(double[][] array, int numRows, int numColumns) {
        this.numRows = numRows;
        this.numColumns = numColumns;

        this.data = array;
    }

    /**
     * Construct a dense matrix by copying data from a given matrix
     *
     * @param mat input matrix
     */
    public DenseMatrix(DenseMatrix mat) {
        this(mat.data);
    }

    /**
     * Make a deep copy of current matrix
     *
     * @return a cloned dense matrix
     */
    public DenseMatrix clone() {
        return new DenseMatrix(this);
    }

    /**
     * Construct an identity matrix
     *
     * @param dim dimension
     * @return an identity matrix
     */
    public static DenseMatrix eye(int dim) {
        DenseMatrix mat = new DenseMatrix(dim, dim);
        for (int i = 0; i < mat.numRows; i++)
            mat.set(i, i, 1.0);

        return mat;
    }

    /**
     * Initialize a dense matrix with small Guassian values <br>
     * <p>
     * <strong>NOTE:</strong> small initial values make it easier to train a model; otherwise a very small learning rate
     * may be needed (especially when the number of factors is large) which can cause bad performance.
     *
     * @param mean  mean of the gaussian function
     * @param sigma sigma of the gaussian function
     */
    public void init(double mean, double sigma) {
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] = Randoms.gaussian(mean, sigma);
    }

    /**
     * Initialize a dense matrix with small random values in (0, range)
     *
     * @param range max of the range
     */
    public void init(double range) {

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] = Randoms.uniform(0, range);
    }

    /**
     * Initialize a dense matrix with small random values in (0, 1)
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
     * Return a copy of row data as a dense vector.
     *
     * @param rowId row id
     * @return a copy of row data as a dense vector
     */
    public DenseVector row(int rowId) {
        return row(rowId, true);
    }

    /**
     * Return a vector of a specific row.
     *
     * @param rowId row id
     * @param deep  whether to copy data or only shallow copy for executing speedup purpose
     * @return a vector of a specific row
     */
    public DenseVector row(int rowId, boolean deep) {
        return new DenseVector(data[rowId], deep);
    }

    /**
     * Return a sub matrix of this matrix.
     *
     * @param rowStart  the row index to start
     * @param rowEnd    the row index to end
     * @param colStart  the column index to start
     * @param colEnd    the column index to end
     * @return  a sub matrix of this matrix
     */
    public DenseMatrix getSubMatrix(int rowStart, int rowEnd, int colStart, int colEnd) {
        if (rowStart >= rowEnd || colStart >= colEnd) {
            return null;
        } else {
            int r = rowEnd - rowStart + 1;
            int c = colEnd - colStart + 1;
            double[][] d = new double[r][c];
            for (int i = rowStart; i <= rowEnd; i++) {
                for (int j = colStart; j <= colEnd; j++) {
                    double a = data[i][j];
                    d[i - rowStart][j - colStart] = a;
                }
            }
            return new DenseMatrix(d);
        }
    }


    /**
     * Return a copy of column data as a dense vector.
     *
     * @param column column id
     * @return a copy of column data as a dense vector
     */
    public DenseVector column(int column) {
        DenseVector vec = new DenseVector(numRows);

        for (int i = 0; i < numRows; i++)
            vec.set(i, data[i][column]);

        return vec;
    }

    /**
     * Compute mean of a column of the current matrix.
     *
     * @param column column id
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
     * Inner product of two row vectors
     *
     * @param m    the first matrix
     * @param mrow row of the first matrix
     * @param n    the second matrix
     * @param nrow row of the second matrix
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
     * Inner product of two column vectors
     *
     * @param m    the first matrix
     * @param mcol column of the first matrix
     * @param n    the second matrix
     * @param ncol column of the second matrix
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
     * Dot product of row x col between two matrices.
     *
     * @param m    the first matrix
     * @param mrow row id of the first matrix
     * @param n    the second matrix
     * @param ncol column id of the second matrix
     * @return dot product of row of the first matrix and column of the second matrix
     * @throws LibrecException if {@code m.numColumns != n.numRows}
     */
    public static double product(DenseMatrix m, int mrow, DenseMatrix n, int ncol) throws LibrecException {
        // assert m.numColumns == n.numRows;
        if ( m.numColumns != n.numRows) {
            throw new LibrecException("m.numColumns should equal to n.numRows");
        }
        double res = 0;
        for (int j = 0; j < m.numColumns; j++)
            res += m.get(mrow, j) * n.get(j, ncol);

        return res;
    }

    /**
     * Return Kronecker product of two arbitrary matrices
     *
     * @param M a dense matrix
     * @param N an other dense matrix
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
     * Return Khatri-Rao product of two matrices.
     *
     * @param M a dense matrix
     * @param N an other dense matrix
     * @return Khatri-Rao product of two matrices
     * @throws Exception if error occurs
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
     * Return Hadamard product of two matrices.
     *
     * @param M a dense matrix
     * @param N an other dense matrix
     * @return Hadamard product of two matrices
     * @throws Exception if The dimensions of two matrices are not consistent
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
     * @param mat a dense matrix
     * @return a dense matrix with results of matrix multiplication
     * @throws LibrecException if {@code this.numColumns != mat.numRows}
     */
    public DenseMatrix mult(DenseMatrix mat) throws LibrecException {
        // assert this.numColumns == mat.numRows;
        if (this.numColumns != mat.numRows) {
            throw new LibrecException("this.numColumns should equal to mat.numRows");
        }

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
     * @param mat a sparse matrix
     * @return a dense matrix with results of matrix multiplication
     * @throws LibrecException if {@code this.numColumns != mat.numRows}
     */
    public DenseMatrix mult(SparseMatrix mat) throws LibrecException {
        if(this.numColumns != mat.numRows){
            throw new LibrecException("numColumns should equal to numRows");
        }

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
     * @param vec a given vector
     * @return a dense vector with the results of {@code matrix x vector}
     * @throws LibrecException if {@code this.numColumns != vec.size}
     */
    public DenseVector mult(DenseVector vec) throws LibrecException {
        // assert this.numColumns == vec.size;
        if (this.numColumns != vec.size) {
            throw new LibrecException("this.numColumns should equal to vec.size");
        }
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
     * @param sm a sparse matrix
     * @param dm a dense matrix
     * @return a dense matrix with the results of matrix multiplication
     * @throws LibrecException if {@code sm.numColumns != dm.numRows}
     */
    public static DenseMatrix mult(SparseMatrix sm, DenseMatrix dm) throws LibrecException {
        //assert sm.numColumns == dm.numRows;
        if (sm.numColumns != dm.numRows) {
            throw new LibrecException("sm.numColumns should equal to dm.numRows");
        }

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
     * @param column column index
     * @param row    row index
     * @return value at entry [row, column]
     */
    public double get(int row, int column) {
        return data[row][column];
    }

    /**
     * Set a value to entry [row, column]
     *
     * @param row    row index
     * @param column column index
     * @param val    the value to be set
     */
    public void set(int row, int column, double val) {
        if (topN < 0) {

        } else {
            data[row][column] = val;
        }
    }

    /**
     * Set a value to all entries
     *
     * @param val the value to be set
     */
    public void setAll(double val) {
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {
                data[row][col] = val;
            }
        }
    }

    /**
     * Return the sum of data entries in a row
     *
     * @param row row index
     * @return the sum of data entries in a row
     */
    public double sumOfRow(int row) {
        double res = 0;
        for (int col = 0; col < numColumns; col++)
            res += data[row][col];

        return res;
    }

    /**
     * Return the sum of data entries in a column.
     *
     * @param col column index
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
     * Return a new matrix by scaling the current matrix.
     *
     * @param val a given value
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
     * Return this matrix by scaling the current matrix.
     *
     * @param val a given value for scaling
     * @return this matrix by scaling the current matrix
     */
    public DenseMatrix scaleEqual(double val) {
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] *= val;

        return this;
    }

    /**
     * Add a value to entry [row, column]
     *
     * @param val    the value to be added
     * @param row    row index
     * @param column column index
     */
    public void add(int row, int column, double val) {
        data[row][column] += val;
    }


    /**
     * Do {@code A + B} matrix operation
     *
     * @param mat another matrix
     * @return a new matrix with results of {@code C = A + B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix add(DenseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }
        DenseMatrix res = new DenseMatrix(numRows, numColumns);

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                res.data[i][j] = data[i][j] + mat.data[i][j];

        return res;
    }


    /**
     * Do {@code A + B} matrix operation
     *
     * @param mat another matrix
     * @return this matrix with results of {@code A = A + B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix addEqual(DenseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] += mat.data[i][j];

        return this;
    }

    /**
     * Do {@code A + B} matrix operation
     *
     * @param mat another matric
     * @return a matrix with results of {@code C = A + B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix add(SparseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        DenseMatrix res = this.clone();

        for (MatrixEntry me : mat)
            res.add(me.row(), me.column(), me.get());

        return res;
    }

    /**
     * Do {@code A + B} matrix operation
     *
     * @param mat another matrix
     * @return this matrix with results of {@code A = A + B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix addEqual(SparseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        for (MatrixEntry me : mat)
            data[me.row()][me.column()] += me.get();

        return this;
    }

    /**
     * Do {@code A + c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
     *
     * @param val  the value to be added
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
     * Do {@code A + c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
     *
     * @param val  the value to be added
     * @return this matrix with results of {@code A = A + c}
     */
    public DenseMatrix addEqual(double val) {
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] += val;

        return this;
    }

    /**
     * Do {@code A - B} matrix operation
     *
     * @param mat another matrix
     * @return a new matrix with results of {@code C = A - B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix minus(DenseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        DenseMatrix res = new DenseMatrix(numRows, numColumns);

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                res.data[i][j] = data[i][j] - mat.data[i][j];

        return res;
    }

    /**
     * Do {@code A - B} matrix operation
     *
     * @param mat another matrix
     * @return this matrix with results of {@code A = A - B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix minusEqual(DenseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] -= mat.data[i][j];

        return this;
    }

    /**
     * Do {@code A - B} matrix operation
     *
     * @param mat another matrix
     * @return a matrix with results of {@code C = A - B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix minus(SparseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        DenseMatrix res = this.clone();

        for (MatrixEntry me : mat)
            res.add(me.row(), me.column(), -me.get());

        return res;
    }

    /**
     * Do {@code A - B} matrix operation
     *
     * @param mat another matrix
     * @return this matrix with results of {@code C = A - B}
     * @throws LibrecException if {@code numRows != mat.numRows} or
     *                         {@code numColumns != mat.numColumns}
     */
    public DenseMatrix minusEqual(SparseMatrix mat) throws LibrecException {
        //assert numRows == mat.numRows;
        if (numRows != mat.numRows) {
            throw new LibrecException("numRows should be equal");
        }
        //assert numColumns == mat.numColumns;
        if (numColumns != mat.numColumns) {
            throw new LibrecException("numColumns should be equal");
        }

        for (MatrixEntry me : mat)
            data[me.row()][me.column()] -= me.get();

        return this;
    }

    /**
     * Do {@code A - c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
     *
     * @param val the value to for minus
     * @return a new matrix with results of {@code C = A - c}
     */
    public DenseMatrix minus(double val) {

        DenseMatrix res = new DenseMatrix(numRows, numColumns);

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                res.data[i][j] = data[i][j] - val;

        return res;
    }

    /**
     * Do {@code A - c} matrix operation, where {@code c} is a constant. Each entries will be added by {@code c}
     *
     * @param val the value to for minus
     * @return this matrix with results of {@code A = A - c}
     */
    public DenseMatrix minusEqual(double val) {
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numColumns; j++)
                data[i][j] -= val;

        return this;
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
     * @return Moore–Penrose pseudoinverse based on singular value decomposition (SVD)
     *
     * @throws LibrecException if error occurs during mult
     */
    public DenseMatrix pinv() throws LibrecException {

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
     * Set one value to a specific row.
     *
     * @param row row id
     * @param val value to be set
     */
    public void setRow(int row, double val) {
        Arrays.fill(data[row], val);
    }

    /**
     * Set values of one dense vector to a specific row.
     *
     * @param row  row id
     * @param vals values of a dense vector
     */
    public void setRow(int row, DenseVector vals) {
        for (int j = 0; j < numColumns; j++)
            data[row][j] = vals.data[j];
    }

    /**
     * Clear and reset all entries to 0.
     */
    public void clear() {
        for (int i = 0; i < numRows; i++)
            setRow(i, 0.0);
    }

    @Override
    public String toString() {
        return StringUtil.toString(data);
    }

    /**
     * @return the data
     */
    public double[][] getData() {
        return data;
    }

    @Override
    public int size() {
        return numRows * numColumns;
    }

}
