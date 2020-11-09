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

/**
 * ** p% rule ** states that the ratio between the percentage of subjects having a certain sensitive attribute value
 * assigned the postive decision outcome and the percentage of subjects not having that value also assigned
 * the positive outcome should be no less than p%.
 * The rule implies that each group has a positive probability of <strong> at least p% </strong> of the other group.
 * the 100%-rule implies perfect removal of disparate impact on group=level fairness and a large value of p is preferred.
 *
 *  The final result should be greater than or equal to "p%" to be considered fair.
 *  min(a/b, b/a) >= p/100
 *
 *  a = P[Y=1|s=1] &
 *  b = P[Y=1|s=0]
 *
 * This is derived from the "80%-rule" supported by the U.S. Equal Employment Opportunity Commission.
 *
 *  * PPercentRuleEvaluator is based on the 80%-rule discussed by
 *  * Dan Biddle, <strong> Adverse Impact and Test Validation: A Practitioner's Guide to Valid amd Defensible
 *  * Employment Testing</strong> 2006 <br>
 *
 *  p% rule is discussed in
 *  * Zafar, Muhammad Bilal and Valera, Isabel and Rogrigez, Manuel Gomes and Gummadi, Krishna P,
 *  <strong>
 *      Fairness constraints: Mechanisms for Fair Classification
 *  </strong> AISTATS 2017 <br>
 *  **
 *
 * @author Nasim Sonboli
 */

public class PPercentRuleEvaluator extends AbstractRecommenderEvaluator {

    /** LOG */
    protected final Log LOG = LogFactory.getLog(this.getClass());

    /**
     * item feature matrix - indicating an item is associated to certain feature or not
     */
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
        int numUsers = groundTruthList.size();

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
            Set<Integer> testSetByUser = groundTruthList.getKeySetByContext(userID);

            if (testSetByUser.size() > 0) {
		        int unprotectedNum = 0;
                int protectedNum = 0;
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
                totalProtected += ((double)protectedNum/topK);
                totalUnprotected += ((double)unprotectedNum/topK);
                nonZeroUsers ++;
            }
        }
	    
        double protectedRatio =  (totalProtected / protectedSize);
        double unprotectedRatio = (totalUnprotected / unprotectedSize);

        // We could only use the number of pro/unpro and unpro/pro without considering the group sizes of + and - groups.
        double ppr = Math.min(protectedRatio/unprotectedRatio, unprotectedRatio/protectedRatio);
        return ppr;

    }
}
