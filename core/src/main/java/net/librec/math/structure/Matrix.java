package net.librec.math.structure;

import net.librec.common.IndexException;

import java.util.Iterator;

/**
 * The basic interface including numerous convenience functions
 * It is adopted from mahout math package
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public interface Matrix extends Cloneable, Iterable<MatrixEntry> {

    /**
     * @return a formatted String suitable for output
     */
    String toString();

    /**
     * @return The number of rows in the matrix.
     */
    int columnSize();

    /**
     * @return Returns the number of rows in the matrix.
     */
    int rowSize();

    Vector row(int row);

    Vector column(int column);

    /**
     * Return a reference to a row.  Changes to the view will change the original matrix.
     *
     * @param row The index of the row to return.
     * @return A vector that shares storage with the original.
     */
    Vector viewRow(int row);

    /**
     * Return a reference to a column.  Changes to the view will change the original matrix.
     *
     * @param column The index of the column to return.
     * @return A vector that shares storage with the original.
     */
    Vector viewColumn(int column);

    /**
     * Return a clone of the recipient
     *
     * @return a new Matrix
     */
    Matrix clone();

    /**
     * Return the value at the given indexes, without checking bounds
     *
     * @param row    an int row index
     * @param column an int column index
     * @return the double at the index
     * @throws IndexException if the index is out of bounds
     */
    double get(int row, int column);

    /**
     * Set the value at the given index, without checking bounds
     *
     * @param row    an int row index into the receiver
     * @param column an int column index into the receiver
     * @param value  a double value to set
     * @throws IndexException if the index is out of bounds
     */
    void set(int row, int column, double value);

    /**
     * Return the number of values in the recipient
     *
     * @return an int count
     */
    int getNumEntries();

    Iterator<MatrixEntry> iterator();

    boolean isRandomAccess();
}
