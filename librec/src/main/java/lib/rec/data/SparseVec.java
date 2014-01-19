package lib.rec.data;

import java.util.Arrays;

import no.uib.cipr.matrix.sparse.SparseVector;

public class SparseVec extends SparseVector {

	private static final long serialVersionUID = 1L;

	int[] index;

	public SparseVec(int size) {
		super(size);
	}

	public SparseVec(SparseVector sv) {
		super(sv);
	}

	/**
	 * Check if a vector contains a specific index
	 * 
	 * @param idx
	 *            the idex to search
	 */
	public boolean contains(int idx) {
		getIndex(false);

		return Arrays.binarySearch(index, idx) >= 0;
	}
	
	public boolean contains(int idx, boolean refresh) {
		getIndex(refresh);

		return Arrays.binarySearch(index, idx) >= 0;
	}

	/**
	 * retrieve the cached index
	 */
	@Override
	public int[] getIndex() {
		return getIndex(false);
	}

	/**
	 * get the indexes of a sparse vector
	 * 
	 * @param refresh
	 *            whether to retrive elements again or use the cached ones
	 */
	public int[] getIndex(boolean refresh) {
		if (index == null || refresh)
			index = super.getIndex();

		return index;
	}
}
