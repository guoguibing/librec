package librec.data;

/**
 * An entry of a matrix. 
 */
public interface MatrixEntry {

	/**
	 * Returns the current row index
	 */
	int row();

	/**
	 * Returns the current column index
	 */
	int column();

	/**
	 * Returns the value at the current index
	 */
	double get();

	/**
	 * Sets the value at the current index
	 */
	void set(double value);

}
