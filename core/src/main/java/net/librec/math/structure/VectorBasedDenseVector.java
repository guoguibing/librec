package net.librec.math.structure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements vector as an array of doubles
 *
 * @author Keqiang Wang (email:sei.wkq2008@gmail.com)
 */
public class VectorBasedDenseVector extends DenseVector {

    private double[] values;

    /**
     * For serialization purposes only
     */
    public VectorBasedDenseVector() {
        super(0);
    }

    /**
     * Construct a new instance using provided values
     *
     * @param values - array of values
     */
    public VectorBasedDenseVector(double[] values) {
        this(values, false);
    }

    public VectorBasedDenseVector(double[] values, boolean shallowCopy) {
        super(values.length);
        if (!shallowCopy) {
            this.values = new double[values.length];
            System.arraycopy(values, 0, this.values, 0, values.length);
        } else {
            this.values = values;
        }
        this.values = shallowCopy ? values : values.clone();
    }

    public VectorBasedDenseVector(VectorBasedDenseVector values, boolean shallowCopy) {
        this(values.values, shallowCopy);
    }

    /**
     * Construct a new instance of the given cardinality
     *
     * @param cardinality - number of values in the vector
     */
    public VectorBasedDenseVector(int cardinality) {
        super(cardinality);
        this.values = new double[cardinality];
    }

    /**
     * Copy-constructor (for use in turning a sparse vector into a dense one, for example)
     *
     * @param vector The vector to clone
     */
    public VectorBasedDenseVector(Vector vector) {
        super(vector.cardinality());
        values = new double[vector.cardinality()];
        for (VectorEntry vectorEntry : vector) {
            values[vectorEntry.index()] = vectorEntry.get();
        }
    }

    @Override
    public VectorBasedDenseVector clone() {
        return (VectorBasedDenseVector) super.clone();
    }

    public double[] getValues() {
        return values;
    }

    @Override
    public VectorEntry getVectorEntry(int index) {
        return new DenseVectorEntry(index);
    }

    @Override
    public void set(int index, double value) {
        values[index] = value;
    }

    @Override
    public double get(int index) {
        return values[index];
    }

    @Override
    public Iterator<VectorEntry> iterator() {
        return new DenseVectorIterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof VectorBasedDenseVector) {
            // Speedup for DenseVectors
            return Arrays.equals(values, ((VectorBasedDenseVector) o).values);
        }
        return super.equals(o);
    }

    private final class DenseVectorIterator implements Iterator<VectorEntry> {
        private final DenseVectorEntry vectorEntry = new DenseVectorEntry(-1);

        @Override
        public boolean hasNext() {
            return vectorEntry.index + 1 < cardinality();
        }

        @Override
        public VectorEntry next() {
            if (!hasNext()) { // If the end is reached.
                throw new NoSuchElementException();
            }
            vectorEntry.index++;
            return vectorEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class DenseVectorEntry implements VectorEntry {
        int index;

        private DenseVectorEntry(int index) {
            this.index = index;
        }

        @Override
        public double get() {
            return values[index];
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
            values[index] = value;
        }
    }
}
