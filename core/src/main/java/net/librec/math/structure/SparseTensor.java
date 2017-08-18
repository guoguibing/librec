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
package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import net.librec.math.algorithm.Randoms;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Data Structure: Sparse Tensor <br>
 * <p>
 * For easy documentation, here we use {@code (keys, value)} to indicate each entry of a tensor, and {@code index} is
 * used to indicate the position in which the entry is stored in the lists.
 * </p>
 * <Strong>Reference:</strong> Kolda and Bader, <strong>Tensor Decompositions and Applications</strong>, SIAM REVIEW,
 * Vol. 51, No. 3, pp. 455–500
 *
 * @author Guo Guibing
 */
public class SparseTensor implements DataSet, Iterable<TensorEntry>, Serializable {

    private static final Log LOG = LogFactory.getLog(SparseTensor.class);

    private static final long serialVersionUID = 2487513413901432943L;

    private class TensorIterator implements Iterator<TensorEntry> {

        private int index = 0;
        private SparseTensorEntry entry = new SparseTensorEntry();

        public boolean hasNext() {
            return index < values.size();
        }

        public TensorEntry next() {
            return entry.update(index++);
        }

        public void remove() {
            entry.remove();
        }

    }

    private class SparseTensorEntry implements TensorEntry {

        private int index = -1;

        public SparseTensorEntry update(int index) {
            this.index = index;
            return this;
        }

        public int key(int d) {
            return ndKeys[d].get(index);
        }

        public double get() {
            return values.get(index);
        }

        public void set(double value) {
            values.set(index, value);
        }

        /**
         * remove the current entry
         */
        public void remove() {
            for (int d = 0; d < numDimensions; d++) {

                // update indices if necessary
                if (isIndexed(d))
                    keyIndices[d].remove(key(d), index);

                ndKeys[d].remove(index);
            }
            values.remove(index);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int d = 0; d < numDimensions; d++) {
                sb.append(key(d)).append("\t");
            }
            sb.append(get());

            return sb.toString();
        }

        public int[] keys() {
            int[] res = new int[numDimensions];
            for (int d = 0; d < numDimensions; d++) {
                res[d] = key(d);
            }

            return res;
        }

    }

    /**
     * number of dimensions, i.e., the order (or modes, ways) of a tensor
     */
    public int numDimensions;
    public int[] dimensions;
    public List<Integer>[] ndKeys; // n-dimensional array
    public List<Double> values; // values

    private Multimap<Integer, Integer>[] keyIndices; // each multimap = {key, {pos1, pos2, ...}}
    private List<Integer> indexedDimensions; // indexed dimensions
    private Set<Integer> indexedDimensionsSet;// indexed dimensionsSet


    // dimensions of users and items
    private int userDimension, itemDimension;

    /**
     * Construct an empty sparse tensor
     *
     * @param dims dimensions of a tensor
     */
    public SparseTensor(int... dims) {
        this(dims, null, null);
    }

    /**
     * Construct a sparse tensor with indices and values
     *
     * @param dims dimensions of a tensor
     * @param nds  n-dimensional keys
     * @param vals entry values
     */
    @SuppressWarnings("unchecked")
    public SparseTensor(int[] dims, List<Integer>[] nds, List<Double> vals) {
        if (dims.length < 3)
            throw new Error("The dimension of a tensor cannot be smaller than 3!");

        numDimensions = dims.length;
        dimensions = new int[numDimensions];

        ndKeys = (List<Integer>[]) new List<?>[numDimensions];
        keyIndices = (Multimap<Integer, Integer>[]) new Multimap<?, ?>[numDimensions];

        for (int d = 0; d < numDimensions; d++) {
            dimensions[d] = dims[d];
            ndKeys[d] = nds == null ? new ArrayList<Integer>() : new ArrayList<Integer>(nds[d]);
            keyIndices[d] = HashMultimap.create();
            //System.out.println(keyIndices[d]);
        }

        values = vals == null ? new ArrayList<Double>() : new ArrayList<Double>(vals);
        indexedDimensions = new ArrayList<Integer>(numDimensions);
        indexedDimensionsSet = new HashSet<Integer>((int) (numDimensions/0.7));


    }

    /**
     * make a deep clone
     */
    public SparseTensor clone() {
        SparseTensor res = new SparseTensor(dimensions);

        // copy indices and values
        for (int d = 0; d < numDimensions; d++) {
            res.ndKeys[d].addAll(this.ndKeys[d]);
            res.keyIndices[d].putAll(this.keyIndices[d]);
        }

        res.values.addAll(this.values);

        // copy indexed array
        res.indexedDimensions.addAll(this.indexedDimensions);

        // others
        res.userDimension = userDimension;
        res.itemDimension = itemDimension;

        return res;
    }

    /**
     * Add a value to a given i-entry
     *
     * @param val  value to add
     * @param keys n-dimensional keys
     * @throws Exception if error occurs during adding
     */
    public void add(double val, int... keys) throws Exception {

        int index = findIndex(keys);

        if (index >= 0) {
            // if keys exist: update value
            values.set(index, values.get(index) + val);
        } else {
            // if keys do not exist: add a new entry
            set(val, keys);
        }
    }

    /**
     * Set a value to a specific i-entry
     *
     * @param val  value to set
     * @param keys n-dimensional keys
     * @throws Exception if error occurs during setting
     */
    public void set(double val, int... keys) throws Exception {
        int index = findIndex(keys);

        // if i-entry exists, set it a new value
        if (index >= 0) {
            values.set(index, val);
            return;
        }

        // otherwise insert a new entry
        for (int d = 0; d < numDimensions; d++) {
            ndKeys[d].add(keys[d]);

            // update indices if necessary
            if (isIndexed(d)) {
                keyIndices[d].put(keys[d], ndKeys[d].size() - 1);

                // other keys' indices do not change
            }
        }
        values.add(val);

    }

    /**
     * remove an entry with specific keys. NOTE: it is not recommended to remove by entry index because the index may be
     * changed after operations are executed, especially operation as addiction, remove, etc.
     *
     * @param keys  n-dimensional keys
     * @throws Exception if error occurs during removing
     * @return true if the operation succeed
     */
    public boolean remove(int... keys) throws Exception {
        int index = findIndex(keys);

        if (index < 0)
            return false;

        for (int d = 0; d < numDimensions; d++) {
            ndKeys[d].remove(index);

            // update indices if necessary
            if (isIndexed(d)) {
                buildIndex(d); // re-build indices
            }
        }
        values.remove(index);

        return true;
    }

    /**
     * Return all entries for a (user, item) pair
     *
     * @param user user index
     * @param item item index
     * @return all entries for a (user, item) pair
     */
    public List<Integer> getIndices(int user, int item) {
        List<Integer> res = new ArrayList<Integer>();

        Collection<Integer> indices = getIndex(userDimension, user);
        for (int index : indices) {
            if (key(itemDimension, index) == item) {
                res.add(index);
            }
        }

        return res;
    }


    /**
     * @return all entries for a numDimensions-1 dimension subKey
     *
     * @param subKey a numDimensions-1 dimension subKey
     * @throws Exception if error occurs during getting
     */
    public List<Integer> getTargetKeyFromSubKey(Integer[] subKey) throws Exception {
        List<Integer> res = new ArrayList<Integer>();
        if (subKey.length != numDimensions - 1)
            throw new Exception("The given input does not match with the subKey dimension!");
        // if no data exists
        if (values.size() == 0)
            return null;

        // if no indexed dimension exists
        if (indexedDimensions.size() == 0)
            buildIndex(0);

        // retrieve from the first indexed dimension
        int d = indexedDimensions.get(0);

        // all relevant positions
        Collection<Integer> indices = keyIndices[d].get(subKey[d]);
        if (indices == null || indices.size() == 0)
            return null;

        // for each possible position
        for (int index : indices) {
            boolean found = true;
            for (int dd = 0; dd < numDimensions - 1; dd++) {
                if (subKey[dd] != key(dd, index)) {
                    found = false;
                    break;
                }
            }
            if (found)
                res.add(ndKeys[numDimensions - 1].get(index));
        }

        return res;
    }


    /**
     * find the inner index of a given keys
     *
     * @param keys a given keys
     * @throws Exception if error occurs during finding
     */
    private int findIndex(int... keys) throws Exception {

        if (keys.length != numDimensions)
            throw new Exception("The given input does not match with the tensor dimension!");

        // if no data exists
        if (values.size() == 0)
            return -1;

        // if no indexed dimension exists
        if (indexedDimensions.size() == 0)
            buildIndex(0);

        // retrieve from the first indexed dimension
        int d = indexedDimensions.get(0);

        // all relevant positions
        Collection<Integer> indices = keyIndices[d].get(keys[d]);
        if (indices == null || indices.size() == 0)
            return -1;

        // for each possible position
        for (int index : indices) {
            boolean found = true;
            for (int dd = 0; dd < numDimensions; dd++) {
                if (keys[dd] != key(dd, index)) {
                    found = false;
                    break;
                }
            }
            if (found)
                return index;
        }

        // if not found
        return -1;

    }

    /**
     * A fiber is defined by fixing every index but one. For example, a matrix column is a mode-1 fiber and a matrix row
     * is a mode-2 fiber.
     *
     * @param dim  the dimension where values can vary
     * @param keys the other fixed dimension keys
     * @return a sparse vector
     */
    public SparseVector fiber(int dim, int... keys) {
        if ((keys.length != numDimensions - 1) || size() < 1)
            throw new Error("The input indices do not match the fiber specification!");

        // find an indexed dimension for searching indices
        int d = -1;
        if ((indexedDimensions.size() == 0) || (indexedDimensions.contains(dim) && indexedDimensions.size() == 1)) {
            d = (dim != 0 ? 0 : 1);
            buildIndex(d);
        } else {
            for (int dd : indexedDimensions) {
                if (dd != dim) {
                    d = dd;
                    break;
                }
            }
        }

        SparseVector res = new SparseVector(dimensions[dim]);

        // all relevant positions
        Collection<Integer> indices = keyIndices[d].get(keys[d < dim ? d : d - 1]);
        if (indices == null || indices.size() == 0)
            return res;

        // for each possible position
        for (int index : indices) {
            boolean found = true;
            for (int dd = 0, ndi = 0; dd < numDimensions; dd++) {

                if (dd == dim)
                    continue;

                if (keys[ndi++] != key(dd, index)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                res.set(key(dim, index), value(index));
            }
        }

        return res;
    }

    /**
     * Check if a given keys exists
     *
     * @param keys keys to check
     * @return true if found, and false otherwise
     * @throws Exception if error occurs during checking
     */
    public boolean contains(int... keys) throws Exception {
        return findIndex(keys) >= 0 ? true : false;
    }

    /**
     * Return whether a dimension d is indexed.
     *
     * @param d a dimension d
     * @return whether a dimension d is indexed
     */
    public boolean isIndexed(int d) {
        return indexedDimensions.contains(d);
    }

    /**
     * @return whether a tensor is cubical
     */
    public boolean isCubical() {
        int dim = dimensions[0];
        for (int d = 1; d < numDimensions; d++) {
            if (dim != dimensions[d])
                return false;
        }

        return true;
    }

    /**
     * @return whether a tensor is diagonal
     */
    public boolean isDiagonal() {
        for (TensorEntry te : this) {
            double val = te.get();
            if (val != 0) {
                int i = te.key(0);
                for (int d = 0; d < numDimensions; d++) {
                    int j = te.key(d);
                    if (i != j)
                        return false;
                }
            }
        }

        return true;
    }

    /**
     * Return a value given a specific i-entry.
     *
     * @param keys a specific i-entry
     * @return a value given a specific i-entry
     * @throws Exception if error occurs during getting
     */
    public double get(int... keys) throws Exception {
        assert keys.length == this.numDimensions;

        int index = findIndex(keys);
        return index < 0 ? 0 : values.get(index);
    }

    /**
     * Shuffle a sparse tensor
     */
    public void shuffle() {
        int len = size();
        for (int i = 0; i < len; i++) {
            // target index
            int j = i + Randoms.uniform(len - i);

            // swap values
            double temp = values.get(i);
            values.set(i, values.get(j));
            values.set(j, temp);

            // swap keys
            for (int d = 0; d < numDimensions; d++) {
                int ikey = key(d, i);
                int jkey = key(d, j);
                ndKeys[d].set(i, jkey);
                ndKeys[d].set(j, ikey);

                // update indices
                if (isIndexed(d)) {
                    keyIndices[d].remove(jkey, j);
                    keyIndices[d].put(jkey, i);

                    keyIndices[d].remove(ikey, i);
                    keyIndices[d].put(ikey, j);
                }

            }

        }
    }

    /**
     * build index at dimensions nd
     *
     * @param dims dimensions to be indexed
     */
    public void buildIndex(int... dims) {
        for (int d : dims) {
            keyIndices[d].clear();
            for (int index = 0; index < ndKeys[d].size(); index++) {
                keyIndices[d].put(key(d, index), index);
            }

            if (!indexedDimensions.contains(d))
                indexedDimensions.add(d);
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
     * Return indices (positions) of a key in dimension d.
     *
     * @param d   dimension
     * @param key key value
     * @return indices (positions) of a key in dimension d
     */
    public Collection<Integer> getIndex(int d, int key) {
        if (!isIndexed(d))
            buildIndex(d);

        return keyIndices[d].get(key);
    }

    /**
     * Return keys in a given index
     *
     * @param index a given index
     * @return keys in a given index
     */
    public int[] keys(int index) {
        int[] res = new int[numDimensions];
        for (int d = 0; d < numDimensions; d++) {
            res[d] = key(d, index);
        }

        return res;
    }

    /**
     * Return key in the position {@code index} of dimension {@code d}.
     *
     * @param d      dimension
     * @param index  a given index
     * @return key in the position {@code index} of dimension {@code d}
     */
    public int key(int d, int index) {
        return ndKeys[d].get(index);
    }

    /**
     * Return value in a given index.
     *
     * @param index a given index
     * @return value in a given index
     */
    public double value(int index) {
        return values.get(index);
    }

    /**
     * Return keys in a target dimension {@code td} related with a key in dimension {@code sd}.
     *
     * @param sd  source dimension
     * @param key key in the source dimension
     * @param td  target dimension
     * @return keys in a target dimension {@code td} related with a key in dimension {@code sd}
     */
    public List<Integer> getRelevantKeys(int sd, int key, int td) {
        Collection<Integer> indices = getIndex(sd, key);
        List<Integer> res = null;
        if (indices != null) {
            res = new ArrayList<Integer>();
            for (int index : indices) {
                res.add(key(td, index));
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
     * Slice is a two-dimensional sub-array of a tensor, defined by fixing all but two indices.
     *
     * @param rowDim    row dimension
     * @param colDim    column dimension
     * @param otherKeys keys of other dimensions
     * @return a sparse matrix
     */
    public SparseMatrix slice(int rowDim, int colDim, int... otherKeys) {

        if (otherKeys.length != numDimensions - 2)
            throw new Error("The input dimensions do not match the tensor specification!");

        // find an indexed array to search
        int d = -1;
        boolean cond1 = indexedDimensions.size() == 0;
        boolean cond2 = (indexedDimensions.contains(rowDim) || indexedDimensions.contains(colDim))
                && indexedDimensions.size() == 1;
        boolean cond3 = indexedDimensions.contains(rowDim) && indexedDimensions.contains(colDim)
                && indexedDimensions.size() == 2;
        if (cond1 || cond2 || cond3) {
            for (d = 0; d < numDimensions; d++) {
                if (d != rowDim && d != colDim)
                    break;
            }
            buildIndex(d);
        } else {
            for (int dd : indexedDimensions) {
                if (dd != rowDim && dd != colDim) {
                    d = dd;
                    break;
                }
            }
        }

        // get search key
        int key = -1;
        for (int dim = 0, i = 0; dim < numDimensions; dim++) {
            if (dim == rowDim || dim == colDim)
                continue;

            if (dim == d) {
                key = otherKeys[i];
                break;
            }
            i++;
        }

        // all relevant positions
        Collection<Integer> indices = keyIndices[d].get(key);
        if (indices == null || indices.size() == 0)
            return null;

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        // for each possible position
        for (int index : indices) {
            boolean found = true;
            for (int dd = 0, j = 0; dd < numDimensions; dd++) {

                if (dd == rowDim || dd == colDim)
                    continue;

                if (otherKeys[j++] != key(dd, index)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                int row = ndKeys[rowDim].get(index);
                int col = ndKeys[colDim].get(index);
                double val = values.get(index);

                dataTable.put(row, col, val);
                colMap.put(col, row);
            }
        }

        return new SparseMatrix(dimensions[rowDim], dimensions[colDim], dataTable, colMap);
    }

    /**
     * Re-ordering entries of a tensor into a matrix
     *
     * @param n mode or dimension
     * @return an unfolded or flatten matrix
     */
    public SparseMatrix matricization(int n) {
        int numRows = dimensions[n];
        int numCols = 1;
        for (int d = 0; d < numDimensions; d++) {
            if (d != n)
                numCols *= dimensions[d];
        }

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();
        for (TensorEntry te : this) {
            int[] keys = te.keys();

            int i = keys[n];
            int j = 0;
            for (int k = 0; k < numDimensions; k++) {
                if (k == n)
                    continue;

                int ik = keys[k];
                int jk = 1;
                for (int m = 0; m < k; m++) {
                    if (m == n)
                        continue;
                    jk *= dimensions[m];
                }

                j += ik * jk;
            }

            dataTable.put(i, j, te.get());
            colMap.put(j, i);
        }

        return new SparseMatrix(numRows, numCols, dataTable, colMap);
    }

    /**
     * n-mode product of a tensor A (I1 x I2 x ... x IN) with a matrix B (J x In), denoted by A Xn B
     *
     * @param mat mat to be multiplied
     * @param dim mode/dimension of the tensor to be used
     * @return a new tensor in (I1 x I2 x ... x In-1 x J x In+1 x ... x IN)
     * @throws Exception if error occurs during product
     */
    public SparseTensor modeProduct(DenseMatrix mat, int dim) throws Exception {

        if (dimensions[dim] != mat.numColumns)
            throw new Exception("Dimensions of a tensor and a matrix do not match for n-mode product!");

        int[] dims = new int[numDimensions];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = i == dim ? mat.numRows : dimensions[i];
        }

        SparseTensor res = new SparseTensor(dims);

        for (TensorEntry te : this) {
            double val = te.get();
            int[] keys = te.keys();

            int i = keys[dim];
            for (int j = 0; j < mat.numRows; j++) {

                int[] ks = new int[numDimensions];
                for (int k = 0; k < ks.length; k++)
                    ks[k] = k == dim ? j : keys[k];

                res.add(val * mat.get(j, i), ks);
            }
        }

        return res;
    }

    /**
     * n-mode product of a tensor A (I1 x I2 x ... x IN) with a vector B (1 x In), denoted by A Xn B
     *
     * @param vec vector to be multiplied
     * @param dim mode/dimension of the tensor to be used
     * @return a new tensor in (I1 x I2 x ... x In-1 x 1 x In+1 x ... x IN)
     * @throws Exception if error occurs during product
     */
    public SparseTensor modeProduct(DenseVector vec, int dim) throws Exception {

        if (dimensions[dim] != vec.size)
            throw new Exception("Dimensions of a tensor and a vector do not match for n-mode product!");

        int[] dims = new int[numDimensions];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = i == dim ? 1 : dimensions[i];
        }

        SparseTensor res = new SparseTensor(dims);

        for (TensorEntry te : this) {
            double val = te.get();
            int[] keys = te.keys();

            int i = keys[dim];

            int[] ks = new int[numDimensions];
            for (int k = 0; k < ks.length; k++)
                ks[k] = k == dim ? 1 : keys[k];

            res.add(val * vec.get(i), ks);
        }

        return res;
    }

    /**
     * retrieve a rating matrix from the tensor. Warning: it assumes there is at most one entry for each (user, item)
     * pair.
     *
     * @return a sparse rating matrix
     */
    public SparseMatrix rateMatrix() {

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for (TensorEntry te : this) {
            int u = te.key(userDimension);
            int i = te.key(itemDimension);

            dataTable.put(u, i, te.get());
            colMap.put(i, u);
        }

        return new SparseMatrix(dimensions[userDimension], dimensions[itemDimension], dataTable, colMap);
    }

    public Iterator<TensorEntry> iterator() {
        return new TensorIterator();
    }

    /**
     * @return norm of a tensor
     */
    public double norm() {
        double res = 0;

        for (double val : values) {
            res += val * val;
        }

        return Math.sqrt(res);
    }

    /**
     * @return mean of a tensor
     */
    public double mean() {
        double res = 0;

        for (double val : values) {
            res += val;
        }

        return res / size();
    }

    /**
     * @param st  another tensor
     * @return inner product with another tensor
     * @throws Exception if error occurs during product
     */
    public double innerProduct(SparseTensor st) throws Exception {
        if (!isDimMatch(st))
            throw new Exception("The dimensions of two sparse tensors do not match!");

        double res = 0;
        for (TensorEntry te : this) {
            double v1 = te.get();
            double v2 = st.get(te.keys());

            res += v1 * v2;
        }

        return res;
    }

    /**
     * Return whether two sparse tensors have the same dimensions
     *
     * @param st a given SparseTensor
     * @return whether two sparse tensors have the same dimensions
     */
    public boolean isDimMatch(SparseTensor st) {
        if (numDimensions != st.numDimensions)
            return false;

        boolean match = true;
        for (int d = 0; d < numDimensions; d++) {
            if (dimensions[d] != st.dimensions[d]) {
                match = false;
                break;
            }
        }

        return match;
    }

    public int getUserDimension() {
        return userDimension;
    }

    public int getIndexDimension(int index) {
        assert index < numDimensions;

        return dimensions[index];
    }

    public void setUserDimension(int userDimension) {
        this.userDimension = userDimension;
    }

    public int getItemDimension() {
        return itemDimension;
    }

    public void setItemDimension(int itemDimension) {
        this.itemDimension = itemDimension;
    }

    public int[] dimensions() {
        return dimensions;
    }

    /**
     * @return the number of a tensor's dimensions, a.k.a., the order of a tensor
     */
    public int numDimensions() {
        return numDimensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("N-Dimension: ").append(numDimensions).append(", Size: ").append(size()).append("\n");
        for (int index = 0; index < values.size(); index++) {
            for (int d = 0; d < numDimensions; d++) {
                sb.append(key(d, index)).append("\t");
            }
            sb.append(value(index)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Usage demonstration
     *
     * @param args arguments
     * @throws Exception if error occurs
     */
    public static void main(String[] args) throws Exception {
        int[] a = {2, 2, 2};
        List<Integer>[] ndLists = null;
        ndLists = (List<Integer>[]) new List<?>[3];
        for (int d = 0; d < 3; d++) {
            ndLists[d] = new ArrayList<Integer>();
        }
        List<Double> vals = new ArrayList<Double>();
        ndLists[0].add(1);
        ndLists[0].add(2);
        ndLists[0].add(3);
        ndLists[1].add(1);
        ndLists[1].add(2);
        ndLists[1].add(4);
        ndLists[2].add(1);
        ndLists[2].add(2);
        ndLists[2].add(5);
        vals.add(1.0);
        vals.add(2.0);
        vals.add(3.0);


        SparseTensor st1 = new SparseTensor(a, ndLists, vals);
        LOG.debug(st1);

        LOG.debug(String.format("Index of keys (1, 2) = {}", st1.getTargetKeyFromSubKey(new Integer[]{3, 4})));


        SparseTensor st = new SparseTensor(4, 4, 6);
        LOG.debug(st);
        st.set(1.0, 1, 0, 0);
        st.set(1.5, 1, 0, 0); // overwrite value
        st.set(2.0, 1, 1, 0);
        st.set(3.0, 2, 0, 0);
        st.set(4.0, 1, 3, 0);
        st.set(5.0, 1, 0, 5);
        st.set(6.0, 3, 1, 4);

        LOG.debug(st);
        LOG.debug(String.format("Keys (1, 0, 0) = {}", st.get(1, 0, 0)));
        LOG.debug(String.format("Keys (1, 1, 0) = {}", st.get(1, 1, 0)));
        LOG.debug(String.format("Keys (1, 2, 0) = {}", st.get(1, 2, 0)));
        LOG.debug(String.format("Keys (2, 0, 0) = {}", st.get(2, 0, 0)));
        LOG.debug(String.format("Keys (1, 0, 6) = {}", st.get(1, 0, 6)));
        LOG.debug(String.format("Keys (3, 1, 4) = {}", st.get(3, 1, 4)));

        LOG.debug(String.format("Index of dimension 0 key 1 = {}", st.getIndex(0, 1)));
        LOG.debug(String.format("Index of dimension 1 key 3 = {}", st.getIndex(1, 3)));
        LOG.debug(String.format("Index of dimension 2 key 1 = {}", st.getIndex(2, 1)));
        LOG.debug(String.format("Index of dimension 2 key 6 = {}", st.getIndex(2, 6)));

        st.set(4.5, 2, 1, 1);
        LOG.debug(st);
        LOG.debug(String.format("Index of dimension 2 key 1 = {}", st.getIndex(2, 1)));
        st.remove(2, 1, 1);
        LOG.debug(String.format("Index of dimension 2 key 1 = {}", st.getIndex(2, 1)));


        LOG.debug(String.format("Index of keys (1, 2, 0) = {}, value = {}", st.findIndex(1, 2, 0), st.get(1, 2, 0)));

        LOG.debug(String.format("Index of keys (3, 1, 4) = {}, value = {}", st.findIndex(3, 1, 4), st.get(3, 1, 4)));

        LOG.debug(String.format("Keys in dimension 2 associated with dimension 0 key 1 = {}", st.getRelevantKeys(0, 1, 2)));

        // norm
        LOG.debug(String.format("norm = {}", st.norm()));

        // clone
        SparseTensor st2 = st.clone();
        LOG.debug(String.format("make a clone = {}", st2));

        // inner product
        LOG.debug(String.format("inner with the clone = {}", st.innerProduct(st2)));

        st.set(2.5, 1, 0, 0);
        st2.remove(1, 0, 0);
        LOG.debug(String.format("st1 = {}", st));
        LOG.debug(String.format("st2 = {}", st2));

        // fiber
        LOG.debug(st);
        int[] aa = {0, 0};
        LOG.debug(String.format("fiber (0, 0, 0) = {}", st.fiber(0, aa).getIndex()));
        LOG.debug(String.format("fiber (1, 1, 0) = {}", st.fiber(1, 1, 0)));
        LOG.debug(String.format("fiber (2, 1, 0) = {}", st.fiber(2, 1, 0)));

        // slice
        LOG.debug(String.format("slice (0, 1, 0) = {}", st.slice(0, 1, 0)));
        LOG.debug(String.format("slice (0, 2, 1) = {}", st.slice(0, 2, 1)));
        LOG.debug(String.format("slice (1, 2, 1) = {}", st.slice(1, 2, 1)));

        // iterator
        for (TensorEntry te : st) {
            te.set(te.get() + 0.588);
        }
        LOG.debug(String.format("Before shuffle: {}", st));

        // shuffle
        st.shuffle();
        LOG.debug(String.format("After shuffle: {}", st));

        // new tensor: example given by the reference paper in (2.1)
        st = new SparseTensor(3, 4, 2);

        st.set(1, 0, 0, 0);
        st.set(4, 0, 1, 0);
        st.set(7, 0, 2, 0);
        st.set(10, 0, 3, 0);
        st.set(2, 1, 0, 0);
        st.set(5, 1, 1, 0);
        st.set(8, 1, 2, 0);
        st.set(11, 1, 3, 0);
        st.set(3, 2, 0, 0);
        st.set(6, 2, 1, 0);
        st.set(9, 2, 2, 0);
        st.set(12, 2, 3, 0);

        st.set(13, 0, 0, 1);
        st.set(16, 0, 1, 1);
        st.set(19, 0, 2, 1);
        st.set(22, 0, 3, 1);
        st.set(14, 1, 0, 1);
        st.set(17, 1, 1, 1);
        st.set(20, 1, 2, 1);
        st.set(23, 1, 3, 1);
        st.set(15, 2, 0, 1);
        st.set(18, 2, 1, 1);
        st.set(21, 2, 2, 1);
        st.set(24, 2, 3, 1);

        LOG.debug(String.format("A new tensor = {}", st));
        LOG.debug(String.format("Mode X0 unfoldings = {}", st.matricization(0)));
        LOG.debug(String.format("Mode X1 unfoldings = {}", st.matricization(1)));
        LOG.debug(String.format("Mode X2 unfoldings = {}", st.matricization(2)));
    }

}
