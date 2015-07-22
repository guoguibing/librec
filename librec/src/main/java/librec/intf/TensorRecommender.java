package librec.intf;

import java.util.List;

import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseTensor;

public class TensorRecommender extends IterativeRecommender {

	/* for all tensors */
	protected static SparseTensor rateTensor;
	protected static int numDimensions, userDimension, itemDimension;
	protected static int[] dimensions;

	/* for a specific recommender */
	protected SparseTensor trainTensor, testTensor;

	static {
		rateTensor = rateDao.getRateTensor();
		numDimensions = rateTensor.numDimensions();
		dimensions = rateTensor.dimensions();
		
		userDimension = rateTensor.getUserDimension();
		itemDimension = rateTensor.getItemDimension();
	}

	public TensorRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) throws Exception {
		super(trainMatrix, testMatrix, fold);

		// construct train and test data
		trainTensor = rateTensor.clone();
		testTensor = new SparseTensor(dimensions);
		testTensor.setUserDimension(userDimension);
		testTensor.setItemDimension(itemDimension);
		
		for (MatrixEntry me : testMatrix) {
			int u = me.row();
			int i = me.column();

			List<Integer> indices = rateTensor.getIndices(u, i);

			for (int index: indices) {
				int[] keys = rateTensor.keys(index);
				testTensor.set(rateTensor.value(index), keys);
				trainTensor.remove(keys);
			}
		}
	}

}
