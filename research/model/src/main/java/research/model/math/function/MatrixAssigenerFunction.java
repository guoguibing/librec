package research.model.math.function;


import research.model.math.structure.DenseMatrix;
import research.model.math.structure.MatrixAssigner;

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
