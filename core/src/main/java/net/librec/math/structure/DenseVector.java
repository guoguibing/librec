package net.librec.math.structure;

import net.librec.common.CardinalityException;
import net.librec.math.algorithm.Randoms;
import net.librec.math.function.DoubleDoubleFunction;
import net.librec.math.function.DoubleFunction;

import java.util.Iterator;

/**
 * abstract dense vector
 *
 * @author Keqiang Wang (email: sei.wkq2008@gmail.com)
 */
public abstract class DenseVector extends AbstractVector {
    public DenseVector(int cardinality) {
        super(cardinality);
    }

    /**
     * Initialize a dense vector with Gaussian values
     *
     * @param mean  mean of the gaussian
     * @param sigma sigma of the gaussian
     */
    public void init(double mean, double sigma) {
        assign((index, value) -> Randoms.gaussian(mean, sigma));
    }

    /**
     * Initialize a dense vector with uniform values in (0, 1)
     */
    public void init() {
        assign((index, value) -> Randoms.uniform());
    }

    /**
     * Initialize a dense vector with uniform values in (0, range)
     *
     * @param range max of the range
     */
    public void init(double range) {
        assign((index, value) -> Randoms.uniform(0, range));
    }

    /**
     * Examples speak louder than words:  aggregate(plus, pow(2)) is another way to say
     * getLengthSquared(), aggregate(max, abs) is norm(Double.POSITIVE_INFINITY).  To sum all of the positive values,
     * aggregate(plus, max(0)).
     *
     * @param aggregator used to combine the current value of the aggregation with the result of map.apply(nextValue)
     * @param map        a function to apply to each element of the vector in turn before passing to the aggregator
     * @return the final aggregation
     */
    public double aggregate(DoubleDoubleFunction aggregator, DoubleFunction map) {
        if (cardinality == 0) {
            return 0;
        }

        // If the aggregator is associative and commutative and it's likeLeftMult (fa(0, y) = 0), and there is
        // at least one zero in the vector (cardinality > getNumNondefaultElements) and applying fm(0) = 0, the result
        // gets cascaded through the aggregation and the final result will be 0.
        if (aggregator.isAssociativeAndCommutative() && aggregator.isLikeLeftMult()
                && cardinality > getNumEntries() && !map.isDensifying()) {
            return 0;
        }

        double result;
        Iterator<VectorEntry> iterator;
        // If fm(0) = 0 and fa(x, 0) = x, we can skip all zero values.
        if (!map.isDensifying() && aggregator.isLikeRightPlus()) {
            iterator = iterator();
            if (!iterator.hasNext()) {
                return 0;
            }
        } else {
            iterator = iterator();
        }
        VectorEntry vectorEntry = iterator.next();
        result = map.apply(vectorEntry.get());
        while (iterator.hasNext()) {
            vectorEntry = iterator.next();
            result = aggregator.apply(result, map.apply(vectorEntry.get()));
        }

        return result;
    }

    public DenseVector normalize() {
        return times(1.0D / Math.sqrt(getLengthSquared()));
    }

    public DenseVector normalize(double power) {
        return times(1.0D / norm(power));
    }

    /**
     * Return the dot product of the recipient and the argument
     *
     * @param vector a Vector
     * @return a new Vector
     * @throws CardinalityException if the cardinalities differ
     */
    public double dot(Vector vector) {
        double resultValue = 0.0D;

        for (VectorEntry vectorEntry : vector) {
            resultValue += vectorEntry.get() * get(vectorEntry.index());
        }
        return resultValue;
    }

    /**
     * Do vector operation: {@code a * b^t}
     *
     * @param vec a given vector
     * @return the outer product of two vectors
     */
    public DenseMatrix outer(DenseVector vec) {
        DenseMatrix mat = new DenseMatrix(this.cardinality, vec.cardinality);

        for (int i = 0; i < mat.rowSize(); i++)
            for (int j = 0; j < mat.columnSize(); j++)
                mat.set(i, j, get(i) * vec.get(j));

        return mat;
    }

    @Override
    public DenseVector clone() {
        try {
            DenseVector r = (DenseVector) super.clone();
            r.cardinality = cardinality;
            return r;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Can't happen");
        }
    }

    public DenseVector assign(DoubleFunction f) {
        for (int index = 0; index < cardinality(); index++) {
            set(index, f.apply(get(index)));
        }
        return this;
    }

    /**
     * Apply the function to each element of the receiver
     *
     * @param vectorAssigner a vector function to apply
     * @return the modified receiver
     */
    public DenseVector assign(VectorAssigner vectorAssigner) {
        for (int index = 0; index < cardinality(); index++) {
            set(index, vectorAssigner.getValue(index, get(index)));
        }
        return this;
    }

    /**
     * Return a new vector containing the sum of each value of the recipient and the argument
     *
     * @param value a double
     * @return a new Vector
     */
    public DenseVector plus(double value) {
        DenseVector resultVector = new VectorBasedDenseVector(cardinality());
        resultVector.assign((rowIndex, tempValue) -> get(rowIndex) + value);
        return resultVector;
    }

    public void plus(int index, double value) {
        set(index, value + get(index));
    }

    public abstract double[] getValues();

    /**
     * Return a new vector containing the element by element sum of the recipient and the argument
     *
     * @param vector a Vector
     * @return a new Vector
     */
    public DenseVector plus(Vector vector) {
        DenseVector resultVector = new VectorBasedDenseVector(cardinality());

        for (VectorEntry vectorEntry : vector) {
            int index = vectorEntry.index();
            resultVector.set(index, get(index) + vectorEntry.get());
        }
        return resultVector;
    }

    /**
     * Return a new vector containing the element by element difference of the recipient and the argument
     *
     * @param vector a Vector
     * @return a new Vector
     */
    public DenseVector minus(Vector vector) {
        DenseVector resultVector = new VectorBasedDenseVector(cardinality());
        for (VectorEntry vectorEntry : vector) {
            int index = vectorEntry.index();
            resultVector.set(index, get(index) - vectorEntry.get());
        }
        return resultVector;
    }

    /**
     * Return a new vector containing the product of each value of the recipient and the argument
     *
     * @param value a double argument
     * @return a new Vector
     */
    public DenseVector times(double value) {
        DenseVector resultVector = new VectorBasedDenseVector(cardinality());
        resultVector.assign((index, tempValue) -> get(index) * value);
        return resultVector;
    }

    /**
     * Return a new dense vector containing the element-wise product of the recipient and the argument
     *
     * @param vector a Dense Vector argument
     * @return a new Vector
     * @throws CardinalityException if the cardinalities differ
     */
    public DenseVector times(DenseVector vector) {
        DenseVector resultVector = new VectorBasedDenseVector(cardinality());
        resultVector.assign((index, tempValue) -> get(index) * vector.get(index));
        return resultVector;
    }

    /**
     * @return true
     */
    @Override
    public boolean isDense() {
        return true;
    }

    /**
     * @return true
     */
    @Override
    public boolean isSequentialAccess() {
        return true;
    }

    @Override
    public double getLookupCost() {
        return 1;
    }

    @Override
    public double getIteratorAdvanceCost() {
        return 1;
    }

    @Override
    public boolean isAddConstantTime() {
        return true;
    }

    @Override
    public int getNumEntries() {
        return cardinality();
    }
}
