package librec.data;

/**
 * An entry of a vector. 
 */
public interface VectorEntry {

	/**
	 * Returns the current index
	 */
	int index();

	/**
	 * Returns the value at the current index
	 */
	double get();

	/**
	 * Sets the value at the current index
	 */
	void set(double value);

}
