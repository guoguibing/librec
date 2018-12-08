package net.librec.increment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.filter.RecommendedFilter;
import net.librec.increment.rating.IncrementalBiasedMFRecommender;
import net.librec.job.RecommenderJob;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.UserKNNRecommender;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author logicxin
 */

public class IncrementalMFTestCase extends BaseTestCase {

    @Test
    public void unitTesting() throws Exception {

        String configFilePath ="rec/increment/rating/biasedmf-test.properties";

        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource(configFilePath);
        conf.addResource(resource);

        // build data model
        DataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();

        // set recommendation context
        //RecommenderContext context = new RecommenderContext(conf, dataModel);

        // training
        IncrementalBiasedMFRecommender recommender = new IncrementalBiasedMFRecommender();

        //recommender.setContext(context);
        //recommender.trainModel();

//        List<Entry<Integer, Double>> ratedItems = new ArrayList<>();
//        ratedItems.add(new SimpleEntry<>(0, 0.4d));
//        List<Integer> candidateItems = new ArrayList<>();
//        candidateItems.add(0);
//        candidateItems.add(1);
//
//        List<Entry<Integer, Double>> result = recommender.scoreItems(ratedItems, candidateItems);
    }

}
