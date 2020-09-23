package net.librec.eval.fairness;

import com.google.common.collect.BiMap;
import net.librec.data.DataModel;
import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Set;

public class StatisticalParityEvaluator extends AbstractRecommenderEvaluator {

    /** LOG */
    protected final Log LOG = LogFactory.getLog(this.getClass());

    /**
     * item feature matrix - indicating an item is associated to certain feature or not
     */
//    protected SparseMatrix itemFeatureMatrix;
    protected SequentialAccessSparseMatrix itemFeatureMatrix;

    public void setDataModel(DataModel datamodel) { // do we need this?
        super.setDataModel(datamodel);

    }

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     *         (number of protected items / protected group size) /
     *         (number of unprotected items / unprotected group size )
     */

//    public double evaluate(SparseMatrix testMatrix, RecommendedList recommendedList) {
    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {

        /**
         * construct protected and unprotected item set
         */
        itemFeatureMatrix = getDataModel().getFeatureAppender().getItemFeatures();
        BiMap<String, Integer> featureIdMapping = getDataModel().getFeatureAppender().getItemFeatureMap();
        BiMap<String, Integer> itemIdMapping = getDataModel().getItemMappingData();

        double totalProtected = 0.0;
        double totalUnprotected = 0.0;
//        int numUsers = testMatrix.numRows();
        int numUsers = groundTruthList.size();
//
//        int numItems = itemFeatureMatrix.numRows();
//        int numFeatures = itemFeatureMatrix.numColumns();
        int numItems = itemFeatureMatrix.rowSize();
        int numFeatures = itemFeatureMatrix.columnSize();

        String protectedAttribute = conf.get("data.protected.feature");
        if (conf != null && StringUtils.isNotBlank(conf.get("data.protected.feature"))) {
            protectedAttribute = conf.get("data.protected.feature");
        }
	    
        double protectedSize = 0.0;
        double unprotectedSize = 0.0;
        int proAttId = featureIdMapping.get(protectedAttribute);
        for (int itemId = 0; itemId < numItems; itemId++) {
            if(itemFeatureMatrix.get(itemId,proAttId) == 1) {
                protectedSize++;
            } else {
                unprotectedSize++;
            }
        }

        double nonZeroUsers = 0.0;
        for (int userID = 0; userID < numUsers; userID++) {
//            Set<Integer> testSetByUser = testMatrix.getColumnsSet(userID);
            Set<Integer> testSetByUser = groundTruthList.getKeySetByContext(userID);

            if (testSetByUser.size() > 0) {
		        int unprotectedNum = 0;
                int protectedNum = 0;
//                List<ItemEntry<Integer, Double>> recommendListByUser = recommendedList.getItemIdxListByUserIdx(userID);
                List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);

                // calculate rate
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int itemID = recommendListByUser.get(indexOfItem).getKey();
                        if(itemFeatureMatrix.get(itemID, proAttId) == 1) {
                            protectedNum++;
                        } else {
                            unprotectedNum++;
                        }
                }
                totalProtected += ((double)protectedNum)/topK;
                totalUnprotected += ((double)unprotectedNum)/topK;
                nonZeroUsers ++;
            }
        }


        // SP = (# protected items / protected group size) / (# unprotected items / unprotected group size)

        double protectedRatio =  totalProtected / nonZeroUsers;
        double unprotectedRatio = totalUnprotected / nonZeroUsers;

//        double relativeChance = protectedRatio / unprotectedRatio;
//        return relativeChance;

        return (protectedRatio - unprotectedRatio);
    }
}
