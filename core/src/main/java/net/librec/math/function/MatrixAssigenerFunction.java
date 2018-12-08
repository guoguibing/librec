package net.librec.math.function;

import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixAssigner;

/**
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public class MatrixAssigenerFunction {
    static MatrixAssigner valueOf(double doubleValue) {
        return (row, column, value) -> doubleValue;
    }

    static MatrixAssigner valueOf(DenseMatrix matrix) {
        return (row, column, value) -> matrix.get(row, column);
    }
}
