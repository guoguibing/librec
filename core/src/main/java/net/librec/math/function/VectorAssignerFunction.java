package net.librec.math.function;

import net.librec.math.structure.DenseVector;
import net.librec.math.structure.VectorAssigner;

/**
 * the function to the argument and return the result
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public final class VectorAssignerFunction {
    static VectorAssigner valueOf(double doubleValue) {
        return (index, value) -> doubleValue;
    }

    static VectorAssigner valueOf(DenseVector vector) {
        return (index, value) -> vector.get(index);
    }
}
