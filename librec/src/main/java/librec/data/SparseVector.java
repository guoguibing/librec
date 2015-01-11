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

package librec.data;

import happy.coding.math.Stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Data Structure: Sparse Vector whose implementation is modified from M4J
 * library
 * 
 * @author guoguibing
 * 
 */
public class SparseVector implements Iterable<VectorEntry>, Serializable {

	private static final long serialVersionUID = 1151609203685872657L;

	// capacity
	protected int capacity;

	// data
	protected double[] data;

	// Indices to data
	protected int[] index;

	// number of items
	protected int count;

	/**
	 * Construct a sparse vector with its maximum capacity
	 * 
	 * @param capcity
	 *            maximum size of the sparse vector
	 */
	public SparseVector(int capcity) {
		this.capacity = capcity;
		data = new double[0];

		count = 0;
		index = new int[0];
	}

	/**
	 * Construct a sparse vector with its maximum capacity, filled with given
	 * data array
	 * 
	 * @param capcity
	 *            maximum size of the sparse vector
	 * @param array
	 *            input data
	 */
	public SparseVector(int capcity, double[] array) {
		this(capcity);

		for (int i = 0; i < array.length; i++)
			if (array[i] != 0)
				this.set(i, array[i]);
	}

	/**
	 * Construct a sparse vecto by deeply copying another vector
	 */
	public SparseVector(SparseVector sv) {
		this(sv.capacity, sv.data);
	}

	/**
	 * Check if a vector contains a specific index
	 * 
	 * @param idx
	 *            the idex to search
	 */
	public boolean contains(int idx) {
		return Arrays.binarySearch(index, idx) >= 0;
	}

	/**
	 * @return a copy of internal data (to prevent changes outside)
	 */
	public double[] getData() {
		double[] res = new double[count];
		for (int i = 0; i < count; i++)
			res[i] = data[i];

		return res;
	}

	/**
	 * @return a copy of indices (to prevent changes outside)
	 */
	public int[] getIndex() {
		int[] res = new int[count];
		for (int i = 0; i < count; i++)
			res[i] = index[i];

		return res;
	}

	/**
	 * @return a list of indices (to prevent changes outside)
	 */
	public List<Integer> getIndexList() {
		List<Integer> res = new ArrayList<>((int) (count * 1.5));
		for (int i = 0; i < count; i++)
			res.add(index[i]);

		return res;
	}

	/**
	 * Number of entries in the sparse structure
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set a value to entry [idx]
	 */
	public void set(int idx, double val) {
		check(idx);

		int i = getIndex(idx);
		data[i] = val;
	}

	/**
	 * Add a value to entry [idx]
	 */
	public void add(int idx, double val) {
		check(idx);

		int i = getIndex(idx);
		data[i] += val;
	}

	/**
	 * Retrieve a value at entry [idx]
	 */
	public double get(int idx) {
		check(idx);

		int i = Arrays.binarySearch(index, 0, count, idx);

		return i >= 0 ? data[i] : 0;
	}

	/**
	 * @return inner product with a given sparse vector
	 */
	public double inner(SparseVector vec) {
		double res = 0;
		for (int idx : this.getIndex()) {
			if (vec.contains(idx))
				res += get(idx) * vec.get(idx);
		}

		return res;
	}

	/**
	 * @return inner product with a given dense vector
	 */
	public double inner(DenseVector vec) {
		double res = 0;
		for (int idx : this.getIndex())
			res += get(idx) * vec.get(idx);

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
		return sum() / count;
	}

	/**
	 * @return the cardinary of a sparse vector
	 */
	public int size() {
		int num = 0;
		for (VectorEntry ve : this)
			if (ve.get() != 0)
				num++;

		return num;
	}

	/**
	 * Checks the index
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

		private final SparseVecEntry entry = new SparseVecEntry();

		public boolean hasNext() {
			return cursor < count;
		}

		public VectorEntry next() {
			entry.update(cursor);

			cursor++;

			return entry;
		}

		public void remove() {
			entry.set(0);
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
		}

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%d\t%d\n", new Object[] { capacity, count }));

		for (VectorEntry ve : this)
			if (ve.get() != 0)
				sb.append(String.format("%d\t%f\n", new Object[] { ve.index(), ve.get() }));

		return sb.toString();
	}

	/**
	 * @return a map of {index, data} of the sparse vector
	 */
	public Map<Integer, Double> toMap() {
		Map<Integer, Double> map = new HashMap<>();
		for (int i = 0; i < count; i++) {
			int idx = index[i];
			double val = data[i];

			if (val != 0)
				map.put(idx, val);
		}

		return map;
	}

}
