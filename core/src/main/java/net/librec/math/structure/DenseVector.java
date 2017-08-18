// Copyright (C) 2014 Guibing Guo
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

import net.librec.math.algorithm.Randoms;
import net.librec.math.algorithm.Stats;
import net.librec.util.StringUtil;

import java.io.Serializable;


/**
 * Data Structure: dense vector
 *
 * @author guoguibing
 */
public class DenseVector implements Serializable {

    private static final long serialVersionUID = -2930574547913792430L;

    protected int size;
    protected double[] data;

    /**
     * Construct a dense vector with a specific size
     *
     * @param size the size of vector
     */
    public DenseVector(int size) {
        this.size = size;
        data = new double[size];
    }

    /**
     * Construct a dense vector by deeply copying data from a given array
     *
     * @param array a given array
     */
    public DenseVector(double[] array) {
        this(array, true);
    }

    /**
     * Construct a dense vector by copying data from a given array
     *
     * @param array a given data array
     * @param deep  whether to deep copy array data
     */
    public DenseVector(double[] array, boolean deep) {
        this.size = array.length;
        if (deep) {
            data = new double[array.length];
            for (int i = 0; i < size; i++)
                data[i] = array[i];
        } else {
            data = array;
        }
    }

    /**
     * Construct a dense vector by deeply copying data from a given vector
     *
     * @param vec a given vector
     */
    public DenseVector(DenseVector vec) {
        this(vec.data);
    }

    /**
     * Make a deep copy of current vector
     */
    public DenseVector clone() {
        return new DenseVector(this);
    }

    /**
     * Initialize a dense vector with Gaussian values
     *
     * @param mean  mean of the gaussian
     * @param sigma sigma of the gaussian
     */
    public void init(double mean, double sigma) {
        for (int i = 0; i < size; i++)
            data[i] = Randoms.gaussian(mean, sigma);
    }

    /**
     * Initialize a dense vector with uniform values in (0, 1)
     */
    public void init() {
        for (int i = 0; i < size; i++)
            data[i] = Randoms.uniform();
    }

    /**
     * Initialize a dense vector with uniform values in (0, range)
     *
     * @param range max of the range
     */
    public void init(double range) {
        for (int i = 0; i < size; i++)
            data[i] = Randoms.uniform(0, range);
    }

    /**
     * Get a value at entry [index]
     *
     * @param idx index of the data
     * @return value at entry [index]
     */
    public double get(int idx) {
        return data[idx];
    }

    /**
     * @return vector's data
     */
    public double[] getData() {
        return data;
    }

    /**
     * @return mean of current vector
     */
    public double mean() {
        return Stats.mean(data);
    }

    /**
     * @return summation of entries
     */
    public double sum() {
        return Stats.sum(data);
    }

    /**
     * Set a value to entry [index]
     *
     * @param idx index to set
     * @param val value to set
     */
    public void set(int idx, double val) {
        data[idx] = val;
    }

    /**
     * Set a value to all entries
     *
     * @param val  value to set
     */
    public void setAll(double val) {
        for (int i = 0; i < size; i++)
            data[i] = val;
    }

    /**
     * Add a value to entry [index]
     *
     * @param idx index to add
     * @param val value to add
     */
    public void add(int idx, double val) {
        data[idx] += val;
    }

    /**
     * Substract a value from entry [index]
     *
     * @param idx index to minus
     * @param val value to minus
     */
    public void minus(int idx, double val) {
        data[idx] -= val;
    }

    /**
     * Return a new dense vector by adding a value to all entries of current vector {@code a[i] = b[i] + c}
     *
     * @param val  value to add
     * @return a new dense vector by adding a value to all entries of current vector {@code a[i] = b[i] + c}
     */
    public DenseVector add(double val) {
        DenseVector result = new DenseVector(size);

        for (int i = 0; i < size; i++)
            result.data[i] = this.data[i] + val;

        return result;
    }

    /**
     * Return this dense vector by adding a value to all entries of current vector {@code b[i] = b[i] + c}
     *
     * @param val  value to add
     * @return this dense vector by adding a value to all entries of current vector {@code b[i] = b[i] + c}
     */
    public DenseVector addEqual(double val) {

        for (int i = 0; i < size; i++)
            data[i] += val;

        return this;
    }

    /**
     * Do vector operation: {@code a + b}
     *
     * @param vec vector to add
     * @return a new dense vector with results of {@code c = a + b}
     */
    public DenseVector add(DenseVector vec) {
        assert size == vec.size;

        DenseVector result = new DenseVector(size);
        for (int i = 0; i < result.size; i++)
            result.data[i] = this.data[i] + vec.data[i];

        return result;
    }

    /**
     * Do vector operation: {@code a + b}
     *
     * @param vec vector to add
     * @return this dense vector with results of {@code a = a + b}
     */
    public DenseVector addEqual(DenseVector vec) {
        assert size == vec.size;

        for (int i = 0; i < size; i++)
            data[i] += vec.data[i];

        return this;
    }

    /**
     * Return a new dense vector by substructing a value from all entries of current vector {@code a[i] = b[i] - c}
     *
     * @param val value to minus
     * @return a new dense vector by substructing a value from all entries of current vector {@code a[i] = b[i] - c}
     */
    public DenseVector minus(double val) {

        DenseVector result = new DenseVector(size);

        for (int i = 0; i < size; i++)
            result.data[i] = this.data[i] - val;

        return result;
    }

    /**
     * Return this dense vector by substructing a value from all entries of current vector {@code b[i] = b[i] - c}
     *
     * @param val value to
     * @return this dense vector by substructing a value from all entries of current vector {@code b[i] = b[i] - c}
     */
    public DenseVector minusEqual(double val) {
        for (int i = 0; i < size; i++)
            data[i] -= val;

        return this;
    }

    /**
     * Do vector operation: {@code a - b}
     *
     * @param vec a given vector
     * @return a new dense vector with results of {@code c = a - b}
     */
    public DenseVector minus(DenseVector vec) {
        assert size == vec.size;

        DenseVector result = new DenseVector(size);
        for (int i = 0; i < vec.size; i++)
            result.data[i] = this.data[i] - vec.data[i];

        return result;
    }

    /**
     * Do vector operation: {@code a - b}
     *
     * @param vec a given vector
     * @return this dense vector with results of {@code a = a - b}
     */
    public DenseVector minusEqual(DenseVector vec) {
        assert size == vec.size;

        for (int i = 0; i < vec.size; i++)
            data[i] -= vec.data[i];

        return this;
    }

    /**
     * Return a new dense vector by scaling a value to all entries of current vector {@code a = b.scale(c)}
     *
     * @param val a given value for scaling
     * @return a new dense vector by scaling a value to all entries of current vector {@code a = b.scale(c)}
     */
    public DenseVector scale(double val) {

        DenseVector result = new DenseVector(size);
        for (int i = 0; i < size; i++)
            result.data[i] = this.data[i] * val;

        return result;
    }

    /**
     * Return this dense vector by scaling a value to all entries of current vector {@code b = b.scale(c)}.
     *
     * @param val a given value for scaling
     * @return this dense vector by scaling a value to all entries of current vector {@code b = b.scale(c)}
     */
    public DenseVector scaleEqual(double val) {
        for (int i = 0; i < size; i++)
            data[i] *= val;
        return this;
    }


    /**
     * Do vector operation: {@code a^t * b}
     *
     * @param vec a given vector
     * @return the inner product of two vectors
     */
    public double inner(DenseVector vec) {
        assert size == vec.size;

        double result = 0;
        for (int i = 0; i < vec.size; i++)
            result += get(i) * vec.get(i);

        return result;
    }

    /**
     * Do vector operation: {@code a^t * b}
     *
     * @param vec a given vector
     * @return the inner product of two vectors
     */
    public double inner(SparseVector vec) {
        double result = 0;
        for (int j : vec.getIndex())
            result += vec.get(j) * get(j);

        return result;
    }

    /**
     * Do vector operation: {@code a * b^t}
     *
     * @param vec a given vector
     * @return the outer product of two vectors
     */
    public DenseMatrix outer(DenseVector vec) {
        DenseMatrix mat = new DenseMatrix(this.size, vec.size);

        for (int i = 0; i < mat.numRows; i++)
            for (int j = 0; j < mat.numColumns; j++)
                mat.set(i, j, get(i) * vec.get(j));

        return mat;
    }

    /**
     * Return the Kronecker product of two vectors
     *
     * @param M a dense matrix
     * @param N another dense matrix
     * @return the Kronecker product of two vectors
     */
    public static DenseVector kroneckerProduct(DenseVector M, DenseVector N) {
        DenseVector res = new DenseVector(M.size * N.size);

        int i = 0;
        for (int m = 0; m < M.size; m++) {
            double mVal = M.get(m);
            for (int n = 0; n < N.size; n++) {
                res.set(i++, mVal * N.get(n));
            }
        }

        return res;
    }

    @Override
    public String toString() {
        return StringUtil.toString(data);
    }

    /**
     * @param data the data to set
     */
    public void setData(double[] data) {
        this.data = data;
    }

}
