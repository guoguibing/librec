package net.librec.math.structure;

import net.librec.common.CardinalityException;
import net.librec.math.algorithm.Randoms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Matrix of doubles implemented using a 2-d array
 *
 * @author Keqiang Wang (email:sei.wkq2008@gmail.com)
 */
public class DenseMatrix extends AbstractMatrix implements Serializable {
    private static final long serialVersionUID = -2069621030647530185L;

    private double[][] values;

    /**
     * Construct a matrix from the given values
     *
     * @param values a double[][]
     */
    public DenseMatrix(double[][] values) {
        this(values, false);
    }

    /**
     * Construct a matrix from the given values
     *
     * @param values      a double[][]
     * @param shallowCopy directly use the supplied array?
     */
    public DenseMatrix(double[][] values, boolean shallowCopy) {
        super(values.length, values[0].length);
        if (shallowCopy) {
            this.values = values;
        } else {
            this.values = new double[values.length][];
            for (int i = 0; i < values.length; i++) {
                this.values[i] = new double[values[i].length];
                System.arraycopy(values[i], 0, this.values[i], 0, this.values[i].length);
            }
        }
    }

    /**
     * Constructs an empty matrix of the given cardinality.
     *
     * @param rows    The number of rows in the result.
     * @param columns The number of columns in the result.
     */
    public DenseMatrix(int rows, int columns) {
        super(rows, columns);
        this.values = new double[rows][columns];
    }

    public DenseMatrix(DenseMatrix denseMatrix) {
        this(denseMatrix.values);
    }

    public DenseMatrix assign(DenseMatrix matrix) {
        // make sure the data field has the correct length
        if (matrix.values[0].length != this.values[0].length || matrix.values.length != this.values.length) {
            this.values = new double[matrix.values.length][matrix.values[0].length];
        }
        // now copy the values
        for (int i = 0; i < this.values.length; i++) {
            System.arraycopy(matrix.values[i], 0, this.values[i], 0, this.values[0].length);
        }
        return this;
    }

    public DenseMatrix assign(MatrixAssigner mapper) {
        for (int row = 0; row < rowSize(); row++) {
            for (int column = 0; column < columnSize(); column++) {
                values[row][column] = mapper.getValue(row, column, values[row][column]);
            }
        }
        return this;
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
        this.assign((row, column, value) -> Randoms.gaussian(mean, sigma));
    }

    /**
     * Initialize a dense matrix with small random values in (0, range)
     *
     * @param range max of the range
     */
    public void init(double range) {
        this.assign((row, column, value) -> Randoms.uniform(0.0D, range));
    }

    /**
     * Initialize a dense matrix with small random values in (0, 1)
     */
    public void init() {
        init(1.0);
    }

    @Override
    public DenseMatrix clone() {
        return new DenseMatrix(this.values);
    }

    @Override
    public double get(int row, int column) {
        return values[row][column];
    }

    @Override
    public void set(int row, int column, double value) {
        values[row][column] = value;
    }

    @Override
    public int getNumEntries() {
        return rowSize() * columnSize();
    }

    @Override
    public DenseVector row(int row) {
        return new VectorBasedDenseVector(values[row], true);
    }

    @Override
    public DenseVector column(int column) {
        return new MatrixBasedDenseVector(this, column);
    }

    @Override
    public DenseVector viewRow(int row) {
        return new VectorBasedDenseVector(values[row]);
    }

    @Override
    public DenseVector viewColumn(int column) {
        double[] vectorValues = new double[rowSize()];
        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            vectorValues[rowIndex] = values[rowIndex][column];
        }
        return new VectorBasedDenseVector(vectorValues);
    }


    public void plus(int row, int column, double value) {
        values[row][column] += value;
    }

    /**
     * Return a new matrix containing the sum of each value of the recipient and the argument
     *
     * @param value a double
     * @return a new Matrix
     */
    public DenseMatrix plus(double value) {
        DenseMatrix denseMatrix = this.clone();
        denseMatrix.assign((row, column, matrixValue) -> matrixValue + value);
        return denseMatrix;
    }

    /**
     * Return a new matrix containing the element by element sum of the recipient and the argument
     *
     * @param otherMatrix a Matrix
     * @return a new Matrix
     */
    public DenseMatrix plus(Matrix otherMatrix) {
        DenseMatrix denseMatrix = this.clone();
        for (MatrixEntry matrixEntry : otherMatrix) {
            denseMatrix.plus(matrixEntry.row(), matrixEntry.column(), matrixEntry.get());
        }
        return denseMatrix;
    }

    public void set(int row, DenseVector denseVector) {
        for (int columnIndex = 0; columnIndex < denseVector.cardinality(); columnIndex++) {
            set(row, columnIndex, denseVector.get(columnIndex));
        }
    }

    /**
     * Return a new matrix containing the element by element difference of the recipient and the argument
     *
     * @param otherMatrix a Matrix
     * @return a new Matrix
     */
    public DenseMatrix minus(Matrix otherMatrix) {
        DenseMatrix denseMatrix = this.clone();
        for (MatrixEntry matrixEntry : otherMatrix) {
            denseMatrix.plus(matrixEntry.row(), matrixEntry.column(), -matrixEntry.get());
        }
        return denseMatrix;
    }

    /**
     * Return a new matrix containing the product of each value of the recipient and the argument
     *
     * @param value a double argument
     * @return a new Matrix
     */
    public DenseMatrix times(double value) {
        DenseMatrix denseMatrix = this.clone();
        denseMatrix.assign((row, column, matrixValue) -> matrixValue * value);
        return denseMatrix;
    }

    /**
     * Return a new matrix containing the product of the recipient and the argument
     *
     * @param otherMatrix a Matrix argument
     * @return a new Matrix
     * @throws CardinalityException if the cardinalities are incompatible
     */
    public DenseMatrix times(Matrix otherMatrix) {
        if (columnSize() != otherMatrix.rowSize()) {
            throw new CardinalityException(columnSize(), otherMatrix.rowSize());
        }
        int tempRows = this.rowSize();
        int tempColumns = otherMatrix.columnSize();
        DenseMatrix denseMatrix = new DenseMatrix(tempRows, tempColumns);
        List<Integer> columnList = new ArrayList<>(denseMatrix.columnSize());
        for (int columnIndex = 0; columnIndex < denseMatrix.columnSize(); columnIndex++) {
            columnList.add(columnIndex);
        }

        columnList.parallelStream().forEach(columnIndex->{
            Vector colVector = otherMatrix.viewColumn(columnIndex);
            for (int rowIndex = 0; rowIndex < denseMatrix.rowSize(); rowIndex++) {
                denseMatrix.set(rowIndex, columnIndex, row(rowIndex).dot(colVector));
            }
        });

        return denseMatrix;
    }


    /**
     * Do {@code matrix x vector} between current matrix and a given vector
     *
     * @param vector a given vector
     * @return a dense vector with the results of {@code matrix x vector}
     * @throws CardinalityException if {@code tcolumnSize() != vector.cardinality()}
     */
    public DenseVector times(Vector vector) {
        if (columnSize() != vector.cardinality()) {
            throw new CardinalityException(columnSize(), vector.cardinality());
        }

        DenseVector resultVector = new VectorBasedDenseVector(rowSize());

        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            resultVector.set(rowIndex, row(rowIndex).dot(vector));
        }

        return resultVector;
    }

    /**
     * @return a transposed matrix of current matrix
     */
    public DenseMatrix transpose() {
        DenseMatrix transposeMatrix = new DenseMatrix(columnSize(), rowSize());

        for (int transposeRowIndex = 0; transposeRowIndex < transposeMatrix.rowSize(); transposeRowIndex++) {
            for (int transposeColumnIndex = 0; transposeColumnIndex < transposeMatrix.columnSize(); transposeColumnIndex++) {
                transposeMatrix.set(transposeRowIndex, transposeColumnIndex, values[transposeColumnIndex][transposeRowIndex]);
            }
        }

        return transposeMatrix;
    }

    /**
     * @return the matrix norm-2
     */
    public double norm() {
        double result = 0.0D;

        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnSize(); columnIndex++) {
                result += values[rowIndex][columnIndex] * values[rowIndex][columnIndex];
            }
        }

        return Math.sqrt(result);
    }

    /**
     * @return a covariance matrix of the current matrix
     */
    public DenseMatrix covariance() {
        DenseMatrix resultMatrix = new DenseMatrix(columnSize(), columnSize());

        for (int resultRowIndex = 0; resultRowIndex < columnSize(); resultRowIndex++) {
            DenseVector columnVector = column(resultRowIndex);
            DenseVector columnDenseVector = columnVector.plus(-columnVector.mean());

            resultMatrix.set(resultRowIndex, resultRowIndex,
                    columnDenseVector.dot(columnDenseVector) / (columnDenseVector.cardinality() - 1));

            for (int resultColumnIndex = resultRowIndex + 1; resultColumnIndex < columnSize(); resultColumnIndex++) {
                DenseVector columnVector_2 = column(resultColumnIndex);
                double value = columnDenseVector.dot(columnVector_2.plus(-columnVector_2.mean()))
                        / (columnDenseVector.cardinality() - 1);

                resultMatrix.set(resultRowIndex, resultColumnIndex, value);
                resultMatrix.set(resultColumnIndex, resultRowIndex, value);
            }
        }

        return resultMatrix;
    }

    /**
     * @return the Cholesky decomposition of the current matrix
     */
    public DenseMatrix cholesky() {
        if (rowSize() != columnSize()) {
            throw new CardinalityException(rowSize(), columnSize());
        }

        int size = rowSize();
        DenseMatrix choleskyMatrix = new DenseMatrix(size, size);

        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            for (int rowIndex_2 = 0; rowIndex_2 <= rowIndex; rowIndex_2++) {
                double sum = 0.0D;
                for (int columnIndex = 0; columnIndex < rowIndex_2; columnIndex++)
                    sum += choleskyMatrix.get(rowIndex, columnIndex) * choleskyMatrix.get(rowIndex_2, columnIndex);

                double val = rowIndex == rowIndex_2 ? Math.sqrt(values[rowIndex][rowIndex] - sum)
                        : (values[rowIndex][rowIndex_2] - sum) / choleskyMatrix.get(rowIndex_2, rowIndex_2);
                choleskyMatrix.set(rowIndex, rowIndex_2, val);
            }
            if (Double.isNaN(choleskyMatrix.get(rowIndex, rowIndex)))
                return null;
        }

        return choleskyMatrix.transpose();
    }

    /**
     * NOTE: this implementation adopted from PREA package
     *
     * @return the inverse matrix of current matrix
     */
    public DenseMatrix inverse() {
        if (rowSize() != columnSize()) {
            throw new CardinalityException(rowSize(), columnSize());
        }

        int size = rowSize();
        DenseMatrix inverseMatrix = new DenseMatrix(size, size);
        for (int index = 0; index < size; index++) {
            inverseMatrix.set(index, index, 1.0D);
        }

        if (size == 1) {
            inverseMatrix.set(0, 0, 1.0D / this.get(0, 0));
            return inverseMatrix;
        }

        DenseMatrix copyMatrix = this.clone();
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            // find pivot:
            double mag = 0.0D;
            int pivot = -1;

            for (int columnIndex = rowIndex; columnIndex < size; columnIndex++) {
                double mag2 = Math.abs(copyMatrix.get(columnIndex, rowIndex));
                if (mag2 > mag) {
                    mag = mag2;
                    pivot = columnIndex;
                }
            }

            // no pivot (error):
            if (pivot == -1 || mag == 0)
                return inverseMatrix;

            // move pivot row into position:
            if (pivot != rowIndex) {
                double temp;
                for (int columnIndex = rowIndex; columnIndex < size; columnIndex++) {
                    temp = copyMatrix.get(rowIndex, columnIndex);
                    copyMatrix.set(rowIndex, columnIndex, copyMatrix.get(pivot, columnIndex));
                    copyMatrix.set(pivot, columnIndex, temp);
                }

                for (int columnIndex = 0; columnIndex < size; columnIndex++) {
                    temp = inverseMatrix.get(rowIndex, columnIndex);
                    inverseMatrix.set(rowIndex, columnIndex, inverseMatrix.get(pivot, columnIndex));
                    inverseMatrix.set(pivot, columnIndex, temp);
                }
            }

            // normalize pivot row:
            mag = copyMatrix.get(rowIndex, rowIndex);
            for (int columnIndex = rowIndex; columnIndex < size; columnIndex++)
                copyMatrix.set(rowIndex, columnIndex, copyMatrix.get(rowIndex, columnIndex) / mag);

            for (int columnIndex = 0; columnIndex < size; columnIndex++)
                inverseMatrix.set(rowIndex, columnIndex, inverseMatrix.get(rowIndex, columnIndex) / mag);

            // eliminate pivot row component from other rows:
            for (int rowIndex_2 = 0; rowIndex_2 < size; rowIndex_2++) {
                if (rowIndex == rowIndex_2)
                    continue;

                double mag2 = copyMatrix.get(rowIndex_2, rowIndex);

                for (int columnIndex = rowIndex; columnIndex < size; columnIndex++)
                    copyMatrix.set(rowIndex_2, columnIndex, copyMatrix.get(rowIndex_2, columnIndex)
                            - mag2 * copyMatrix.get(rowIndex, columnIndex));

                for (int columnIndex = 0; columnIndex < size; columnIndex++)
                    inverseMatrix.set(rowIndex_2, columnIndex, inverseMatrix.get(rowIndex_2, columnIndex)
                            - mag2 * inverseMatrix.get(rowIndex, columnIndex));
            }
        }

        return inverseMatrix;
    }

    @Override
    public boolean isRandomAccess() {
        return true;
    }

    @Override
    public Iterator<MatrixEntry> iterator() {
        return new DenseMatrixIterator();
    }

    public double[][] getValues() {
        return values;
    }

    private final class DenseMatrixIterator implements Iterator<MatrixEntry> {
        private final DenseMatrixEntry matrixEntry = new DenseMatrixEntry();
        private final int maxColumn = columnSize() - 1;
        private final int maxRow = rowSize() - 1;

        @Override
        public boolean hasNext() {
            return matrixEntry.row() < maxRow || matrixEntry.column() < maxColumn;
        }

        @Override
        public MatrixEntry next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (matrixEntry.column < maxColumn) {
                matrixEntry.column++;
            } else {
                matrixEntry.row++;
                matrixEntry.column = 0;
            }
            return matrixEntry;
        }
    }

    private final class DenseMatrixEntry implements MatrixEntry {
        int row = 0;
        int column = -1;

        @Override
        public int row() {
            return row;
        }

        @Override
        public int column() {
            return column;
        }

        @Override
        public double get() {
            return DenseMatrix.this.get(row, column);
        }

        @Override
        public void set(double value) {
            DenseMatrix.this.set(row, column, value);
        }

        @Override
        public int rowPosition() {
            return row;
        }

        @Override
        public int columnPosition() {
            return column;
        }
    }
}
