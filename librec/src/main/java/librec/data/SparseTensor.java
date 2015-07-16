// Copyright (C) 2014-2015 Guibing Guo
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

import happy.coding.io.Logs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * 
 * Data Structure: Sparse Tensor <br>
 * 
 * <p>
 * For easy documentation, here we use (i-entry, value) to indicate each entry of a tensor, where the term {@value
 * i-entry} is short for the indices of an entry.
 * </p>
 * 
 * @author Guo Guibing
 *
 */
public class SparseTensor {

	/**
	 * number of dimensions except the dimension of values
	 */
	private int numDimensions;
	private List<Integer>[] ndArray; // n-dimensional array
	private List<Double> values; // values

	private Multimap<Integer, Integer>[] ndIndices; // each multimap = {key, {pos1, pos2, ...}}

	/**
	 * Construct an empty sparse tensor
	 * 
	 * @param nd
	 *            number of dimensions of the tensor
	 */
	@SuppressWarnings("unchecked")
	public SparseTensor(int nd) {

		numDimensions = nd;
		ndArray = (List<Integer>[]) new List<?>[nd];
		ndIndices = (Multimap<Integer, Integer>[]) new Multimap<?, ?>[nd];

		for (int d = 0; d < numDimensions; d++) {
			ndArray[d] = new ArrayList<Integer>();
			ndIndices[d] = HashMultimap.create();
		}

		values = new ArrayList<>();
	}

	/**
	 * If the given i-entry exists, a value is added to original value. If not, a new entry is added to the tensor.
	 * 
	 * @param val
	 *            value to add
	 * @param nd
	 *            i-entry
	 */
	public void add(double val, int... nd) {

		int index = findIndex(nd);

		// if i-entry exists
		if (index >= 0) {
			// update value
			values.set(index, values.get(index) + val);
			return;
		}

		// if i-entry does not exist
		for (int d = 0; d < numDimensions; d++) {
			ndArray[d].add(nd[d]);

			// update indices if necessary
			Multimap<Integer, Integer> indices = ndIndices[d];
			if (indices != null && indices.size() > 0) {
				indices.put(nd[d], ndArray[d].size() - 1);
			}
		}
		values.add(val);
	}

	/**
	 * Set a value to a specific i-entry
	 * 
	 * @param val
	 *            value to set
	 * @param nd
	 *            i-entry
	 */
	public void set(double val, int... nd) {
		int index = findIndex(nd);

		if (index >= 0)
			values.set(index, val);
	}

	/**
	 * @return true if a given i-entry is removed and false otherwise.
	 */
	public boolean remove(int... nd) {

		int index = findIndex(nd);

		if (index < 0)
			return false;

		for (int d = 0; d < numDimensions; d++) {
			ndArray[d].remove(index);

			// update indices if necessary
			Multimap<Integer, Integer> indices = ndIndices[d];
			if (indices != null && indices.size() > 0) {
				indices.remove(nd[d], index);
			}
		}
		values.remove(index);

		return true;
	}

	/**
	 * find the inner index of a given i-entry
	 */
	private int findIndex(int... nd) {

		if (nd.length != numDimensions)
			throw new Error("Tensor dimensions do not match with the given input");

		// first, retrieve from indexed dimension
		for (int d = 0; d < numDimensions; d++) {
			Multimap<Integer, Integer> indices = ndIndices[d];
			// if indexed
			if (indices.size() > 0) {
				int key = nd[d];
				// all relevant positions
				Collection<Integer> pos = indices.get(key);
				if (pos == null)
					return -1;
				// for each possible position
				for (int p : pos) {
					boolean found = true;
					for (int dd = 0; dd < nd.length; dd++) {
						if (nd[dd] != ndArray[dd].get(p)) {
							found = false;
							break;
						}
					}
					if (found)
						return p;
				}

				// if not found
				return -1;
			}
		}

		// otherwise, let's index the first dimension and invoke this method again
		buildIndex(0);

		return findIndex(nd);
	}

	/**
	 * Check if a given i-entry exists in the tensor
	 * 
	 * @param nd
	 *            i-entry
	 * @return true if found, and false otherwise
	 */
	public boolean contains(int... nd) {
		return findIndex(nd) >= 0 ? true : false;
	}

	/**
	 * @return a value given a specific i-entry
	 */
	public double get(int... nd) {
		assert nd.length == this.numDimensions;

		int index = findIndex(nd);
		return index < 0 ? 0 : values.get(index);
	}

	/**
	 * build index at dimensions nd
	 * 
	 * @param nd
	 *            dimensions to be indexed
	 */
	public void buildIndex(int... nd) {
		for (int d : nd) {
			for (int index = 0; index < ndArray[d].size(); index++) {
				int key = ndArray[d].get(index);
				ndIndices[d].put(key, index);
			}
		}
	}

	/**
	 * build index for all dimensions
	 */
	public void buildIndices() {
		for (int d = 0; d < numDimensions; d++) {
			buildIndex(d);
		}
	}

	/**
	 * @return indices (positions) of a key in dimension d
	 */
	public Collection<Integer> getIndex(int d, int key) {
		Multimap<Integer, Integer> indices = ndIndices[d];
		if (indices == null || indices.size() == 0)
			buildIndex(d);

		return indices.get(key);
	}

	/**
	 * @param sd
	 *            source dimension
	 * @param key
	 *            key in the source dimension
	 * @param td
	 *            target dimension
	 * 
	 * @return indices in a target dimension {@code td} related with a key in dimension {@code sd}
	 */
	public List<Integer> getRelevantIndex(int sd, int key, int td) {
		Collection<Integer> indices = getIndex(sd, key);
		List<Integer> res = null;
		if (indices != null) {
			res = new ArrayList<>();
			for (int index : indices) {
				res.add(ndArray[td].get(index));
			}
		}

		return res;
	}

	/**
	 * @return number of entries of the tensor
	 */
	public int size() {
		return values.size();
	}

	/**
	 * Slice a tensor to form a new matrix (row, column, value)
	 * 
	 * @param rowDim
	 *            row dimension
	 * @param colDim
	 *            column dimension
	 * @param valDim
	 *            value dimension
	 * @return a sparse matrix
	 */
	public SparseMatrix sliceMatrix(int rowDim, int colDim, int valDim, int numRows, int numCols) {

		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		for (int index = 0, mindex = size(); index < mindex; index++) {
			int row = ndArray[rowDim].get(index);
			int col = ndArray[colDim].get(index);
			double val = ndArray[valDim].get(index);

			dataTable.put(row, col, val);
			colMap.put(col, row);
		}

		return new SparseMatrix(numRows, numCols, dataTable, colMap);
	}

	/**
	 * Slice a tensor to form a new matrix (row, column, value)
	 * 
	 * @param rowDim
	 *            row dimension
	 * @param colDim
	 *            column dimension
	 * @return a sparse matrix
	 */
	public SparseMatrix sliceMatrix(int rowDim, int colDim, int numRows, int numCols) {

		Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
		Multimap<Integer, Integer> colMap = HashMultimap.create();

		for (int index = 0, mindex = size(); index < mindex; index++) {
			int row = ndArray[rowDim].get(index);
			int col = ndArray[colDim].get(index);
			double val = values.get(index);

			dataTable.put(row, col, val);
			colMap.put(col, row);
		}

		return new SparseMatrix(numRows, numCols, dataTable, colMap);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("N-Dimension: ").append(numDimensions).append(", Size: ").append(size()).append("\n");
		for (int i = 0; i < values.size(); i++) {
			for (int d = 0; d < numDimensions; d++) {
				sb.append(ndArray[d].get(i)).append("\t");
			}
			sb.append(values.get(i)).append("\n");
		}

		return sb.toString();
	}

	/**
	 * Usage demonstration
	 */
	public static void main(String[] args) {
		SparseTensor st = new SparseTensor(3);
		st.add(1.0, 1, 0, 0);
		st.add(2.0, 1, 1, 0);
		st.add(3.0, 2, 0, 0);
		st.add(4.0, 1, 3, 0);
		st.add(5.0, 1, 0, 6);
		st.add(6.0, 3, 1, 4);

		Logs.debug(st);
		Logs.debug("I-Entry (1, 0, 0) = {}", st.get(1, 0, 0));
		Logs.debug("I-Entry (1, 1, 0) = {}", st.get(1, 1, 0));
		Logs.debug("I-Entry (1, 2, 0) = {}", st.get(1, 2, 0));
		Logs.debug("I-Entry (2, 0, 0) = {}", st.get(2, 0, 0));
		Logs.debug("I-Entry (1, 0, 6) = {}", st.get(1, 0, 6));
		Logs.debug("I-Entry (3, 1, 4) = {}", st.get(3, 1, 4));

		Logs.debug("dimension 0 key 1 = {}", st.getIndex(0, 1));
		Logs.debug("dimension 1 key 3 = {}", st.getIndex(1, 3));
		Logs.debug("dimension 2 key 1 = {}", st.getIndex(2, 1));
		Logs.debug("dimension 2 key 6 = {}", st.getIndex(2, 6));

		st.add(4.5, 2, 1, 1);
		Logs.debug(st);
		Logs.debug("dimension 2 key 1 = {}", st.getIndex(2, 1));
		st.remove(2, 1, 1);
		Logs.debug("dimension 2 key 1 = {}", st.getIndex(2, 1));

		Logs.debug("index of i-entry (1, 2, 0) = {}, value = {}", st.findIndex(1, 2, 0), st.get(1, 2, 0));
		Logs.debug("index of i-entry (3, 1, 4) = {}, value = {}", st.findIndex(3, 1, 4), st.get(3, 1, 4));

		Logs.debug("indices in dimension 2 associated with dimension 0 key 1 = {}", st.getRelevantIndex(0, 1, 2));
	}

}
