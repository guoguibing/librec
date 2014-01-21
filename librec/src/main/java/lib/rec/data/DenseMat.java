package lib.rec.data;

import happy.coding.math.Randoms;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;

public class DenseMat extends DenseMatrix {

	public DenseMat(int numRows, int numColumns) {
		super(numRows, numColumns);
	}

	public DenseMat(Matrix A) {
		super(A);
	}

	public DenseMat copy() {
		return new DenseMat(this);
	}

	/**
	 * initialize a dense matrix with small Guassian values <br/>
	 * 
	 * <strong>NOTE:</strong> small initial values make it easier to train a
	 * model; otherwise a very small learning rate may be needed (especially
	 * when the number of factors is large) which can cause bad performance.
	 */
	public void init(double mean, double sigma) {

		double[] data = super.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.gaussian(mean, sigma);
	}

	/**
	 * initialize a dense matrix with small random values
	 */
	public void init() {

		double[] data = super.getData();
		for (int i = 0; i < data.length; i++)
			data[i] = Randoms.uniform(0.0, 0.01);
	}

	public DenseVector row(int row) {

		DenseVector dv = new DenseVector(numColumns);

		for (int j = 0; j < numColumns(); j++) {
			double val = super.get(row, j);
			if (val != 0.0)
				dv.set(j, val);
		}

		return dv;
	}

	/**
	 * row x row of two matrix
	 * 
	 * @param m
	 *            the first matrix
	 * @param mrow
	 *            row of the first matrix
	 * @param n
	 *            the second matrix
	 * @param nrow
	 *            row of the second matrix
	 * @return inner product of two row vectors
	 */
	public static double rowMult(DenseMatrix m, int mrow, DenseMatrix n, int nrow) {

		assert m.numColumns() == n.numColumns();

		double result = 0;

		double[] mdata = m.getData();
		double[] ndata = n.getData();

		// NOTE: the data storage in DenseMatrix is column-wise
		int mRows = m.numRows(), nRows = n.numRows();
		for (int j = 0, k = m.numColumns(); j < k; j++)
			result += mdata[mrow + j * mRows] * ndata[nrow + j * nRows];

		return result;
	}

	/**
	 * set one value to the whole row
	 * 
	 * @param row
	 *            row id
	 * @param val
	 *            value to be set
	 */
	public void setRow(int row, double val) {
		for (int c = 0; c < numColumns; c++)
			super.set(row, c, val);
	}
}
