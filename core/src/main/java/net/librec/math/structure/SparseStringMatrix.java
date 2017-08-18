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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import java.util.*;

/**
 * Data Structure: Sparse Matrix whose implementation is modified from M4J library.
 * <ul>
 * <li><a href="http://netlib.org/linalg/html_templates/node91.html">Compressed Row Storage (CRS)</a></li>
 * <li><a href="http://netlib.org/linalg/html_templates/node92.html">Compressed Col Storage (CCS)</a></li>
 * </ul>
 *
 * @author guoguibing
 */
public class SparseStringMatrix {

    private static final long serialVersionUID = 8024536511172609539L;

    // matrix dimension
    protected int numRows, numColumns;
    protected Table<Integer, Integer, ? extends String> dataTable;
    // Compressed Row Storage (CRS)
    protected String[] rowData;
    protected int[] rowPtr, colInd;

    // Compressed Col Storage (CCS)
    protected String[] colData;
    protected int[] colPtr, rowInd;
    protected Multimap<Integer, Integer> colMap;

    /**
     * Construct a sparse matrix with both CRS and CCS structures
     *
     * @param rows  number of rows
     * @param cols  number of columns
     * @param dT    data table
     * @param cM    column map
     */
    public SparseStringMatrix(int rows, int cols, Table<Integer, Integer, ? extends String> dT,
                              Multimap<Integer, Integer> cM) {
        numRows = rows;
        numColumns = cols;
        colMap = cM;
        dataTable = dT;
        construct(dataTable, colMap);
    }

    /**
     *  Construct a sparse matrix with only CRS structures
     *
     * @param rows  number of rows
     * @param cols  number of columns
     * @param dataTable    data table
     */
    public SparseStringMatrix(int rows, int cols, Table<Integer, Integer, ? extends String> dataTable) {
        this(rows, cols, dataTable, null);
    }

    /**
     * Define a sparse matrix without data, only use for {@code transpose} method
     *
     * @param rows  number of rows
     * @param cols  number of columns
     */
    private SparseStringMatrix(int rows, int cols) {
        numRows = rows;
        numColumns = cols;
    }

    /**
     * Construct a sparse matrix from another sparse matrix
     *
     * @param mat the original sparse matrix
     */
    public SparseStringMatrix(SparseStringMatrix mat) {
        numRows = mat.numRows;
        numColumns = mat.numColumns;

        copyCRS(mat.rowData, mat.rowPtr, mat.colInd);

        copyCCS(mat.colData, mat.colPtr, mat.rowInd);
    }

    private void copyCRS(String[] data, int[] ptr, int[] idx) {
        rowData = new String[data.length];
        for (int i = 0; i < rowData.length; i++)
            rowData[i] = data[i];

        rowPtr = new int[ptr.length];
        for (int i = 0; i < rowPtr.length; i++)
            rowPtr[i] = ptr[i];

        colInd = new int[idx.length];
        for (int i = 0; i < colInd.length; i++)
            colInd[i] = idx[i];
    }

    private void copyCCS(String[] data, int[] ptr, int[] idx) {

        colData = new String[data.length];
        for (int i = 0; i < colData.length; i++)
            colData[i] = data[i];

        colPtr = new int[ptr.length];
        for (int i = 0; i < colPtr.length; i++)
            colPtr[i] = ptr[i];

        rowInd = new int[idx.length];
        for (int i = 0; i < rowInd.length; i++)
            rowInd[i] = idx[i];
    }

    /**
     * Make a deep clone of current matrix
     */
    public SparseStringMatrix clone() {
        return new SparseStringMatrix(this);
    }

    /**
     * @return the transpose of current matrix
     */
    public SparseStringMatrix transpose() {
        SparseStringMatrix tr = new SparseStringMatrix(numColumns, numRows);

        tr.copyCRS(this.rowData, this.rowPtr, this.colInd);
        tr.copyCCS(this.colData, this.colPtr, this.rowInd);

        return tr;
    }

    /**
     * @return the row pointers of CRS structure
     */
    public int[] getRowPointers() {
        return rowPtr;
    }

    /**
     * @return the column indices of CCS structure
     */
    public int[] getColumnIndices() {
        return colInd;
    }

    /**
     * Construct a sparse matrix
     *
     * @param dataTable       data table
     * @param columnStructure column structure
     */
    private void construct(Table<Integer, Integer, ? extends String> dataTable,
                           Multimap<Integer, Integer> columnStructure) {
        int nnz = dataTable.size();

        // CRS
        rowPtr = new int[numRows + 1];
        colInd = new int[nnz];
        rowData = new String[nnz];

        int j = 0;
        for (int i = 1; i <= numRows; ++i) {
            Set<Integer> cols = dataTable.row(i - 1).keySet();
            rowPtr[i] = rowPtr[i - 1] + cols.size();

            for (int col : cols) {
                colInd[j++] = col;
                if (col < 0 || col >= numColumns)
                    throw new IllegalArgumentException("colInd[" + j + "]=" + col
                            + ", which is not a valid column index");
            }

            Arrays.sort(colInd, rowPtr[i - 1], rowPtr[i]);
        }

        // CCS
        colPtr = new int[numColumns + 1];
        rowInd = new int[nnz];
        colData = new String[nnz];

        j = 0;
        for (int i = 1; i <= numColumns; ++i) {
            // dataTable.col(i-1) is more time-consuming than columnStructure.get(i-1)
            Collection<Integer> rows = columnStructure != null ? columnStructure.get(i - 1) : dataTable.column(i - 1)
                    .keySet();
            colPtr[i] = colPtr[i - 1] + rows.size();

            for (int row : rows) {
                rowInd[j++] = row;
                if (row < 0 || row >= numRows)
                    throw new IllegalArgumentException("rowInd[" + j + "]=" + row + ", which is not a valid row index");
            }

            Arrays.sort(rowInd, colPtr[i - 1], colPtr[i]);
        }

        // set data
        for (Cell<Integer, Integer, ? extends String> en : dataTable.cellSet()) {
            int row = en.getRowKey();
            int col = en.getColumnKey();
            String val = en.getValue().toString();

            set(row, col, val);
        }
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
     * @return referce to the data of current matrix
     */
    public String[] getData() {
        return rowData;
    }

    /**
     * Set a value to entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @param val    value to set
     */
    public void set(int row, int column, String val) {
        int index = getCRSIndex(row, column);
        rowData[index] = val;

        index = getCCSIndex(row, column);
        colData[index] = val;
    }

    /**
     * Add a value to entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @param val    value to add
     */
    public void add(int row, int column, double val) {
        int index = getCRSIndex(row, column);
        rowData[index] += val;

        index = getCCSIndex(row, column);
        colData[index] += val;
    }

    /**
     * Retrieve value at entry [row, column]
     *
     * @param row    row id
     * @param column column id
     * @return value at entry [row, column]
     */
    public String get(int row, int column) {

        int index = Arrays.binarySearch(colInd, rowPtr[row], rowPtr[row + 1], column);

        if (index >= 0)
            return rowData[index];
        else
            return "0";
    }


    /**
     * create a row cache of a matrix in {row, row-specific columns}
     *
     * @param cacheSpec cache specification
     * @return a matrix row cache in {row, row-specific columns}
     */
    public LoadingCache<Integer, List<Integer>> columnRowsCache(String cacheSpec) {
        LoadingCache<Integer, List<Integer>> cache = CacheBuilder.from(cacheSpec).build(
                new CacheLoader<Integer, List<Integer>>() {

                    @Override
                    public List<Integer> load(Integer colId) throws Exception {
                        return getRows(colId);
                    }
                });

        return cache;
    }

    /**
     * get a row sparse vector of a matrix
     *
     * @param row
     *            row id
     * @param except
     *            row id to be excluded
     * @return a sparse vector of {index, value}
     *

    public SparseVector row(int row, int except) {

    SparseVector sv = new SparseVector(numColumns);

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
     */
    /**
     * query the size of a specific row
     *
     * @param row row id
     * @return the size of non-zero elements of a row
     */
    public int rowSize(int row) {

        int size = 0;
        for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
            int col = colInd[j];
            if (get(row, col) != "0")
                size++;
        }

        return size;
    }

    /**
     * @return a list of rows which have at least one non-empty entry
     */
    public List<Integer> rows() {
        List<Integer> list = new ArrayList<Integer>();

        for (int row = 0; row < numRows; row++) {
            for (int j = rowPtr[row]; j < rowPtr[row + 1]; j++) {
                int col = colInd[j];
                if (get(row, col) != "0") {
                    list.add(row);
                    break;
                }
            }
        }

        return list;
    }

    /**
     * query the size of a specific col
     *
     * @param col col id
     * @return the size of non-zero elements of a row
     */
    public int columnSize(int col) {

        int size = 0;

        for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
            int row = rowInd[j];
            String val = get(row, col);
            if (val != "0")
                size++;
        }

        return size;
    }

    /**
     * get rows of a specific column where (row, column) entries are non-zero
     *
     * @param col column id
     * @return a list of column index
     */
    public List<Integer> getRows(int col) {

        List<Integer> res = new ArrayList<Integer>();

        if (col < numColumns) {
            for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
                int row = rowInd[j];
                String val = get(row, col);
                if (val != "0")
                    res.add(row);
            }
        }

        return res;
    }

    /**
     * @return a list of columns which have at least one non-empty entry
     */
    public List<Integer> columns() {
        List<Integer> list = new ArrayList<Integer>();

        for (int col = 0; col < numColumns; col++) {
            for (int j = colPtr[col]; j < colPtr[col + 1]; j++) {
                int row = rowInd[j];
                String val = get(row, col);
                if (val != "0") {
                    list.add(col);
                    break;
                }
            }
        }

        return list;
    }


    /**
     * @return a matrix format string
     */
    public String matString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dimension: ").append(numRows).append(" x ").append(numColumns).append("\n");

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                sb.append(get(i, j));
                if (j < numColumns - 1)
                    sb.append("\t");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Finds the insertion index of CRS
     *
     * @param row  row index
     * @param col  col index
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
     *
     * @param row  row index
     * @param col  col index
     */
    private int getCCSIndex(int row, int col) {
        int i = Arrays.binarySearch(rowInd, colPtr[col], colPtr[col + 1], row);

        if (i >= 0 && rowInd[i] == row)
            return i;
        else
            throw new IndexOutOfBoundsException("Entry (" + (row + 1) + ", " + (col + 1)
                    + ") is not in the matrix structure");
    }
}
