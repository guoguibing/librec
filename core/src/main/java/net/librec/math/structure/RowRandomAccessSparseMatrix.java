package net.librec.math.structure;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.librec.common.IndexException;

import java.io.Serializable;
import java.util.Iterator;

public class RowRandomAccessSparseMatrix  extends AbstractMatrix implements Serializable {
    private static final long serialVersionUID = 8024536511172609931L;
    private Int2ObjectOpenHashMap<RandomAccessSparseVector> rowVectors;
    private int numEntries;


    public RowRandomAccessSparseMatrix(int rows, int columns) {
        super(rows, columns);
    }

    @Override
    public Vector row(int row) {
        if (row < 0 || row >= rowSize()) {
            throw new IndexException(row, rowSize());
        }
        RandomAccessSparseVector res = rowVectors.get(row);
        if (res == null) {
            res = new RandomAccessSparseVector(columnSize());
            rowVectors.put(row, res);
        }
        return res;
    }

    @Override
    @Deprecated
    public Vector column(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector viewRow(int row) {
        if (row < 0 || row >= rowSize()) {
            throw new IndexException(row, rowSize());
        }
        RandomAccessSparseVector res = rowVectors.get(row);
        if (res == null) {
            res = new RandomAccessSparseVector(columnSize());
            rowVectors.put(row, res);
        }
        return res.clone();
    }

    @Override
    @Deprecated
    public Vector viewColumn(int column) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double get(int row, int column) {
        RandomAccessSparseVector r = rowVectors.get(row);
        return r == null ? 0.0 : r.get(column);
    }

    @Override
    public void set(int row, int column, double value) {
        RandomAccessSparseVector r = rowVectors.get(row);
        if (r == null) {
            r = new RandomAccessSparseVector(columnSize());
            rowVectors.put(row, r);
        }
        r.set(column, value);
    }

    @Override
    public int getNumEntries() {
        return numEntries;
    }

    @Override
    public Iterator<MatrixEntry> iterator() {
        return null;
    }

    @Override
    public boolean isRandomAccess() {
        return true;
    }
}
