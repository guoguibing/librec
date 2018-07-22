package net.librec.increment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.filter.RecommendedFilter;
import net.librec.increment.rating.IncrementalBiasedMFRecommender;
import net.librec.job.RecommenderJob;
import net.librec.recommender.RecommenderContext;
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

public class IncrementalKNNTestCase extends BaseTestCase {

    @Test
    public void recommendObject() throws Exception {
        String configFilePath = "";
        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource(configFilePath);
        conf.addResource(resource);

        // build / model
        DataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();

        // set recommendation context
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        RecommenderSimilarity similarity = new PCCSimilarity();
        similarity.buildSimilarityMatrix(dataModel);
        context.setSimilarity(similarity);

        // training
        IncrementalBiasedMFRecommender recommender = new IncrementalBiasedMFRecommender();
        //recommender.recommend(context);

        // evaluation
        RecommenderEvaluator evaluator = new RMSEEvaluator();

        //recommender.evaluate(evaluator);

        // recommendation results
        //List recommendedItemList = recommender.getRecommendedList();
        RecommendedFilter filter = new GenericRecommendedFilter();
        //recommendedItemList = filter.filter(recommendedItemList);

    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * test the whole process of BaisedMF recommendation
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */

    @Test
    public void testIncrementBiasedMF() throws ClassNotFoundException, LibrecException, IOException {
        Configuration.Resource resource = new Configuration.Resource("rec/increment/rating/biasedmf-test.properties");
        conf.addResource(resource);
        RecommenderJob job = new RecommenderJob(conf);

        //job.runJob();
    }

    @Test
    public void unitTesting() throws Exception {

       // double globalMeanToInteger = Math.rint(-3.55);

        //
        Table<String, Integer, String> aTable = HashBasedTable.create();

        for (char a = 'A'; a <= 'B'; ++a) {
            for (Integer b = 1; b <= 3; ++b) {
                aTable.put(Character.toString(a), b, String.format("%c%d", a, b));
            }
        }

        int size = aTable.size();
        int rowSize = aTable.rowMap().size();
        aTable.columnMap().size();

        String configFilePath ="rec/increment/rating/biasedmf-test.properties";

        Configuration conf = new Configuration();
        Configuration.Resource resource = new Configuration.Resource(configFilePath);
        conf.addResource(resource);

        // build data model
        DataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();

        // set recommendation context
        RecommenderContext context = new RecommenderContext(conf, dataModel);
        //RecommenderSimilarity similarity = new PCCSimilarity();
        //similarity.buildSimilarityMatrix(dataModel);
        //context.setSimilarity(similarity);

        // training
        IncrementalBiasedMFRecommender recommender = new IncrementalBiasedMFRecommender();

        //recommender.recommend(context);
        recommender.setContext(context);
        recommender.trainModel();


        List<Entry<Integer, Double>> ratedItems = new ArrayList<>();
        ratedItems.add(new SimpleEntry<>(0, 0.4d));
        List<Integer> candidateItems = new ArrayList<>();
        candidateItems.add(0);
        candidateItems.add(1);

       // List<Entry<Integer, Double>> result = recommender.scoreItems(ratedItems, candidateItems);
    }

}
