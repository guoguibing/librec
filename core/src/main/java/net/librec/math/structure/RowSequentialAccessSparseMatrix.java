package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.Serializable;
import java.util.Iterator;

/**
 * row sequential sparse matrix
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class RowSequentialAccessSparseMatrix extends AbstractMatrix implements Serializable {
    private static final long serialVersionUID = 8024536511172609930L;

    private VectorBasedSequentialSparseVector[] rowVectors;
    private int numEntries;

    /**
     * for the column and viewColumn operate, Delay Operation
     */
    private SequentialAccessSparseMatrix tmpSparseMatrix = null;

    /**
     * Construct a sparse matrix starting with the provided row vectors.
     *
     * @param rows       The number of rows in the result
     * @param columns    The number of columns in the result
     * @param rowVectors a Vector[] array of rows
     */
    public RowSequentialAccessSparseMatrix(int rows, int columns, VectorBasedSequentialSparseVector[] rowVectors) {
        this(rows, columns, rowVectors, false);
    }

    public RowSequentialAccessSparseMatrix(int rows, int columns, VectorBasedSequentialSparseVector[] vectors,
                                           boolean shallowCopy) {
        super(rows, columns);
        rowVectors = new VectorBasedSequentialSparseVector[rows];
        numEntries = 0;
        for (int row = 0; row < rows; row++) {
            rowVectors[row] = shallowCopy ? vectors[row] : vectors[row].clone();
            numEntries += rowVectors[row].getNumEntries();
        }
    }

    public RowSequentialAccessSparseMatrix(int rows,
                                           int columns,
                                           DataFrame df,
                                           DataFrameIndex rowDataFrameIndex,
                                           int itemColumn,
                                           int valueColumn) {
        super(rows, columns);
        rowVectors = new VectorBasedSequentialSparseVector[rows];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            IntArrayList positions = rowDataFrameIndex.getIndices(rowIndex);
            IntArrayList indices = new IntArrayList(positions.size());
            DoubleArrayList doubleValues = new DoubleArrayList(positions.size());
            for (int position : positions) {
                indices.add((int) df.get(itemColumn,position));
                doubleValues.add((double) df.get(valueColumn,position));
            }
            rowVectors[rowIndex] = new VectorBasedSequentialSparseVector( columns, indices, doubleValues);
        }
    }

    /**
     * Construct a matrix of the given cardinality, with rows defaulting to RandomAccessSparseVector
     * implementation
     *
     * @param rows    Number of rows in result
     * @param columns Number of columns in result
     */
    private RowSequentialAccessSparseMatrix(int rows, int columns) {
        super(rows, columns);
        rowVectors = new VectorBasedSequentialSparseVector[rows];
        numEntries = 0;
    }

    public RowSequentialAccessSparseMatrix(SequentialAccessSparseMatrix matrix) {
        this(matrix.getRowMatrix());
    }

    public RowSequentialAccessSparseMatrix(RowSequentialAccessSparseMatrix matrix) {
        this(matrix.rowSize(), matrix.columnSize());
        numEntries = matrix.getNumEntries();
        for (int rowIndex = 0; rowIndex < matrix.rowSize(); rowIndex++) {
            rowVectors[rowIndex] = new VectorBasedSequentialSparseVector(matrix.row(rowIndex));
        }
    }

    public RowSequentialAccessSparseMatrix(SequentialAccessSparseMatrix matrix, boolean shallowIndicesCopy) {
        this(matrix.getRowMatrix(), shallowIndicesCopy);
    }

    public RowSequentialAccessSparseMatrix(RowSequentialAccessSparseMatrix matrix, boolean shallowIndicesCopy) {
        this(matrix.rowSize(), matrix.columnSize());
        numEntries = matrix.getNumEntries();
        for (int rowIndex = 0; rowIndex < matrix.rowSize(); rowIndex++) {
            rowVectors[rowIndex] = new VectorBasedSequentialSparseVector((VectorBasedSequentialSparseVector) matrix.row(rowIndex), shallowIndicesCopy);
        }
    }

    public RowSequentialAccessSparseMatrix(int rows,
                                           int columns,
                                           Table<Integer, Integer, ? extends Number> dataTable) {
        this(rows, columns);
        numEntries = dataTable.size();
        construct(dataTable, false);
    }

    public RowSequentialAccessSparseMatrix(int rows,
                                           int columns,
                                           Table<Integer, Integer, ? extends Number> dataTable,
                                           boolean transpose) {
        this(rows, columns);
        numEntries = dataTable.size();
        construct(dataTable, transpose);
    }


    private void construct(Table<Integer, Integer, ? extends Number> dataTable, boolean transpose) {
        if (transpose) {
            Table<Integer, Integer, Double> colDataTable = HashBasedTable.create();
            for (Table.Cell<Integer, Integer, ? extends Number> cell : dataTable.cellSet()) {
                colDataTable.put(cell.getColumnKey(), cell.getRowKey(), cell.getValue().doubleValue());
            }
            dataTable = colDataTable;
        }

        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            if (dataTable.containsRow(rowIndex)) {
                rowVectors[rowIndex] = new VectorBasedSequentialSparseVector(columnSize(), dataTable.row(rowIndex));
            } else {
                rowVectors[rowIndex] = new VectorBasedSequentialSparseVector(columnSize());
            }
        }
    }

    /**
     * remove zero entries
     */
    public void reshape() {
        numEntries = 0;
        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            rowVectors[rowIndex].reshape();
            numEntries += rowVectors[rowIndex].getNumEntries();
        }
    }

    @Override
    public RowSequentialAccessSparseMatrix clone() {
        return new RowSequentialAccessSparseMatrix(rowSize(), columnSize(), rowVectors, false);
    }

    /**
     * @return the mean of all entries in the sparse matrix
     */
    public double mean() {
        double meanValue = 0.0D;
        for (MatrixEntry matrixEntry : this) {
            meanValue += matrixEntry.get();
        }
        return meanValue / this.getNumEntries();
    }

    @Override
    public double get(int row, int column) {
        return row(row).get(column);
    }

    public double getAtColumnPosition(int row, int columnPosition) {
        return row(row).getAtPosition(columnPosition);
    }

    @Override
    public void set(int row, int column, double value) {
        row(row).set(column, value);
    }

    public void setAtColumnPosition(int row, int columnPosition, double value) {
        row(row).setAtPosition(columnPosition, value);
    }

    @Override
    public int getNumEntries() {
        return numEntries;
    }

    @Override
    public Iterator<MatrixEntry> iterator() {
        return new SparseMatrixIterator();
    }

    @Override
    public SequentialSparseVector row(int row) {
        return rowVectors[row];
    }

    @Override
    public SequentialSparseVector column(int column) {
        if (tmpSparseMatrix == null) {
            tmpSparseMatrix = new SequentialAccessSparseMatrix(this);
        }
        return tmpSparseMatrix.column(column);
    }

    /**
     * @param row an int row index
     * @return a shallow view of the Vector at specified row (ie you may mutate the original matrix
     * using this row)
     */
    @Override
    public SequentialSparseVector viewRow(int row) {
        return rowVectors[row].clone();
    }

    @Override
    public SequentialSparseVector viewColumn(int column) {
        if (tmpSparseMatrix == null) {
            tmpSparseMatrix = new SequentialAccessSparseMatrix(this);
        }
        return tmpSparseMatrix.viewColumn(column);
    }

    @Override
    public boolean isRandomAccess() {
        return false;
    }

    public final class SparseMatrixIterator implements Iterator<MatrixEntry> {
        private final RowSequentialSparseMatrixEntry matrixEntry = new RowSequentialSparseMatrixEntry();

        @Override
        public boolean hasNext() {
            return matrixEntry.getMatrixEntryPosition() < getNumEntries();
        }

        @Override
        public RowSequentialSparseMatrixEntry next() {
            matrixEntry.advanceOffset();
            return matrixEntry;
        }
    }

    public final class RowSequentialSparseMatrixEntry implements MatrixEntry {
        int row = 0;
        int columnPosition = -1;
        int matrixEntryPosition = 0;
        SequentialSparseVector tempVector = rowVectors[0];

        private void advanceOffset() {
            matrixEntryPosition++;
            if (columnPosition < tempVector.getNumEntries() - 1) {
                columnPosition++;
            } else {
                row++;
                columnPosition = 0;
                tempVector = rowVectors[row];

                while (tempVector.getNumEntries() == 0) {
                    row++;
                    tempVector = rowVectors[row];
                }

            }
        }

        private int getMatrixEntryPosition() {
            return matrixEntryPosition;
        }

        @Override
        public int row() {
            return row;
        }

        @Override
        public int column() {
            return tempVector.getIndexAtPosition(columnPosition);
        }

        @Override
        public double get() {
            return tempVector.getAtPosition(columnPosition);
        }

        public int columnPosition() {
            return columnPosition;
        }

        @Override
        public void set(double value) {
            tempVector.setAtPosition(columnPosition, value);
        }

        @Override
        public int rowPosition() {
            return row;
        }
    }
}
