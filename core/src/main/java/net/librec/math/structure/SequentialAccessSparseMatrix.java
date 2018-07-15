package net.librec.math.structure;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.librec.util.ArrayUtils;

import java.io.Serializable;
import java.util.*;

//TODO map the row matrix index and column matrix index 2018-03-08

/**
 * Compressed Row Storage (CRS) and Compressed Col Storage (CCS)
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class SequentialAccessSparseMatrix extends AbstractMatrix implements Serializable {
    private static final long serialVersionUID = 8024536511172609539L;

    private RowSequentialAccessSparseMatrix rowMatrix;
    private int[][] rowToColumnPositionMap, columnToRowPositionMap;

    private int[][] columnIndices;

    private SequentialAccessSparseMatrix(int rows, int columns) {
        super(rows, columns);
    }

    public SequentialAccessSparseMatrix(int rows,
                                        int columns,
                                        Table<Integer, Integer, ? extends Number> dataTable) {
        this(rows, columns);
        rowMatrix = new RowSequentialAccessSparseMatrix(rows, columns, dataTable, false);
        constructColumnIndices(rowMatrix);
        constructMap();
    }

    public SequentialAccessSparseMatrix(RowSequentialAccessSparseMatrix rowMatrix) {
        this(rowMatrix.rowSize(), rowMatrix.columnSize());
        this.rowMatrix = rowMatrix.clone();
        constructColumnIndices(rowMatrix);
        constructMap();
    }

    public SequentialAccessSparseMatrix(SequentialAccessSparseMatrix otherMatrix) {
        this(otherMatrix.rowSize(), otherMatrix.columnSize());
        this.rowMatrix = otherMatrix.getRowMatrix().clone();
        columnIndices = ArrayUtils.copy(otherMatrix.columnIndices);
        rowToColumnPositionMap = ArrayUtils.copy(otherMatrix.rowToColumnPositionMap);
        columnToRowPositionMap = ArrayUtils.copy(otherMatrix.columnToRowPositionMap);
    }


    public SequentialAccessSparseMatrix(int rows,
                                        int columns,
                                        DataFrame df,
                                        DataFrameIndex rowDataFrameIndex,
                                        int itemColumn,
                                        int valueColumn) {
        this(rows, columns);
        rowMatrix = new RowSequentialAccessSparseMatrix(
                rows,
                columns,
                df,
                rowDataFrameIndex,
                itemColumn,
                valueColumn);
        constructColumnIndices(rowMatrix);
        constructMap();
    }

    public SequentialAccessSparseMatrix(RowSequentialAccessSparseMatrix rowMatrix, boolean shallowIndicesCopy) {
        this(rowMatrix.rowSize(), rowMatrix.columnSize());
        this.rowMatrix = new RowSequentialAccessSparseMatrix(rowMatrix, shallowIndicesCopy);
        constructColumnIndices(rowMatrix);
        constructMap();
    }

    public SequentialAccessSparseMatrix(SequentialAccessSparseMatrix otherMatrix, boolean shallowIndicesCopy) {
        this(otherMatrix.rowSize(), otherMatrix.columnSize());
        this.rowMatrix = new RowSequentialAccessSparseMatrix(otherMatrix, shallowIndicesCopy);
        columnIndices = shallowIndicesCopy ? otherMatrix.columnIndices : ArrayUtils.copy(otherMatrix.columnIndices);
        rowToColumnPositionMap = shallowIndicesCopy ? otherMatrix.rowToColumnPositionMap : ArrayUtils.copy(otherMatrix.rowToColumnPositionMap);
        columnToRowPositionMap = shallowIndicesCopy ? otherMatrix.columnToRowPositionMap : ArrayUtils.copy(otherMatrix.columnToRowPositionMap);
    }


    private void constructColumnIndices(RowSequentialAccessSparseMatrix rowMatrix) {
        IntArrayList[] columnIndicesList = new IntArrayList[rowMatrix.columnSize()];
        for (int columnIndex = 0; columnIndex < rowMatrix.columnSize(); columnIndex++) {
            columnIndicesList[columnIndex] = new IntArrayList();
        }

        for (MatrixEntry matrixEntry : rowMatrix) {
            int rowIndex = matrixEntry.row();
            int columnIndex = matrixEntry.column();
            columnIndicesList[columnIndex].add(rowIndex);
        }

        columnIndices = new int[rowMatrix.columnSize()][];
        for (int columnIndex = 0; columnIndex < rowMatrix.columnSize(); columnIndex++) {
            int size = columnIndicesList[columnIndex].size();
            columnIndices[columnIndex] = new int[size];
            Collections.sort(columnIndicesList[columnIndex]);
            for (int position = 0; position < size; position++) {
                columnIndices[columnIndex][position] = columnIndicesList[columnIndex].getInt(position);
            }
        }
    }

    /**
     * construct the index map between row matrix and column matrix
     */
    private void constructMap() {
        Int2ObjectOpenHashMap<Int2IntOpenHashMap> rowPositionMap = new Int2ObjectOpenHashMap<>();
        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            SequentialSparseVector tempRowVector = row(rowIndex);
            Int2IntOpenHashMap tempPositionMap = new Int2IntOpenHashMap();
            for (Vector.VectorEntry vectorEntry : tempRowVector) {
                tempPositionMap.put(vectorEntry.index(), vectorEntry.position());
            }
            rowPositionMap.put(rowIndex, tempPositionMap);
        }

        columnToRowPositionMap = new int[columnSize()][];
        for (int columnIndex = 0; columnIndex < columnSize(); columnIndex++) {
            SequentialSparseVector tempRowVector = column(columnIndex);
            columnToRowPositionMap[columnIndex] = new int[tempRowVector.getNumEntries()];
            for (Vector.VectorEntry vectorEntry : tempRowVector) {
                columnToRowPositionMap[columnIndex][vectorEntry.position()]
                        = rowPositionMap.get(vectorEntry.index()).get(columnIndex);
            }
        }

        Int2ObjectOpenHashMap<Int2IntOpenHashMap> columnPositionMap = new Int2ObjectOpenHashMap<>();
        for (int columnIndex = 0; columnIndex < columnSize(); columnIndex++) {
            SequentialSparseVector tempColumnVector = column(columnIndex);
            Int2IntOpenHashMap tempPositionMap = new Int2IntOpenHashMap();
            for (Vector.VectorEntry vectorEntry : tempColumnVector) {
                tempPositionMap.put(vectorEntry.index(), vectorEntry.position());
            }
            columnPositionMap.put(columnIndex, tempPositionMap);
        }

        rowToColumnPositionMap = new int[rowSize()][];
        for (int rowIndex = 0; rowIndex < rowSize(); rowIndex++) {
            SequentialSparseVector tempRowVector = row(rowIndex);
            rowToColumnPositionMap[rowIndex] = new int[tempRowVector.getNumEntries()];
            for (Vector.VectorEntry vectorEntry : tempRowVector) {
                rowToColumnPositionMap[rowIndex][vectorEntry.position()]
                        = columnPositionMap.get(vectorEntry.index()).get(rowIndex);
            }
        }

    }

    /**
     * @return the mean of all entries in the sparse matrix
     */
    public double mean() {
        return rowMatrix.mean();
    }

    @Override
    public SequentialSparseVector row(int row) {
        return rowMatrix.row(row);
    }

    @Override
    public SequentialSparseVector column(int column) {
        return new MatrixBasedSequentialSparseVector(this, column);
    }

    @Override
    public SequentialSparseVector viewRow(int row) {
        return rowMatrix.viewRow(row);
    }

    @Override
    public SequentialSparseVector viewColumn(int column) {
        int columnLength = columnIndices[column].length;
        int[] indices = new int[columnLength];
        double[] values = new double[columnLength];
        for (int position = 0; position < columnLength; position++) {
            indices[position] = columnIndices[column][position];
            int row = columnIndices[column][position];
            int columnPosition = columnToRowPositionMap[column][position];
            values[position] = getAtColumnPosition(row, columnPosition);
        }

        return new VectorBasedSequentialSparseVector(rowSize(), indices, values);
    }

    /**
     * remove zero entries
     */
    public void reshape() {
        rowMatrix.reshape();
        constructColumnIndices(rowMatrix);
        constructMap();
    }

    protected int[][] columnBasedRowIndices() {
        return columnIndices;
    }

    @Override
    public SequentialAccessSparseMatrix clone() {
        SequentialAccessSparseMatrix copyMatrix = new SequentialAccessSparseMatrix(rowSize(), columnSize());
        copyMatrix.rowMatrix = rowMatrix.clone();
        copyMatrix.columnIndices = ArrayUtils.copy(columnIndices);
        copyMatrix.rowToColumnPositionMap = ArrayUtils.copy(rowToColumnPositionMap);
        copyMatrix.columnToRowPositionMap = ArrayUtils.copy(columnToRowPositionMap);
        return copyMatrix;
    }

    public RowSequentialAccessSparseMatrix getRowMatrix() {
        return rowMatrix;
    }

    @Override
    public double get(int row, int column) {
        return rowMatrix.get(row, column);
    }

    @Override
    public void set(int row, int column, double value) {
        rowMatrix.set(row, column, value);
    }

    public void setAtColumnPosition(int row, int columnPosition, double value) {
        rowMatrix.setAtColumnPosition(row, columnPosition, value);
    }

    public double getAtColumnPosition(int row, int columnPosition) {
        return rowMatrix.getAtColumnPosition(row, columnPosition);
    }

    public void setAtRowPosition(int rowPosition, int column, double value) {
        int columnPosition = columnToRowPositionMap[column][rowPosition];
        int row = columnIndices[column][rowPosition];
        rowMatrix.setAtColumnPosition(row, columnPosition, value);
    }

    public double getAtRowPosition(int rowPosition, int column) {
        int row = columnIndices[column][rowPosition];
        int columnPosition = columnToRowPositionMap[column][rowPosition];
        return rowMatrix.getAtColumnPosition(row, columnPosition);
    }

    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    @Deprecated
    public LoadingCache<Integer, List<Integer>> rowColumnsCache(String cacheSpec) {
        LoadingCache<Integer, List<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, List<Integer>>() {

                    @Override
                    public List<Integer> load(Integer rowId) throws Exception {
                        int[] itemIndexes = row(rowId).getIndices();
                        Integer[] inputBoxed = org.apache.commons.lang.ArrayUtils.toObject(itemIndexes);
                        List<Integer> itemList = Arrays.asList(inputBoxed);
                        return itemList;
                    }
                });

        return cache;
    }

    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    @Deprecated
    public LoadingCache<Integer, Set<Integer>> rowColumnsSetCache(String cacheSpec) {
        LoadingCache<Integer, Set<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, Set<Integer>>() {

                    @Override
                    public Set<Integer> load(Integer rowId) throws Exception {
                        int[] itemIndexes = row(rowId).getIndices();
                        Integer[] inputBoxed = org.apache.commons.lang.ArrayUtils.toObject(itemIndexes);
                        List<Integer> itemList = Arrays.asList(inputBoxed);
                        return new HashSet(itemList);
                    }
                });

        return cache;
    }

    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    @Deprecated
    public LoadingCache<Integer, List<Integer>> columnRowsCache(String cacheSpec) {
        LoadingCache<Integer, List<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, List<Integer>>() {

                    @Override
                    public List<Integer> load(Integer colId) throws Exception {
                        int[] userIndexes = column(colId).getIndices();
                        Integer[] inputBoxed = org.apache.commons.lang.ArrayUtils.toObject(userIndexes);
                        List<Integer> userList = Arrays.asList(inputBoxed);
                        return userList;
                    }
                });

        return cache;
    }

    /**
     * @return the data table of this matrix as (row, column, value) cells
     */
    @Deprecated
    public Table<Integer, Integer, Double> getDataTable() {
        Table<Integer, Integer, Double> res = HashBasedTable.create();

        for (MatrixEntry me : this) {
            res.put(me.row(), me.column(), me.get());
        }

        return res;
    }

    @Override
    public boolean isRandomAccess() {
        return false;
    }

    @Override
    public int getNumEntries() {
        return rowMatrix.getNumEntries();
    }

    @Override
    public Iterator<MatrixEntry> iterator() {
        return new SparseMatrixIterator();
    }


    public final class SparseMatrixIterator implements Iterator<MatrixEntry> {
        private final SequentialSparseMatrixEntry matrixEntry = new SequentialSparseMatrixEntry();

        @Override
        public boolean hasNext() {
            return matrixEntry.getMatrixEntryPosition() < getNumEntries();
        }

        @Override
        public SequentialSparseMatrixEntry next() {
            matrixEntry.advanceOffset();
            return matrixEntry;
        }
    }

    public final class SequentialSparseMatrixEntry implements MatrixEntry {
        int row = 0;
        int columnPosition = -1;
        int matrixEntryPosition = 0;
        SequentialSparseVector tempVector = rowMatrix.row(0);

        private void advanceOffset() {
            matrixEntryPosition++;
            if (columnPosition < tempVector.getNumEntries() - 1) {
                columnPosition++;
            } else {
                row++;
                columnPosition = 0;
                tempVector = rowMatrix.row(row);
                while (tempVector.getNumEntries() == 0) {
                    row++;
                    tempVector = rowMatrix.row(row);
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
        @Deprecated
        public void set(double value) {
            tempVector.setAtPosition(columnPosition, value);
        }

        @Override
        public int rowPosition() {
            return rowToColumnPositionMap[row][columnPosition];
        }
    }
}
