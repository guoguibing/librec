package lib.rec.data;

import happy.coding.math.Randoms;
import no.uib.cipr.matrix.DenseVector;

public class DenseVec extends DenseVector {

	private static final long serialVersionUID = 1L;

	public DenseVec(int size) {
		super(size);
	}

	/**
	 * initialize a dense vector with Gaussian values
	 */
	public void init(double mean, double sigma) {
		double[] data = super.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.gaussian(mean, sigma);
	}

}
