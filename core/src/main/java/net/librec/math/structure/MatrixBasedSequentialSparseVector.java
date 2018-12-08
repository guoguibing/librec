package net.librec.math.structure;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class MatrixBasedSequentialSparseVector extends SequentialSparseVector {

    private SequentialAccessSparseMatrix sparseMatrix;
    private int column;
    private int length;
    private int[] indices;

    private MatrixBasedSequentialSparseVector(int size) {
        super(size);
    }

    public MatrixBasedSequentialSparseVector(SequentialAccessSparseMatrix sparseMatrix, int column) {
        super(sparseMatrix.rowSize());
        this.sparseMatrix = sparseMatrix;
        this.column = column;
        indices = sparseMatrix.columnBasedRowIndices()[column];
        length = indices.length;
    }

    @Override
    public void setAtPosition(int position, double value) {
        sparseMatrix.setAtRowPosition(position, column, value);
    }

    @Override
    public double getAtPosition(int position) {
        return sparseMatrix.getAtRowPosition(position, column);
    }

    @Override
    public int getIndexAtPosition(int position) {
        return getIndices()[position];
    }

    @Override
    protected void reshape() {
        sparseMatrix.reshape();
    }

    @Override
    public int[] getIndices() {
        return sparseMatrix.columnBasedRowIndices()[column];
    }

    @Override
    public VectorEntry getVectorEntryAtPosition(int position) {
        MatrixBasedSparseVectorEntry matrixBasedSparseVectorEntry = new MatrixBasedSparseVectorEntry();
        matrixBasedSparseVectorEntry.position = position;
        return matrixBasedSparseVectorEntry;
    }

    @Override
    public int getNumEntries() {
        return length;
    }

    @Override
    @Deprecated
    public void set(int index, double value) {
        sparseMatrix.set(index, column, value);
    }

    @Override
    @Deprecated
    public double get(int index) {
        return sparseMatrix.get(index, column);
    }

    @Override
    public Iterator<VectorEntry> iterator() {
        return new MatrixBasedSequentialAccessIterator();
    }

    private final class MatrixBasedSequentialAccessIterator implements Iterator<VectorEntry> {
        private final MatrixBasedSparseVectorEntry vectorEntry = new MatrixBasedSparseVectorEntry();

        @Override
        public boolean hasNext() {
            return vectorEntry.getNextOffset() < getNumEntries();
        }

        @Override
        public MatrixBasedSparseVectorEntry next() {
            vectorEntry.advanceOffset();
            return vectorEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public final class MatrixBasedSparseVectorEntry implements VectorEntry {
        private int position = -1;

        void advanceOffset() {
            position++;
        }

        int getNextOffset() {
            return position + 1;
        }

        @Override
        public double get() {
            return sparseMatrix.getAtRowPosition(position, column);
        }

        @Override
        public int index() {
            return indices[position];
        }

        /**
         * @return the position of this vector element.
         * For dense vector and random sparse vector, the values of position and index are the same, i.e., index() = position()
         */
        @Override
        public int position() {
            return position;
        }

        @Override
        public void set(double value) {
            sparseMatrix.setAtRowPosition(position, column, value);
        }
    }
}
