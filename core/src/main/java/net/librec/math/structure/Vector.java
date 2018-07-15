package net.librec.math.structure;

import net.librec.common.IndexException;

import java.io.Serializable;


/**
 * The basic vector interface
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public interface Vector extends Cloneable, Serializable, Iterable<Vector.VectorEntry> {

    /**
     * @return a formatted String suitable for output
     */
    String toString();


    /**
     * Number of active entries.  An "active element" is an element which is explicitly stored,
     * regardless of its value.  Note that inactive entries have value 0.
     *
     * @return an int
     */
    int size();

    /**
     * Return the cardinality of the recipient (the maximum number of values)
     *
     * @return an int
     */
    int cardinality();

    /**
     * Number of active entries.  An "active element" is an element which is explicitly stored,
     * regardless of its value.  Note that inactive entries have value 0.
     *
     * @return an int
     */
    int getNumEntries();

    /**
     * @return true iff this implementation should be considered dense -- that it explicitly
     * represents every value
     */
    boolean isDense();

    /**
     * @return true iff this implementation should be considered to be iterable in index order in an efficient way.
     * \     * in ascending order by index.
     */
    boolean isSequentialAccess();


    /**
     * Return an object of Vector.VectorEntry representing an entry of this Vector. Useful when designing new iterator
     * types.
     *
     * @param index Index of the Vector.VectorEntry required
     * @return The Vector.Element Object
     */
    VectorEntry getVectorEntry(int index);

    /**
     * Set the value at the given index, without checking bounds
     *
     * @param index an int index into the receiver
     * @param value a double value to set
     * @throws IndexException if the index is out of bounds
     */
    void set(int index, double value);

    /**
     * Return the value at the given index, without checking bounds
     *
     * @param index an int index
     * @return the double at the index
     * @throws IndexException if the index is out of bounds
     */
    double get(int index);

    /**
     * Return the k-norm of the vector. <p/> See http://en.wikipedia.org/wiki/Lp_space <p>
     * Technically, when {@code 0 > power < 1}, we don't have a norm, just a metric, but we'll overload this here. Also supports power == 0 (number of
     * non-zero elements) and power = {@link Double#POSITIVE_INFINITY} (max element). Again, see the Wikipedia page for
     * more info.
     *
     * @param power The power to use.
     */
    double norm(double power);

    /**
     * Return the sum of all the elements of the receiver
     *
     * @return a double
     */
    double sum();

    /**
     * Return the mean of all the elements
     *
     * @return a double
     */
    double mean();

    /**
     * Return the sum of squares of all elements in the vector. Square root of this value is the length of the vector.
     */
    double getLengthSquared();

    /**
     * Gets an estimate of the cost (in number of operations) it takes to lookup a random element in this vector.
     */
    double getLookupCost();

    /**
     * Gets an estimate of the cost (in number of operations) it takes to advance an iterator through the nonzero
     * elements of this vector.
     */
    double getIteratorAdvanceCost();

    /**
     * Return true iff adding a new (nonzero) element takes constant time for this vector.
     */
    boolean isAddConstantTime();

    /**
     * A holder for information about a specific item in the Vector. <p/> When using with an Iterator, the implementation
     * may choose to reuse this element, so you may need to make a clone if you want to keep it
     */
    interface VectorEntry {
        /**
         * @return the value of this vector element.
         */
        double get();

        /**
         * @return the index of this vector element.
         */
        int index();

        /**
         * @return the position of the vector element.
         */
        int position();

        /**
         * @param value Set the current element to value.
         */
        void set(double value);
    }
}
