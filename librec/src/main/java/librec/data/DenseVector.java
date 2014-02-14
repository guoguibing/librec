package librec.data;

import happy.coding.math.Randoms;
import happy.coding.math.Stats;

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
		this(array, true);
	}

	public DenseVector(double[] array, boolean deep) {
		this.size = array.length;
		data = deep ? Arrays.copyOf(array, array.length) : array;
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

	public double[] getData() {
		return data;
	}

	public double mean() {
		return Stats.mean(data);
	}

	public void set(int idx, double val) {
		data[idx] = val;
	}

	public void add(int idx, double val) {
		data[idx] += val;
	}

	public void sub(int idx, double val) {
		data[idx] -= val;
	}

	public DenseVector add(double val) {
		for (int i = 0; i < size; i++)
			data[i] += val;

		return this;
	}

	public DenseVector sub(double val) {
		for (int i = 0; i < size; i++)
			data[i] -= val;

		return this;
	}

	public DenseVector scale(double val) {
		for (int i = 0; i < size; i++)
			data[i] *= val;

		return this;
	}

	public DenseVector add(DenseVector vec) {
		assert size == vec.size;

		for (int i = 0; i < vec.size; i++)
			data[i] += vec.data[i];

		return this;
	}

	public DenseVector sub(DenseVector vec) {
		assert size == vec.size;

		for (int i = 0; i < vec.size; i++)
			data[i] -= vec.data[i];

		return this;
	}

	public double inner(DenseVector vec) {
		assert size == vec.size;

		double result = 0;
		for (int i = 0; i < vec.size; i++)
			result += get(i) * vec.get(i);

		return result;
	}

	public double inner(SparseVector vec) {
		double result = 0;
		for (int j : vec.getIndex())
			result += vec.get(j) * get(j);

		return result;
	}

	public DenseMatrix outer(DenseVector vec) {
		DenseMatrix mat = new DenseMatrix(this.size, vec.size);

		for (int i = 0; i < mat.numRows; i++)
			for (int j = 0; j < mat.numCols; j++)
				mat.set(i, j, get(i) * vec.get(j));

		return mat;
	}

}
