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

package librec.data;

import happy.coding.io.Strings;
import happy.coding.math.Randoms;
import happy.coding.math.Stats;

import java.io.Serializable;

/**
 * Data Structure: dense vector
 * 
 * @author guoguibing
 * 
 */
public class DenseVector implements Serializable {

	private static final long serialVersionUID = -2930574547913792430L;

	protected int size;
	protected double[] data;

	/**
	 * Construct a dense vector with a specific size
	 * 
	 * @param size
	 *            the size of vector
	 */
	public DenseVector(int size) {
		this.size = size;
		data = new double[size];
	}

	/**
	 * Construct a dense vector by deeply copying data from a given array
	 */
	public DenseVector(double[] array) {
		this(array, true);
	}

	/**
	 * Construct a dense vector by copying data from a given array
	 * 
	 * @param array
	 *            a given data array
	 * @param deep
	 *            whether to deep copy array data
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
	 */
	public void init(double range) {
		for (int i = 0; i < size; i++)
			data[i] = Randoms.uniform(0, range);
	}

	/**
	 * Get a value at entry [index]
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
	public double sum(){
		return Stats.sum(data);
	}

	/**
	 * Set a value to entry [index]
	 */
	public void set(int idx, double val) {
		data[idx] = val;
	}

	/**
	 * Set a value to all entries
	 */
	public void setAll(double val) {
		for (int i = 0; i < size; i++)
			data[i] = val;
	}

	/**
	 * Add a value to entry [index]
	 */
	public void add(int idx, double val) {
		data[idx] += val;
	}

	/**
	 * Substract a value from entry [index]
	 */
	public void minus(int idx, double val) {
		data[idx] -= val;
	}

	/**
	 * @return a dense vector by adding a value to all entries of current vector
	 */
	public DenseVector add(double val) {
		DenseVector result = new DenseVector(size);

		for (int i = 0; i < size; i++)
			result.data[i] = this.data[i] + val;

		return result;
	}

	/**
	 * @return a dense vector by substructing a value from all entries of current vector
	 */
	public DenseVector minus(double val) {

		DenseVector result = new DenseVector(size);

		for (int i = 0; i < size; i++)
			result.data[i] = this.data[i] - val;

		return result;
	}

	/**
	 * @return a dense vector by scaling a value to all entries of current vector
	 */
	public DenseVector scale(double val) {

		DenseVector result = new DenseVector(size);
		for (int i = 0; i < size; i++)
			result.data[i] = this.data[i] * val;

		return result;
	}

	/**
	 * Do vector operation: {@code a + b}
	 * 
	 * @return a dense vector with results of {@code c = a + b}
	 */
	public DenseVector add(DenseVector vec) {
		assert size == vec.size;

		DenseVector result = new DenseVector(size);
		for (int i = 0; i < result.size; i++)
			result.data[i] = this.data[i] + vec.data[i];

		return result;
	}

	/**
	 * Do vector operation: {@code a - b}
	 * 
	 * @return a dense vector with results of {@code c = a - b}
	 */
	public DenseVector minus(DenseVector vec) {
		assert size == vec.size;

		DenseVector result = new DenseVector(size);
		for (int i = 0; i < vec.size; i++)
			result.data[i] = this.data[i] - vec.data[i];

		return result;
	}

	/**
	 * Do vector operation: {@code a^t * b}
	 * 
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
	 * @return the outer product of two vectors
	 */
	public DenseMatrix outer(DenseVector vec) {
		DenseMatrix mat = new DenseMatrix(this.size, vec.size);

		for (int i = 0; i < mat.numRows; i++)
			for (int j = 0; j < mat.numColumns; j++)
				mat.set(i, j, get(i) * vec.get(j));

		return mat;
	}

	@Override
	public String toString() {
		return Strings.toString(data);
	}

}
