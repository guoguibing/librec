package lib.rec.data;

import happy.coding.math.Randoms;

import java.util.Arrays;

public class DenseVec {

	protected int size;
	protected double[] data;

	public DenseVec(int size) {
		this.size = size;
		data = new double[size];
	}

	public DenseVec(DenseVec vec) {
		this.size = vec.size;
		data = Arrays.copyOf(vec.data, size);
	}

	public DenseVec clone() {
		return new DenseVec(this);
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

}
