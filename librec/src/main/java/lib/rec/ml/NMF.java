package lib.rec.ml;

import lib.rec.data.SparseMatrix;
import lib.rec.intf.IterativeRecommender;

/**
 * Daniel D. Lee and H. Sebastian Seung, <strong>Algorithms for Non-negative
 * Matrix Factorization</strong>, NIPS 2001.
 * 
 * @author guoguibing
 * 
 */
public class NMF extends IterativeRecommender {

	public NMF(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "NMF";
	}

}
