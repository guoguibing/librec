package net.librec.math.structure;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Data Structure: Ordered Int Double Mapping whose implementation is modified from mahout math library.
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public final class OrderedIntDoubleMapping implements Serializable, Cloneable {

    private static final double DEFAULT_VALUE = 0.0;

    /**
     * Indices to mapping data
     */
    private int[] indices;

    /**
     * mapping data values
     */
    private double[] values;

    private boolean noDefault = true;

    /**
     * number of mapping data values
     */
    private int numMappings;

    /**
     * no-arg constructor for deserializer
     */
    OrderedIntDoubleMapping() {
        // no-arg constructor for deserializer
        this(0);
    }

    /**
     * constructor by giving de
     *
     * @param noDefault
     */
    OrderedIntDoubleMapping(boolean noDefault) {
        this();
        this.noDefault = noDefault;
    }

    OrderedIntDoubleMapping(int capacity) {
        indices = new int[capacity];
        values = new double[capacity];
        numMappings = 0;
    }

    /**
     * Construct a Ordered Int Double Mapping
     * indices to data, and data
     *
     * @param indices     Indices to mapping data
     * @param values      mapping data values
     */
    OrderedIntDoubleMapping(int[] indices, double[] values) {
        this.indices = indices;
        this.values = values;
        this.numMappings = values.length;
    }

    /**
     * Construct a Ordered Int Double Mapping by Int2DoubleOpenHashMap
     */
    OrderedIntDoubleMapping(Int2DoubleOpenHashMap map) {
        this.numMappings = map.size();
        OrderedVectorElement[] sortableElements = this.getOrderedVectorElements(map);

        for (int i = 0; i < numMappings; i++) {
            this.indices[i] = sortableElements[i].getIndex();
            this.values[i] = sortableElements[i].getValue();
        }
    }

    /**
     * @return the indices
     */
    public int[] getIndices() {
        return indices;
    }

    /**
     * @return the values
     */
    public double[] getValues() {
        return values;
    }


    /**
     * Return the mapping data index at the given position of indices array
     *
     * @param position an position of indices
     * @return the mapping data index at the given position of indices array
     */
    public int indexAt(int position) {
        return indices[position];
    }

    /**
     * Set the mapping index at the given position of the indices array
     *
     * @param position an int position of the indices array
     * @param index    an int mapping index
     */
    public void setIndexAt(int position, int index) {
        indices[position] = index;
    }

    /**
     * Set the value at the given position of the values array
     *
     * @param position an int position of the values array
     * @param value    the double value at the given position of the values array
     */
    public void setValueAt(int position, double value) {
        values[position] = value;
    }

    /**
     * Return the value at the given position of the values array
     *
     * @param position an int position of the values array
     * @return the double value at the given position of the values array
     */
    public double getValueAt(int position) {
        return values[position];
    }

    /**
     * Return the number of mapping data = values.length = indices.length
     *
     * @return the number of mapping
     */
    public int getNumMappings() {
        return numMappings;
    }


    /**
     * grow the capacity if there are not enough space to store data
     *
     * @param newCapacity new capacity
     */
    private void growTo(int newCapacity) {
        if (newCapacity > indices.length) {
            int[] newIndices = new int[newCapacity];
            System.arraycopy(indices, 0, newIndices, 0, numMappings);
            indices = newIndices;
            double[] newValues = new double[newCapacity];
            System.arraycopy(values, 0, newValues, 0, numMappings);
            values = newValues;
        }
    }

    /**
     * Return the offset index at the mapping index
     * The time cost is O(log numMapping), so it is not suggested to use
     *
     * @param index a mapping index of value
     * @return the position at the mapping index
     * @deprecated because of the time cost
     */
    @Deprecated
    private int find(int index) {
        int low = 0;
        int high = getNumMappings() - 1;
        while (low <= high) {
            int mid = low + (high - low >>> 1);
            int midVal = indices[mid];
            if (midVal < index) {
                low = mid + 1;
            } else if (midVal > index) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    /**
     * Return the value at given mapping index
     * The time cost is O(log numMapping), so it is not suggested to use
     *
     * @param index a mapping index of value
     * @return the value at the mapping index
     * @deprecated because of the time cost
     */
    @Deprecated
    public double get(int index) {
        int offset = find(index);
        return offset >= 0 ? values[offset] : DEFAULT_VALUE;
    }

    /**
     * Set the given value at the given index
     *
     * @param index a int index of values
     * @param value a double value to be set
     * @deprecated because of the time cost
     */
    @Deprecated
    public void set(int index, double value) {
        if (numMappings == 0 || index > indices[numMappings - 1]) {
            if (!noDefault || value != DEFAULT_VALUE) {
                if (numMappings >= indices.length) {
                    growTo(Math.max((int) (1.2 * numMappings), numMappings + 1));
                }
                indices[numMappings] = index;
                values[numMappings] = value;
                ++numMappings;
            }
        } else {
            int offset = find(index);
            if (offset >= 0) {
                insertOrUpdateValueIfPresent(offset, value);
            } else {
                insertValueIfNotDefault(index, offset, value);
            }
        }
    }

    /**
     * Merges the updates in linear time by allocating new arrays and iterating through the existing indices and values
     * and the updates' indices and values at the same time while selecting the minimum index to set at each step.
     *
     * @param updates another list of ordered int double mappings to be merged in.
     */
    public void merge(OrderedIntDoubleMapping updates) {
        int[] updateIndices = updates.getIndices();
        double[] updateValues = updates.getValues();

        int newNumMappings = numMappings + updates.getNumMappings();
        int newCapacity = Math.max((int) (1.1 * newNumMappings), newNumMappings + 1);
        int[] newIndices = new int[newCapacity];
        double[] newValues = new double[newCapacity];

        int k = 0;
        int i = 0, j = 0;
        for (; i < numMappings && j < updates.getNumMappings(); ++k) {
            if (indices[i] < updateIndices[j]) {
                newIndices[k] = indices[i];
                newValues[k] = values[i];
                ++i;
            } else if (indices[i] > updateIndices[j]) {
                newIndices[k] = updateIndices[j];
                newValues[k] = updateValues[j];
                ++j;
            } else {
                newIndices[k] = updateIndices[j];
                newValues[k] = updateValues[j];
                ++i;
                ++j;
            }
        }

        for (; i < numMappings; ++i, ++k) {
            newIndices[k] = indices[i];
            newValues[k] = values[i];
        }
        for (; j < updates.getNumMappings(); ++j, ++k) {
            newIndices[k] = updateIndices[j];
            newValues[k] = updateValues[j];
        }

        indices = newIndices;
        values = newValues;
        numMappings = k;
    }


    /**
     * Merges the updates in linear time by allocating new arrays and iterating through the existing indices and values
     * and the updates' indices and values at the same time while selecting the minimum index to set at each step.
     *
     * @param updatesMap another list of int double hash mappings to be merged in.
     */
    public void merge(Int2DoubleOpenHashMap updatesMap) {
        OrderedVectorElement[] sortableElements = this.getOrderedVectorElements(updatesMap);

        int newCapacity = numMappings + updatesMap.size();
        int[] newIndices = new int[newCapacity];
        double[] newValues = new double[newCapacity];

        int k = 0;
        int i = 0, j = 0;
        for (; i < numMappings && j < sortableElements.length; ++k) {
            int updateIndex = sortableElements[j].getIndex();
            double updateValue = sortableElements[j].getValue();
            if (indices[i] < updateIndex) {
                newIndices[k] = indices[i];
                newValues[k] = values[i];
                ++i;
            } else if (indices[i] > updateIndex) {
                newIndices[k] = updateIndex;
                newValues[k] = updateValue;
                ++j;
            } else {
                newIndices[k] = updateIndex;
                newValues[k] = updateValue;
                ++i;
                ++j;
            }
        }

        for (; i < numMappings; ++i, ++k) {
            newIndices[k] = indices[i];
            newValues[k] = values[i];
        }
        for (; j < updatesMap.size(); ++j, ++k) {
            newIndices[k] = sortableElements[j].getIndex();
            newValues[k] = sortableElements[j].getValue();
        }

        indices = newIndices;
        values = newValues;
        numMappings = k;
    }

    public void compress() {
        if (numMappings != values.length) {
            int[] newIndices = new int[numMappings];
            System.arraycopy(indices, 0, newIndices, 0, numMappings);
            indices = newIndices;
            double[] newValues = new double[numMappings];
            System.arraycopy(values, 0, newValues, 0, numMappings);
            values = newValues;
        }
    }

    public void reshape(){
        int[] newIndices = new int[numMappings];
        double[] newValues = new double[numMappings];
        int newNumMappings = 0;
        for(int index = 0, newIndex = 0; index < numMappings; index++){
            if(values[index]!=0.0D){
                newIndices[newIndex] = indices[index];
                newValues[newIndex] = values[index];
                newIndex++;
                newNumMappings++;
            }
        }
        indices = newIndices;
        values = newValues;
        numMappings = newNumMappings;

        compress();
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < getNumMappings(); i++) {
            result = 31 * result + indices[i];
            result = 31 * result + (int) Double.doubleToRawLongBits(values[i]);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OrderedIntDoubleMapping) {
            OrderedIntDoubleMapping other = (OrderedIntDoubleMapping) o;
            if (getNumMappings() == other.getNumMappings()) {
                for (int i = 0; i < getNumMappings(); i++) {
                    if (indices[i] != other.indices[i] || values[i] != other.values[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(10 * getNumMappings());
        for (int i = 0; i < getNumMappings(); i++) {
            result.append('(');
            result.append(indices[i]);
            result.append(',');
            result.append(values[i]);
            result.append(')');
        }
        return result.toString();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public OrderedIntDoubleMapping clone() {
        return new OrderedIntDoubleMapping(indices.clone(), values.clone());
    }

    public void increment(int index, double increment) {
        int offset = find(index);
        if (offset >= 0) {
            double newValue = values[offset] + increment;
            insertOrUpdateValueIfPresent(offset, newValue);
        } else {
            insertValueIfNotDefault(index, offset, increment);
        }
    }

    private void insertValueIfNotDefault(int index, int offset, double value) {
        if (!noDefault || value != DEFAULT_VALUE) {
            if (numMappings >= indices.length) {
                growTo(Math.max((int) (1.2 * numMappings), numMappings + 1));
            }
            int at = -offset - 1;
            if (numMappings > at) {
                for (int i = numMappings - 1, j = numMappings; i >= at; i--, j--) {
                    indices[j] = indices[i];
                    values[j] = values[i];
                }
            }
            indices[at] = index;
            values[at] = value;
            numMappings++;
        }
    }

    private void insertOrUpdateValueIfPresent(int offset, double newValue) {
        if (noDefault && newValue == DEFAULT_VALUE) {
            for (int i = offset + 1, j = offset; i < numMappings; i++, j++) {
                indices[j] = indices[i];
                values[j] = values[i];
            }
            numMappings--;
        } else {
            values[offset] = newValue;
        }
    }

    private OrderedVectorElement[] getOrderedVectorElements(Int2DoubleOpenHashMap map) {
        OrderedVectorElement[] sortableElements = new OrderedVectorElement[this.numMappings];

        int s = 0;
        for (Int2DoubleMap.Entry entrySet : map.int2DoubleEntrySet()) {
            sortableElements[s++] = new OrderedVectorElement(entrySet.getIntKey(), entrySet.getDoubleValue());
        }

        Arrays.sort(sortableElements);
        return sortableElements;
    }
}
