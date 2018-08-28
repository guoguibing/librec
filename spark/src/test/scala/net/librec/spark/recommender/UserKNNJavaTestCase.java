package net.librec.spark.recommender;

import net.librec.recommender.item.RecommendedList;
import net.librec.spark.*;
import net.librec.spark.data.DataConverter;
import net.librec.spark.data.Rating;
import net.librec.spark.math.structure.IndexedVector;
import net.librec.spark.rdd.SimilarityFunctions;
import net.librec.spark.rdd.SplitterFunctions;
import net.librec.spark.rdd.StatisticalFunctions;
import net.librec.spark.recommender.cf.UserKNN;
import org.apache.spark.rdd.RDD;

/**
 * UserKNNJavaTestCase.
 *
 * @author Zhang Chaoli
 */
public class UserKNNJavaTestCase {

    public static void main(String[] args) {

        LibrecConf conf = new LibrecConf().setMaster("local[*]").setAppName("UserKNNJava");
        LibrecContext lc = new LibrecContext(conf);
        RDD<Rating> data = new DataConverter(lc).convertText("/Users/clzhang/Documents/IntelliJIDEA_program/librec/librec_3.0.0_matrix/data/spark/ratings.txt", " ");
        RDD<Rating>[] algoData = new SplitterFunctions(data).splitByRatio((new double[]{0.8, 0.2}), "rating", 1000);
        RDD<Rating> trainData = algoData[0];
        RDD<Rating> testData = algoData[1];

        RDD<IndexedVector> indexedVectorRDD = StatisticalFunctions.toIndexedSparseVectors(data, "user");
        RDD similarity = (new SimilarityFunctions(indexedVectorRDD)).computeSimilarity(Correlation.BCOS(), conf);
        UserKNN userKNN = new UserKNN(10, true, trainData, similarity);
        userKNN.train();

//        JavaConverters.asScalaBufferConverter(predictArr).asScala().toSeq();
        RDD<Rating> predictResult = userKNN.predict(testData);
        for (Rating rating : predictResult.toJavaRDD().take(10)) {
            System.out.println("---predictResult: " + rating.user() + ", " + rating.item() + ", " + rating.rate());
        }

        int predictResultNum = (int) predictResult.count();
        RecommendedList groundTruthList = new RecommendedList(predictResultNum, true);
        for (Rating rating : testData.toJavaRDD().collect()) {
            groundTruthList.addIndependently(rating.user(), rating.item(), rating.rate());
        }
        RecommendedList recommendedList = new RecommendedList(predictResultNum);
        for (Rating rating : predictResult.toJavaRDD().collect()) {
            recommendedList.addIndependently(rating.user(), rating.item(), rating.rate());
        }
        conf.setInt("rec.recommender.ranking.topn", 5);
        System.out.println("---Evaluator AP: " + Evaluator.eval(Measure.AP(), groundTruthList, recommendedList, conf));
        conf.setInt("rec.recommender.ranking.topn", 100);
        System.out.println("---Evaluator IDCG: " + Evaluator.eval(Measure.IDCG(), groundTruthList, recommendedList, conf));

        lc.stop();
    }
}
