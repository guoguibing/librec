package lib.rec.data;

import happy.coding.math.Randoms;

import java.util.Arrays;

/**
 * Data Structure: dense vector
 * 
 * @author guoguibing
 * 
 */
public class DenseVector {

	protected int size;
	protected double[] data;

	public DenseVector(int size) {
		this.size = size;
		data = new double[size];
	}

	public DenseVector(double[] array) {
		this.size = array.length;
		data = Arrays.copyOf(array, array.length);
	}

	public DenseVector(DenseVector vec) {
		this.size = vec.size;
		data = Arrays.copyOf(vec.data, vec.data.length);
	}

	public DenseVector clone() {
		return new DenseVector(this);
	}

	/**
	 * initialize a dense vector with Gaussian values
	 */
	public void init(double mean, double sigma) {
		for (int i = 0; i < size; i++)
			data[i] = Randoms.gaussian(mean, sigma);
	}

	public double get(int idx) {
		return data[idx];
	}

	public void set(int idx, double val) {
		data[idx] = val;
	}

	public void add(int idx, double val) {
		data[idx] += val;
	}

	public DenseVector add(DenseVector vec) {
		assert size == vec.size;

		for (int i = 0; i < vec.size; i++)
			add(i, vec.get(i));

		return this;
	}

	public double inner(DenseVector vec) {
		assert size == vec.size;

		double result = 0;
		for (int i = 0; i < vec.size; i++)
			result += get(i) * vec.get(i);

		return result;
	}

}
