package research.model.eval;

import research.model.common.LibrecException;
import research.model.conf.Configuration;
import research.model.math.structure.*;
import research.model.recommend.item.KeyValue;
import research.model.recommend.item.RecommendedList;

import java.util.ArrayList;
import java.util.Map;

/**
 * Evaluator Context
 *
 * @author Wang Keqiang
 */

public class EvalContext {

    private RecommendedList groundTruthList;
    private RecommendedList recommendedList;
    private SymmMatrix similarityMatrix = null;
    private Configuration conf;


    public EvalContext(Configuration conf, RecommendedList recommendedList, SequentialAccessSparseMatrix testMatrix,
                       SymmMatrix similarityMatrix) {
        this.conf = conf;
        this.similarityMatrix = similarityMatrix;
        this.recommendedList = recommendedList;
        this.groundTruthList = getGroundTruthListFromSparseMatrix(testMatrix);
    }

    public EvalContext(Configuration conf, RecommendedList recommendedList, SequentialAccessSparseMatrix testMatrix) {
        this.conf = conf;
        this.recommendedList = recommendedList;
        this.groundTruthList = getGroundTruthListFromSparseMatrix(testMatrix);
    }





    /**
     * return the test set ground truth list
     *
     * @return the test set ground truth list
     */
    public RecommendedList getGroundTruthListFromSparseMatrix(SequentialAccessSparseMatrix testMatrix) {
        int numUsers = testMatrix.rowSize();
        RecommendedList groundTruthList = new RecommendedList(numUsers);
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            groundTruthList.addList(new ArrayList<KeyValue<Integer, Double>>());
        }
        for (MatrixEntry matrixEntry : testMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            double rating = matrixEntry.get();
            groundTruthList.add(userIdx, itemIdx, rating);
        }
        return groundTruthList;
    }

    /**
     * return the test set ground truth list
     *
     * @return the test set ground truth list
     */
    public RecommendedList getGroundTruthListFromSparseTensor(SparseTensor testTensor) {
        int userDimension = testTensor.getUserDimension();
        int itemDimension = testTensor.getItemDimension();

        int numUsers = testTensor.dimensions()[userDimension];
        RecommendedList groundTruthList = new RecommendedList(numUsers);
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            groundTruthList.addList(new ArrayList<KeyValue<Integer, Double>>());
        }
        for (TensorEntry testTensorEntry : testTensor) {
            int userIdx = testTensorEntry.key(userDimension);
            int itemIdx = testTensorEntry.key(itemDimension);
            double rating = testTensorEntry.get();
            groundTruthList.add(userIdx, itemIdx, rating);
        }
        return groundTruthList;
    }

    public RecommendedList getGroundTruthList() {
        return groundTruthList;
    }

    public void setGroundTruthList(RecommendedList groundTruthList) {
        this.groundTruthList = groundTruthList;
    }

    public RecommendedList getRecommendedList() {
        return recommendedList;
    }

    public void setRecommendedList(RecommendedList recommendedList) {
        this.recommendedList = recommendedList;
    }

    public SymmMatrix getSimilarityMatrix() {
        return similarityMatrix;
    }

    public void setSimilarityMatrix(SymmMatrix similarityMatrix) {
        this.similarityMatrix = similarityMatrix;
    }


    public Configuration getConf() {
        return conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
    }

}
