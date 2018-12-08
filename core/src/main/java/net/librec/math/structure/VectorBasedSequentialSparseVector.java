/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a clone of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.librec.math.structure;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.librec.common.IndexException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * sequential access sparse vector
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class VectorBasedSequentialSparseVector extends SequentialSparseVector {
    private static final Log LOG = LogFactory.getLog(AbstractVector.class);

    private OrderedIntDoubleMapping values;

    /**
     * For serialization purposes only.
     */
    public VectorBasedSequentialSparseVector(int cardinality) {
        super(cardinality);
        values = new OrderedIntDoubleMapping();
    }

    public VectorBasedSequentialSparseVector(int cardinality, int numActivesElement) {
        super(cardinality);
        values = new OrderedIntDoubleMapping(numActivesElement);
    }

    public VectorBasedSequentialSparseVector(Vector other) {
        this(other.cardinality(), other.getNumEntries());

        if (other.isSequentialAccess()) {
            int position = 0;
            for (VectorEntry e : other) {
                values.setIndexAt(position, e.index());
                values.setValueAt(position, e.get());
                position++;
            }
        } else {
            // If the incoming Vector to clone is random, then adding items
            // from the Iterator can degrade performance dramatically if
            // the number of elements is large as this Vector tries to stay
            // in order as items are added, so it's better to sort the other
            // Vector's elements by index and then plus them to this
            copySortedRandomAccessSparseVector(other);
        }
    }

    public VectorBasedSequentialSparseVector(int cardinality, Map<Integer, ? extends Number> mapVector) {
        this(cardinality, mapVector.size());
        int entryCount = mapVector.size();
        OrderedVectorElement[] sortableEntries = new OrderedVectorElement[entryCount];
        int elementIndex = 0;
        for (Map.Entry<Integer, ? extends Number> mapEntry : mapVector.entrySet()) {
            sortableEntries[elementIndex++] = new OrderedVectorElement(mapEntry.getKey(), mapEntry.getValue().doubleValue());
        }

        Arrays.sort(sortableEntries);
        int[] indices = new int[sortableEntries.length];
        double[] doubleValues = new double[sortableEntries.length];
        for (int index = 0; index < sortableEntries.length; index++) {
            indices[index] = sortableEntries[index].getIndex();
            doubleValues[index] = sortableEntries[index].getValue();
        }
        values = new OrderedIntDoubleMapping(indices, doubleValues);
    }

    /**
     * Construct the VectorBasedSequentialSparseVector by a int indices and double values.
     *
     * @param cardinality
     * @param indices
     * @param doubleValues
     */
    public VectorBasedSequentialSparseVector(int cardinality, IntArrayList indices, DoubleArrayList doubleValues) {
        this(cardinality, indices.size());
        int entryCount = indices.size();
        OrderedVectorElement[] sortableEntries = new OrderedVectorElement[entryCount];
        int elementIndex = 0;
        for (int index = 0; index < entryCount; index++) {
            sortableEntries[elementIndex++] = new OrderedVectorElement(indices.getInt(index), doubleValues.getDouble(index));
        }

        Arrays.sort(sortableEntries);
        int[] tmpIndices = new int[sortableEntries.length];
        double[] tmpDoubleValues = new double[sortableEntries.length];
        for (int index = 0; index < sortableEntries.length; index++) {
            tmpIndices[index] = sortableEntries[index].getIndex();
            tmpDoubleValues[index] = sortableEntries[index].getValue();
        }
        values = new OrderedIntDoubleMapping(tmpIndices, tmpDoubleValues);
    }

    public VectorBasedSequentialSparseVector(VectorBasedSequentialSparseVector other,
                                             boolean shallowIndicesCopy){
        this(other.cardinality());
        int[] indices = shallowIndicesCopy ? other.values.getIndices(): other.values.getIndices().clone();
        double[] doublesValues = other.values.getValues().clone();
        values = new OrderedIntDoubleMapping(indices, doublesValues);
    }


    public VectorBasedSequentialSparseVector(VectorBasedSequentialSparseVector other) {
        this(other.cardinality());
        values = other.values.clone();
    }

    private VectorBasedSequentialSparseVector(int cardinality, OrderedIntDoubleMapping values) {
        this(cardinality);
        this.values = values;
    }

    public VectorBasedSequentialSparseVector(int cardinality, int[] indices, double[] values) {
        this(cardinality);
        this.values = new OrderedIntDoubleMapping(indices, values);
    }

    // Sorts a RandomAccessSparseVectors Elements before adding them to this
    private void copySortedRandomAccessSparseVector(Vector other) {
        int entryCount = other.getNumEntries();
        OrderedVectorElement[] sortableEntries = new OrderedVectorElement[entryCount];
        int elementIndex = 0;
        for (VectorEntry vectorEntry : other) {
            sortableEntries[elementIndex++] = new OrderedVectorElement(vectorEntry.index(), vectorEntry.get());
        }

        if (!other.isSequentialAccess()) {
            Arrays.sort(sortableEntries);
        }

        for (int sortIndex = 0; sortIndex < sortableEntries.length; sortIndex++) {
            values.setIndexAt(sortIndex, sortableEntries[sortIndex].getIndex());
            values.setValueAt(sortIndex, sortableEntries[sortIndex].getValue());
        }
    }

    @Override
    public VectorBasedSequentialSparseVector clone() {
        return new VectorBasedSequentialSparseVector(cardinality(), values.clone());
    }

    protected void reshape() {
        values.reshape();
    }

//    public void mergeUpdates(OrderedIntDoubleMapping updates) {
//        values.merge(updates);
//    }
//
//    public void mergeUpdates(Int2DoubleOpenHashMap updates) {
//        //TODO the mergeUpdates for Int2DoubleOpenHashMap
//    }

    public int[] getIndices() {
        return values.getIndices();
    }

    @Override
    public String toString() {
        return sparseVectorToString();
    }


    @Deprecated
    @Override
    public VectorEntry getVectorEntry(int index) {
//        LOG.warn("We don't suggest you to use random access in SequentialAccessSparseVector for the time efficiency.\n" +
//                "If you want to use random access operation in sparse vector, please use RandomAccessSparseVector.\n" +
//                "If you want to use SequentialAccessSparseVector, please use the sequential access method, " +
//                "such as getVectorEntryAtPosition.");
        return new LocalVectorEntry(index);
    }

    /**
     * Return an object of Vector.VectorEntry representing an entry of this Vector. Useful when designing new iterator
     * types.
     *
     * @param position position of the Vector.VectorEntry required. When it is used as a SequentialAccessSparseVector,
     *                 the position is different from index. While for other vector type,
     *                 the position is the same as the index.
     * @return The Vector.Element Object
     */
    public VectorEntry getVectorEntryAtPosition(int position) {
        SparseVectorEntry vectorEntry = new SparseVectorEntry();
        vectorEntry.position = position;
        return vectorEntry;
    }

    @Deprecated
    @Override
    public void set(int index, double value) {
//        LOG.warn("We don't suggest you to use random access in SequentialAccessSparseVector for the time efficiency.\n" +
//                "If you want to use random access operation in sparse vector, please use RandomAccessSparseVector.\n" +
//                "If you want to use SequentialAccessSparseVector, please use the sequential access method, " +
//                "such as setAtPosition.");
        values.set(index, value);
    }

    @Deprecated
    @Override
    public double get(int index) {
//        LOG.warn("We don't suggest you to use random access in SequentialAccessSparseVector for the time efficiency.\n" +
//                "If you want to use random access operation in sparse vector, please use RandomAccessSparseVector.\n" +
//                "If you want to use SequentialAccessSparseVector, please use the sequential access method, " +
//                "such as getAtPosition.");
        return values.get(index);
    }

    /**
     * Set the value at the given index, without checking bounds
     * Noted that for the DenseVector or RandomAccessSparseVector(Random Access Vector),
     * the position is as the same the index
     *
     * @param position an int position into the receiver
     * @param value    a double value to set
     * @throws IndexException if the index is out of bounds
     */
    public void setAtPosition(int position, double value) {
        values.setValueAt(position, value);
    }

    /**
     * Return the value at the given index, without checking bounds
     * Noted that for the DenseVector or RandomAccessSparseVector(Random Access Vector),
     * the position is as the same the index
     *
     * @param position an int position
     * @return the double at the position
     * @throws IndexException if the index is out of bounds
     */
    public double getAtPosition(int position) {
        return values.getValueAt(position);
    }

    public int getIndexAtPosition(int position) {
        return values.indexAt(position);
    }


    @Override
    public int getNumEntries() {
        return values.getNumMappings();
    }


    @Override
    public Iterator<VectorEntry> iterator() {
        return new SequentialAccessIterator();
    }

    private final class SequentialAccessIterator implements Iterator<VectorEntry> {
        private final SparseVectorEntry vectorEntry = new SparseVectorEntry();

        @Override
        public boolean hasNext() {
            return vectorEntry.getNextOffset() < values.getNumMappings();
        }

        @Override
        public SparseVectorEntry next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            vectorEntry.advanceOffset();
            return vectorEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public final class SparseVectorEntry implements VectorEntry {
        private int position = -1;

        void advanceOffset() {
            position++;
        }

        int getNextOffset() {
            return position + 1;
        }

        @Override
        public double get() {
            return values.getValues()[position];
        }

        @Override
        public int index() {
            return values.getIndices()[position];
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
            values.setValueAt(position, value);
        }
    }
}
