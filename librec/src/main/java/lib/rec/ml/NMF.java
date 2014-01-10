package lib.rec.ml;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import lib.rec.core.RegSVD;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends RegSVD {

	public NMF(CompRowMatrix trainMatrix, CompRowMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";
	}

}
