package net.librec.math.structure;

/**
 * Apply the function to the argument and return the result
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public interface MatrixAssigner {
    double getValue(int row, int column, double value);
}
