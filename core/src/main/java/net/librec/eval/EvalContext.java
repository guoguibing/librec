package net.librec.eval;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.math.structure.*;
import net.librec.recommender.Recommender;
import net.librec.recommender.TensorRecommender;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.RecommenderSimilarity;

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
    private Map<String, RecommenderSimilarity> similarities = null;
    private Configuration conf;
    private Recommender recommender;

    public EvalContext(Configuration conf, RecommendedList recommendedList, SequentialAccessSparseMatrix testMatrix,
                       SymmMatrix similarityMatrix, Map<String, RecommenderSimilarity> similarities) {
        this.conf = conf;
        this.similarities = similarities;
        this.similarityMatrix = similarityMatrix;
        this.recommendedList = recommendedList;
        this.groundTruthList = getGroundTruthListFromSparseMatrix(testMatrix);
    }

    public EvalContext(Configuration conf, RecommendedList recommendedList, SequentialAccessSparseMatrix testMatrix) {
        this.conf = conf;
        this.recommendedList = recommendedList;
        this.groundTruthList = getGroundTruthListFromSparseMatrix(testMatrix);
    }

    public EvalContext(Configuration conf, Recommender recommender, DataSet testDataset) throws LibrecException {
        this.conf = conf;
        this.recommender = recommender;
        boolean isRanking = conf.getBoolean("rec.recommender.isranking");
        if (isRanking){
            recommendedList = recommender.recommendRank();
        } else {
            recommendedList = recommender.recommendRating(testDataset);
        }
        this.groundTruthList = getGroundTruthListFromDataSet(testDataset);
    }

    public EvalContext(Configuration conf, Recommender recommender, DataSet testDataset,
                       SymmMatrix similarityMatrix, Map<String, RecommenderSimilarity> similarities) throws LibrecException {
        this(conf, recommender, testDataset);
        this.similarities = similarities;
        this.similarityMatrix = similarityMatrix;
    }

    public RecommendedList getGroundTruthListFromDataSet(DataSet dataset) {
        if (this.recommender instanceof TensorRecommender){
            return getGroundTruthListFromSparseTensor((SparseTensor) dataset);
        } else {
            return getGroundTruthListFromSparseMatrix((SequentialAccessSparseMatrix) dataset);
        }
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

    public Map<String, RecommenderSimilarity> getSimilarities() {
        return similarities;
    }

    public void setSimilarities(Map<String, RecommenderSimilarity> similarities) {
        this.similarities = similarities;
    }

    public Configuration getConf() {
        return conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
    }
}
