package librec.rating;

import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

/**
 * Bayesian UCM: Nicola Barbieri et al., <strong>Modeling Item Selection and Relevance for Accurate Recommendations: a
 * Bayesian Approach</strong>, RecSys 2011.
 * 
 * @author Guo Guibing
 *
 */
public class BUCM extends GraphicRecommender {

	public BUCM(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		// TODO Auto-generated method stub
		super.initModel();
	}

	@Override
	protected void inferParams() {
		// TODO Auto-generated method stub
		super.inferParams();
	}
}
