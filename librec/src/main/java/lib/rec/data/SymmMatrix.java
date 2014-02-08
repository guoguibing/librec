package lib.rec.data;

import java.util.Arrays;

/**
 * Data Structure, Lower Symmetric Matrix
 * 
 * @author guoguibing
 * 
 */
public class SymmMatrix {

	protected int dim;
	protected double[][] data;

	/**
	 * Constructor for Symmetric Matrix
	 * 
	 * @param dim
	 *            dimension size
	 */
	public SymmMatrix(int dim) {
		this.dim = dim;

		data = new double[dim][];
		for (int i = 0; i < dim; i++)
			data[i] = new double[i + 1];
	}

	public SymmMatrix(SymmMatrix mat) {
		dim = mat.dim;

		data = Arrays.copyOf(mat.data, mat.data.length);
	}

	public SymmMatrix clone() {
		return new SymmMatrix(this);
	}

	/**
	 * get a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 * @return a value at (row, col) if row<col; otherwise at (col, row)
	 */
	public double get(int row, int col) {
		return row >= col ? data[row][col] : data[col][row];
	}

	/**
	 * set a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public void set(int row, int col, double val) {
		if (row >= col)
			data[row][col] = val;
		else
			data[col][row] = val;
	}

	/**
	 * add a specific value at (row, col) of a Symmetric (upper) matrix
	 * 
	 * @param row
	 *            row id
	 * @param col
	 *            col id
	 */
	public void add(int row, int col, double val) {
		if (row >= col)
			data[row][col] += val;
		else
			data[col][row] += val;
	}

	/**
	 * find a complete row of items
	 * 
	 * @param row
	 *            row id
	 * @return a sparse vector
	 */
	public SparseVector row(int row) {
		SparseVector nv = new SparseVector(dim);
		for (int col = 0; col < dim; col++) {
			double val = get(row, col);
			if (val != 0)
				nv.set(col, val);
		}

		return nv;
	}
}
