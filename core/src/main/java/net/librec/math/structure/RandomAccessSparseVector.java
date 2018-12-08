package net.librec.math.structure;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements vector that only stores non-zero doubles
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class RandomAccessSparseVector extends AbstractVector {

    private static final int INITIAL_CAPACITY = 11;

    private Int2DoubleOpenHashMap values;

    /**
     * For serialization purposes only.
     */
    public RandomAccessSparseVector() {
        super(0);
    }

    public RandomAccessSparseVector(int cardinality) {
        this(cardinality, Math.min(cardinality, INITIAL_CAPACITY)); // arbitrary estimate of 'sparseness'
    }

    public RandomAccessSparseVector(int cardinality, int initialCapacity) {
        super(cardinality);
        values = new Int2DoubleOpenHashMap(initialCapacity, .5f);
    }

    public RandomAccessSparseVector(Vector other) {
        this(other.cardinality(), other.getNumEntries());
        for (VectorEntry e : other) {
            values.put(e.index(), e.get());
        }
    }

    private RandomAccessSparseVector(int cardinality, Int2DoubleOpenHashMap values) {
        super(cardinality);
        this.values = values;
    }

    public RandomAccessSparseVector(RandomAccessSparseVector other, boolean shallowCopy) {
        super(other.cardinality());
        values = shallowCopy ? other.values : other.values.clone();
    }

    @Override
    public String toString() {
        return sparseVectorToString();
    }


    /**
     * @return false
     */
    @Override
    public boolean isDense() {
        return false;
    }

    /**
     * @return false
     */
    @Override
    public boolean isSequentialAccess() {
        return false;
    }

    @Override
    public RandomAccessSparseVector clone() {
        return new RandomAccessSparseVector(cardinality(), values.clone());
    }

    @Override
    public void set(int index, double value) {
        values.put(index, value);
    }

    @Override
    public double get(int index) {
        return values.getOrDefault(index, 0.0D);
    }

    public boolean contains(int index) {
        return values.containsKey(index);
    }

    @Override
    public int getNumEntries() {
        return values.size();
    }


    @Override
    public double getLengthSquared() {
        return 0;
    }

    @Override
    public double getLookupCost() {
        return 1;
    }

    @Override
    public double getIteratorAdvanceCost() {
        return 1 + Hash.DEFAULT_LOAD_FACTOR / 2;
    }

    /**
     * This is "sort of" constant, but really it might resize the array.
     */
    @Override
    public boolean isAddConstantTime() {
        return true;
    }

    @Override
    public Iterator<VectorEntry> iterator() {
        return new RandomAccessIterator();
    }

    private final class RandomAccessIterator implements Iterator<VectorEntry> {
        final ObjectIterator<Entry> fastIterator = values.int2DoubleEntrySet().fastIterator();
        final RandomAccessEntry element = new RandomAccessEntry(fastIterator);

        @Override
        public boolean hasNext() {
            return fastIterator.hasNext();
        }

        @Override
        public RandomAccessEntry next() {
            if (!hasNext()) throw new NoSuchElementException();
            element.entry = fastIterator.next();
            return element;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public class RandomAccessEntry implements VectorEntry {
        final ObjectIterator<Entry> fastIterator;
        Entry entry;

        RandomAccessEntry(ObjectIterator<Entry> fastIterator) {
            super();
            this.fastIterator = fastIterator;
        }

        @Override
        public double get() {
            return entry.getDoubleValue();
        }

        @Override
        public int index() {
            return entry.getIntKey();
        }

        @Override
        public int position(){
            return index();
        }

        @Override
        public void set(double value) {
            entry.setValue(value);
        }
    }
}
