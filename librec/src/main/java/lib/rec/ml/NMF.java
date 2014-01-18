package lib.rec.ml;

import lib.rec.core.RegSVD;
import lib.rec.data.SparseMat;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends RegSVD {

	public NMF(SparseMat trainMatrix, SparseMat testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";
	}

}
