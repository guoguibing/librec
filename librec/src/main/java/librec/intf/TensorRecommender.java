package librec.intf;

import java.util.List;

import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.data.SparseTensor;

public class TensorRecommender extends IterativeRecommender {

	protected SparseTensor rateTensor, trainTensor, testTensor;
	protected int numDimensions;
	protected int[] dimensions;

	public TensorRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);

		// construct train and test data
		rateTensor = rateDao.getRateTensor();
		numDimensions = rateTensor.numDimensions();
		dimensions = rateTensor.dimensions();

		trainTensor = rateTensor.clone();
		testTensor = new SparseTensor(rateTensor.dimensions());
		for (MatrixEntry me : testMatrix) {
			int u = me.row();
			int i = me.column();

			List<Integer> indices = rateTensor.getIndices(u, i);

			for (int index : indices) {
				int[] keys = rateTensor.keys(index);
				testTensor.add(rateTensor.value(index), keys);
				trainTensor.remove(keys);
			}
		}
	}

}
