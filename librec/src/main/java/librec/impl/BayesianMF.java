package librec.impl;

import librec.data.SparseMatrix;
import librec.intf.IterativeRecommender;

public class BayesianMF extends IterativeRecommender {

	public BayesianMF(SparseMatrix trainMatrix, SparseMatrix testMatrix,
			int fold) {
		super(trainMatrix, testMatrix, fold);

		algoName = "BayesianMF";
	}

}
