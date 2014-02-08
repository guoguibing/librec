package lib.rec.ml;

import lib.rec.core.RegSVD;
import lib.rec.data.SparseMatrix;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends RegSVD {

	public NMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";
	}

}
