package net.librec.math.structure;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * dense matrix based dense vector
 *
 * @author Keqiang Wang (email:sei.wkq2008@gmail.com)
 */
public class MatrixBasedDenseVector extends DenseVector {
    private double[][] values;
    private int column;

    protected MatrixBasedDenseVector(int size) {
        super(size);
    }

    public MatrixBasedDenseVector(DenseMatrix matrix, int column) {
        super(matrix.rowSize());
        values = matrix.getValues();
        this.column = column;
    }

    @Override
    public MatrixBasedDenseVector clone() {
        return (MatrixBasedDenseVector) super.clone();
    }

    @Override
    public double[] getValues() {
        double[] tmp_values = new double[cardinality];
        for (VectorEntry vectorEntry : this) {
            tmp_values[vectorEntry.index()] = vectorEntry.get();
        }
        return tmp_values;
    }

    @Override
    public void set(int index, double value) {
        values[index][column] = value;
    }

    @Override
    public double get(int index) {
        return values[index][column];
    }

    @Override
    public Iterator<Vector.VectorEntry> iterator() {
        return new DenseVectorIterator();
    }

    private final class DenseVectorIterator implements Iterator<Vector.VectorEntry> {
        private final MatrixDenseVectorEntry element = new MatrixDenseVectorEntry(-1);

        @Override
        public boolean hasNext() {
            return element.index + 1 < cardinality();
        }

        @Override
        public Vector.VectorEntry next() {
            if (element.index + 1 >= cardinality()) { // If the end is reached.
                throw new NoSuchElementException();
            }
            element.index++;
            return element;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class MatrixDenseVectorEntry implements Vector.VectorEntry {
        int index;

        public MatrixDenseVectorEntry(int index) {
            this.index = index;
        }

        @Override
        public double get() {
            return values[index][column];
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int position() {
            return index;
        }

        @Override
        public void set(double value) {
            values[index][column] = value;
        }
    }
}
