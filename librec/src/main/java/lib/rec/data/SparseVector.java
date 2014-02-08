package lib.rec.data;

import happy.coding.io.Logs;

import java.util.Arrays;
import java.util.Iterator;

public class SparseVector implements Iterable<VectorEntry> {

	// capacity
	protected int size;

	// data
	protected double[] data;

	// Indices to data
	protected int[] index;

	// How much has been used
	protected int used;

	public SparseVector(int size) {
		this.size = size;
		data = new double[0];

		used = 0;
		index = new int[0];
	}

	public SparseVector(int size, double[] array) {
		this(size);

		for (int i = 0; i < array.length; i++)
			if (array[i] != 0)
				this.set(i, array[i]);

		System.out.println();
	}

	public SparseVector(SparseVector sv) {
		this.size = sv.size;
		this.data = Arrays.copyOf(sv.data, sv.data.length);
		this.used = sv.used;
		this.index = Arrays.copyOf(sv.index, sv.index.length);
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
	 * Returns the internal data
	 */
	public double[] getData() {
		return data;
	}

	/**
	 * Returns the indices
	 */
	public int[] getIndex() {
		if (used == index.length)
			return index;

		int[] indices = new int[used];
		for (int i = 0; i < used; i++)
			indices[i] = index[i];

		return indices;
	}

	/**
	 * Number of entries used in the sparse structure
	 */
	public int getUsed() {
		return used;
	}

	public void set(int idx, double val) {
		check(idx);

		int i = getIndex(idx);
		data[i] = val;
	}

	public void add(int idx, double val) {
		check(idx);

		int i = getIndex(idx);
		data[i] += val;
	}

	public double get(int idx) {
		check(idx);

		int in = Arrays.binarySearch(index, 0, used, idx);
		if (in >= 0)
			return data[in];
		return 0;
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
		if (idx >= size)
			throw new IndexOutOfBoundsException("index >= size (" + idx + " >= " + size + ")");
	}

	/**
	 * Tries to find the index. If it is not found, a reallocation is done, and
	 * a new index is returned.
	 */
	private int getIndex(int idx) {
		// Try to find column index
		int i = Arrays.binarySearch(index, 0, used, idx);

		// Found
		if (i >= 0 && index[i] == idx)
			return i;

		int[] newIndex = index;
		double[] newData = data;

		// get insert position
		i = -(i + 1);

		// Check available memory
		if (++used > data.length) {

			// If zero-length, use new length of 1, else double the bandwidth
			int newLength = data.length != 0 ? data.length << 1 : 1;

			// Copy existing data into new arrays
			newIndex = new int[newLength];
			newData = new double[newLength];
			System.arraycopy(index, 0, newIndex, 0, i);
			System.arraycopy(data, 0, newData, 0, i);
		}

		// All ok, make room for insertion
		System.arraycopy(index, i, newIndex, i + 1, used - i - 1);
		System.arraycopy(data, i, newData, i + 1, used - i - 1);

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
			return cursor < used;
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
		sb.append(String.format("%d\t%d\n", new Object[] { size, used }));

		for (VectorEntry ve : this)
			if (ve.get() != 0)
				sb.append(String.format("%d\t%f\n", new Object[] { ve.index(), ve.get() }));

		return sb.toString();
	}

	public static void main(String[] args) {
		double[] array = { 0, 7, 8, 7, 0, 1 };
		SparseVector vec = new SparseVector(10, array);

		Logs.debug(vec);

		vec.set(6, 10);
		vec.set(9, 11);
		Logs.debug(vec);
	}
}
