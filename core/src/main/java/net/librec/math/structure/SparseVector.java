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

import net.librec.math.algorithm.Stats;

import java.io.Serializable;
import java.util.*;


/**
 * Data Structure: Sparse Vector whose implementation is modified from M4J
 * library
 *
 * @author guoguibing and Keqiang Wang
 */
public class SparseVector implements Iterable<VectorEntry>, Serializable {

    private static final long serialVersionUID = 1151609203685872657L;

    static final float DEFAULT_COMPRESS_FACTOR = 0.75f;
    static final float DEFAULT_LOAD_FACTOR = 0.375f;

    // capacity
    protected int capacity;

    // data
    protected double[] data;

    // Indices to data
    protected int[] index;

    // number of items
    protected int count;

    // number of zero items
    protected int zeroCount;

    // the first index of zero items
    protected int zeroFirstIndex;

    // true: auto compress , false: not auto compress
    protected boolean autoCompress;

    {
        autoCompress = true;
        zeroCount = 0;
    }

    /**
     * Construct a sparse vector with its maximum capacity
     *
     * @param capcity maximum size of the sparse vector
     */
    public SparseVector(int capcity) {
        this.capacity = capcity;
        data = new double[0];

        count = 0;
        index = new int[0];

        zeroFirstIndex = 0;
    }


    /**
     * Construct a sparse vector with its maximum capacity
     *
     * @param capcity maximum size of the sparse vector
     * @param dataLength length of the data
     */
    public SparseVector(int capcity, int dataLength) {
        this.capacity = capcity;
        data = new double[dataLength];

        count = 0;
        index = new int[dataLength];

        zeroFirstIndex = 0;
    }

    /**
     * Construct a sparse vector with its maximum capacity, filled with given
     * data array
     *
     * @param capcity maximum size of the sparse vector
     * @param array   input data
     */
    public SparseVector(int capcity, double[] array) {
        this(capcity);

        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                this.set(i, array[i]);
            }
        }
        zeroFirstIndex = count;
    }

    /**
     * Construct a sparse vector by deeply copying with tis maximum capacity, indices to data, and data
     *
     * @param capacity  maximum size of the sparse vector
     * @param index     indices to data
     * @param data      data
     */
    public SparseVector(int capacity, int[] index, double[] data) {
        this(capacity, index, data, 0, index.length - 1);
    }

    /**
     * Construct a sparse vector by deeply copying with tis maximum capacity, indices to data, and data
     *
     * @param capacity  maximum size of the sparse vector
     * @param index     indices to data
     * @param data      data
     * @param startIdx  start index
     * @param endIdx    end index
     */
    public SparseVector(int capacity, int[] index, double[] data, int startIdx, int endIdx) {
        this.capacity = capacity;
        int length = endIdx - startIdx + 1;

        this.index = new int[length];
        this.data = new double[length];
        for (int idx = startIdx, idxData = 0; idx <= endIdx; idx++) {
            if (data[idx] != 0.0) {
                this.data[idxData] = data[idx];
                this.index[idxData] = index[idx];
                idxData++;
                count++;
            }
        }

        zeroCount = 0;
        zeroFirstIndex = count;
    }

    /**
     * Construct a sparse vector by deeply copying another vector
     *
     * @param sv another vector
     */
    public SparseVector(SparseVector sv) {
        this(sv.capacity, sv.data);
    }

    /**
     * Check if a vector contains a specific index
     *
     * @param idx the idex to search
     * @return true if a vector contains a specific index
     */
    public boolean contains(int idx) {
        return Arrays.binarySearch(index, 0, count, idx) >= 0;
    }

    /**
     * @return a copy of internal data (to prevent changes outside)
     */
    public double[] getData() {
        int nonZeroCount = count - zeroCount;
        double[] res = new double[nonZeroCount];
        for (int i = 0, idx = 0; i < count; i++) {
            if (data[i] != 0.0) {
                res[idx++] = data[i];
            }
        }

        return res;
    }

    /**
     * @return a copy of indices (to prevent changes outside)
     */
    public int[] getIndex() {
        int nonZeroCount = count - zeroCount;
        int[] res = new int[nonZeroCount];
        for (int i = 0, idx = 0; i < count; i++) {
            if (data[i] != 0.0) {
                res[idx++] = index[i];
            }
        }

        return res;
    }

    /**
     * @return a list of indices (to prevent changes outside)
     */
    public List<Integer> getIndexList() {
        List<Integer> res = new ArrayList<Integer>((int) (count * 1.5));
        for (int i = 0; i < count; i++) {
            if (data[i] != 0.0) {
                res.add(index[i]);
            }
        }
        return res;
    }

    /**
     * @return a list of indices (to prevent changes outside)
     */
    public Set<Integer> getIndexSet() {
        Set<Integer> res = new HashSet<>((int) (count * 1.5));
        for (int i = 0; i < count; i++) {
            if (data[i] != 0.0) {
                res.add(index[i]);
            }
        }
        return res;
    }

    /**
     * Number of entries in the sparse structure
     *
     * @return number of entries in the sparse structure
     */
    public int getCount() {
        return count;
    }

    /**
     * Set a value to entry [idx]
     *
     * @param idx  index
     * @param val  the value to set
     */
    public void set(int idx, double val) {
        check(idx);
        int i = getIndex(idx);
        data[i] = val;
        if (val == 0.0) {
            zeroCount++;
        }
    }

    /**
     * Add a value to entry [idx]
     *
     * @param idx  index
     * @param val  the value to add
     */
    public void add(int idx, double val) {
        check(idx);

        int i = getIndex(idx);
        data[i] += val;
        if (data[i] == 0.0) {
            zeroCount++;
        }
    }

    /**
     * append a value to entry [idx] if the idx is sorted
     *
     * @param idx  index
     * @param val  the value to append
     */
    public void append(int idx, double val) {
        check(idx);

        if (val == 0.0) {
            return;
        }
        int[] newIndex = index;
        double[] newData = data;
        if (++count > data.length) {

            // If zero-length, use new length of 1, else double the bandwidth
            int newLength = data.length != 0 ? data.length << 1 : 1;

            // Copy existing data into new arrays
            newIndex = new int[newLength];
            newData = new double[newLength];
            System.arraycopy(index, 0, newIndex, 0, data.length);
            System.arraycopy(data, 0, newData, 0, data.length);
        }
        newIndex[count - 1] = idx;
        newData[count - 1] = val;

        index = newIndex;
        data = newData;
    }

    /**
     * compress the sparse vector
     */
    public void compress() {
        for (int idx = zeroFirstIndex + 1; idx < count; idx++) {
            if (data[idx] != 0.0) {
                data[zeroFirstIndex] = data[idx];
                index[zeroFirstIndex] = index[idx];
                zeroFirstIndex++;
            }
        }
        count = zeroFirstIndex;
        zeroCount = 0;
    }

    /**
     * Retrieve a value at entry [idx]
     *
     * @param idx index
     * @return a value at entry [idx]
     */
    public double get(int idx) {
        check(idx);

        int i = Arrays.binarySearch(index, 0, count, idx);

        return i >= 0 ? data[i] : 0;
    }

    /**
     * Return inner product with a given sparse vector
     *
     * @param vec a given sparse vector
     * @return inner product with a given sparse vector
     */
    public double inner(SparseVector vec) {
        assert capacity == vec.capacity;
        double res = 0.0;
        Iterator<VectorEntry> vecEntryIterator = iterator();
        while (vecEntryIterator.hasNext()) {
            VectorEntry entry = vecEntryIterator.next();
            res += entry.get() * vec.get(entry.index());
        }
        return res;
    }

    /**
     * Return inner product with a given dense vector.
     *
     * @param vec a given dense vector
     * @return inner product with a given dense vector
     */
    public double inner(DenseVector vec) {
        assert capacity == vec.size;

        double res = 0.0;
        Iterator<VectorEntry> vecEntryIterator = iterator();
        while (vecEntryIterator.hasNext()) {
            VectorEntry entry = vecEntryIterator.next();
            res += entry.get() * vec.get(entry.index());
        }

        return res;
    }

    /**
     * @return sum of vector entries
     */
    public double sum() {
        return Stats.sum(data);
    }

    /**
     * @return mean of vector entries
     */
    public double mean() {
        return sum() / size();
    }

    /**
     * @return the cardinary of a sparse vector
     */
    public int size() {
        return count - zeroCount;
    }

    /**
     * Checks the index
     *
     * @param idx index
     */
    protected void check(int idx) {
        if (idx < 0)
            throw new IndexOutOfBoundsException("index is negative (" + idx + ")");
        if (idx >= capacity)
            throw new IndexOutOfBoundsException("index >= size (" + idx + " >= " + capacity + ")");
    }

    /**
     * Tries to find the index. If it is not found, a reallocation is done, and
     * a new index is returned.
     *
     * @param idx index
     * @return  If the index is not found, a reallocation is done, and
     *          a new index is returned.
     */
    private int getIndex(int idx) {
        // Try to find column index
        int i = Arrays.binarySearch(index, 0, count, idx);

        // Found
        if (i >= 0 && index[i] == idx)
            return i;

        int[] newIndex = index;
        double[] newData = data;

        // get insert position
        i = -(i + 1);

        // Check available memory
        if (++count > data.length) {

            // If zero-length, use new length of 1, else double the bandwidth
            int newLength = data.length != 0 ? data.length << 1 : 1;

            // Copy existing data into new arrays
            newIndex = new int[newLength];
            newData = new double[newLength];
            System.arraycopy(index, 0, newIndex, 0, i);
            System.arraycopy(data, 0, newData, 0, i);
        }

        // All ok, make room for insertion
        System.arraycopy(index, i, newIndex, i + 1, count - i - 1);
        System.arraycopy(data, i, newData, i + 1, count - i - 1);

        // Put in new structure
        newIndex[i] = idx;
        newData[i] = 0.;

        // Update pointers
        index = newIndex;
        data = newData;

        // Return insertion index
        return i;
    }

    public Iterator<VectorEntry> iterator() {
        return new SparseVecIterator();
    }

    /**
     * Iterator over a sparse vector
     */
    private class SparseVecIterator implements Iterator<VectorEntry> {

        private int cursor;

        private final SparseVecEntry entry;

        private SparseVecIterator() {
            entry = new SparseVecEntry();
            cursor = 0;
            entry.update(cursor);
            while ((cursor + 1) < count && entry.get() == 0.0) {
                cursor++;
                entry.update(cursor);
            }
        }

        public boolean hasNext() {
            return cursor < count;
        }

        public VectorEntry next() {
            entry.update(cursor);

            cursor++;
            SparseVecEntry tempEntry = new SparseVecEntry();
            tempEntry.update(cursor);

            while (hasNext() && tempEntry.get() == 0.0) {
                cursor++;
                tempEntry.update(cursor);
            }

            return entry;
        }

        public void remove() {
            entry.set(0.0);
        }

    }

    /**
     * Entry of a sparse vector
     */
    private class SparseVecEntry implements VectorEntry {

        private int cursor;

        public void update(int cursor) {
            this.cursor = cursor;
        }

        public int index() {
            return index[cursor];
        }

        public double get() {
            return data[cursor];
        }

        public void set(double value) {
            data[cursor] = value;
            if (value == 0.0) {
                zeroCount++;
            }
        }

    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d\t%d\n", new Object[]{capacity, count}));

        for (VectorEntry ve : this)
            if (ve.get() != 0)
                sb.append(String.format("%d\t%f\n", new Object[]{ve.index(), ve.get()}));

        return sb.toString();
    }

    /**
     * @return a map of {index, data} of the sparse vector
     */
    public Map<Integer, Double> toMap() {
        Map<Integer, Double> map = new HashMap<Integer, Double>();
        for (int i = 0; i < count; i++) {
            int idx = index[i];
            double val = data[i];

            if (val != 0)
                map.put(idx, val);
        }
        return map;
    }

    /**
     * @return capacity of vector
     */
    public int getCapacity() {
        return capacity;
    }
}
