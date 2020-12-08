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

public class CStatisticalParityEvaluator extends AbstractRecommenderEvaluator {

    /** LOG */
    protected final Log LOG = LogFactory.getLog(this.getClass());

    /**
     * item feature matrix - indicating an item is associated to certain feature or not
     */
//    protected SparseMatrix itemFeatureMatrix;
    protected SequentialAccessSparseMatrix userFeatureMatrix;

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
        userFeatureMatrix = getDataModel().getFeatureAppender().getUserFeatures();
        BiMap<String, Integer> featureIdMapping = getDataModel().getFeatureAppender().getUserFeatureMap();

        int numUsers = groundTruthList.size();
        int numFeatures = userFeatureMatrix.columnSize();

        int proAttId = 0;
        //protected users
        String protectedAttribute = "";
        if (conf != null && StringUtils.isNotBlank(conf.get("data.protected.feature"))) {
            protectedAttribute = conf.get("data.protected.feature");
            proAttId = featureIdMapping.get(protectedAttribute);
        } else {
            return 0;
        }


        double protectedSize = 0.0;
        double unprotectedSize = 0.0;

//        double totalPrecision = 0.0;
        double totalPrecisionPro = 0.0;
        double totalPrecisionUnpro = 0.0;
//        double nonZeroUsers = 0.0;
        for (int userID = 0; userID < numUsers; userID++) {

            double userPrecision = 0.0;
            Set<Integer> testSetByUser = groundTruthList.getKeySetByContext(userID);

            if (testSetByUser.size() > 0) {
                List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);

                int numHits = 0;
                // calculate rate
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int itemID = recommendListByUser.get(indexOfItem).getKey();
                    if (testSetByUser.contains(itemID)) {
                        numHits++;
                    }
                }
                // remove +=, since it's not an aggregate measure, it's for each user
                userPrecision = numHits / (this.topN + 0.0);
                if (userFeatureMatrix.get(userID, proAttId) == 1) {
                    totalPrecisionPro += userPrecision;
                    protectedSize++;
                } else {
                    totalPrecisionUnpro += userPrecision;
                    unprotectedSize++;
                }
//                totalPrecision += numHits / (this.topN + 0.0);
//                nonZeroUsers ++;
            }
        }

        double res = 0.0;
        if (protectedSize == 0 && unprotectedSize == 0) {
            res = 0.0;
        }
        if (protectedSize == 0 && unprotectedSize > 0) {
            res = -1.0*(totalPrecisionUnpro / unprotectedSize);
        }
        if (protectedSize > 0 && unprotectedSize == 0) {
            res = (totalPrecisionPro / protectedSize)*1.0;
        }
        if (protectedSize > 0 && unprotectedSize > 0) {
            res = ((totalPrecisionPro / protectedSize) - (totalPrecisionUnpro / unprotectedSize))*1.0;
        }

        return res;
    }
}
