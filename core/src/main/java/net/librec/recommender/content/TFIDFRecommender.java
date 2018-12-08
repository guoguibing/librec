package net.librec.recommender.content;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.TensorEntry;
import net.librec.recommender.TensorRecommender;
import net.librec.similarity.CosineSimilarity;
import org.apache.commons.lang.StringUtils;

import java.util.*;


/**
 * Created by liuxz on 17-4-29.
 */
public class TFIDFRecommender extends TensorRecommender {

    private BiMap<String, Integer> reviewMappingData;
    private Map<String, Integer> word2DocNum = new HashMap<>();
    private Map<String, String> iwDict = new HashMap<String, String>();
    private Map<String, Double> idf = new HashMap<>();
    private Map<Integer, double[]> userVec = new HashMap<>();
    private Map<Integer, double[]> itemVec = new HashMap<>();
    private Table<Integer, Integer, double[]> featureVec_test = HashBasedTable.create();
    private CosineSimilarity similarity = new CosineSimilarity();

    private double smooth;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        smooth = conf.getDouble("rec.tfidf.smooth", 1D);
        reviewMappingData = DataFrame.getInnerMapping("review");
        int numberOfWords = 0;
        // build review matrix and counting the number of words
        Table<Integer, Integer, String> res = HashBasedTable.create();

        Set<String> wordSet = new HashSet<>();
        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.inverse().get(reviewIndex);
            String[] fReviewContent = reviewContent.split(":");
            for (String word : fReviewContent) {
                if ((!iwDict.containsKey(word)) && StringUtils.isNotEmpty(word)) {
                    iwDict.put(word, String.valueOf(numberOfWords));
                    numberOfWords++;
                }
                wordSet.add(iwDict.get(word));
            }
            for (String word : wordSet) {
                if (!word2DocNum.containsKey(word)) {
                    word2DocNum.put(word, 1);
                } else {
                    word2DocNum.put(word, word2DocNum.get(word) + 1);
                }
            }
            wordSet.clear();
            res.put(userIndex, itemIndex, reviewContent);
        }

        for (TensorEntry te : testTensor) {
            int[] entryKeys = te.keys();
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.inverse().get(reviewIndex);
            String[] fReviewContent = reviewContent.split(":");
            for (String word : fReviewContent) {
                if (!iwDict.containsKey(word) && StringUtils.isNotEmpty(word)) {
                    iwDict.put(word, String.valueOf(numberOfWords));
                    numberOfWords++;
                }
            }
        }

        for (String word : word2DocNum.keySet()) {
            idf.put(word, Math.log10((res.size() / (word2DocNum.get(word) + smooth))));
        }

        Table<Integer, Integer, double[]> featureVec = HashBasedTable.create();
        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int userIndex = entryKeys[0];
            int itemIndex = entryKeys[1];
            featureVec.put(userIndex, itemIndex, calcFeatureVector(reviewMappingData.inverse().get(entryKeys[2])));
        }

        Map<Integer, Integer> userCount = new HashMap<>();
        Map<Integer, Integer> itemCount = new HashMap<>();

        for (Table.Cell<Integer, Integer, double[]> x : featureVec.cellSet()) {
            userCount.put(x.getRowKey(), userCount.containsKey(x.getRowKey()) ? userCount.get(x.getRowKey()) + 1 : 1);
            itemCount.put(x.getColumnKey(), itemCount.containsKey(x.getColumnKey()) ? itemCount.get(x.getColumnKey()) + 1 : 1);
            if (userVec.containsKey(x.getRowKey())) {
                addVec(userVec.get(x.getRowKey()), x.getValue());
            } else {
                userVec.put(x.getRowKey(), new double[x.getValue().length]);
                addVec(userVec.get(x.getRowKey()), x.getValue());
            }
            if (itemVec.containsKey(x.getColumnKey())) {
                addVec(itemVec.get(x.getColumnKey()), x.getValue());
            } else {
                itemVec.put(x.getColumnKey(), new double[x.getValue().length]);
                addVec(itemVec.get(x.getColumnKey()), x.getValue());
            }
        }


        for (int item : itemVec.keySet()) {
            divVec(itemVec.get(item), itemCount.get(item));
        }
        for (int user : userVec.keySet()) {
            divVec(userVec.get(user), userCount.get(user));
        }
    }

    @Override
    protected void trainModel() throws LibrecException {

    }

    @Override
    protected double predict(int[] keys) throws LibrecException {
        return 0;
    }

    protected double predict(int user, int item) {
        double sim = getSimilarity(userVec.get(user),
                itemVec.get(item));
        return sim;
    }

    double[] calcFeatureVector(String reviewContent) {
        double[] featureVector = new double[word2DocNum.size()];
        String[] content = reviewContent.split(":");

        Map<String, Integer> termNumber = new HashMap<>();
        for (String word : content) {
            if (!termNumber.containsKey(iwDict.get(word))) {
                termNumber.put(iwDict.get(word), 1);
            } else {
                termNumber.put(iwDict.get(word), termNumber.get(iwDict.get(word)) + 1);
            }
        }
        for (String word : termNumber.keySet()) {
            if ((word != null) && idf.keySet().contains(word) && (idf.get(word) != null)) {
                featureVector[Integer.parseInt(word)] = ((double)(termNumber.get(word)) / content.length) * idf.get(word);
            }
        }
        return featureVector;
    }

    private void addVec(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] + b[i];
        }
    }

    private void divVec(double[] a, int de) {
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] / de;
        }
    }

    protected double getSimilarity(double[] thisList, double[] thatList) {
        if (thisList == null || thatList == null || thisList.length < 1 || thatList.length < 1 ||
                thisList.length != thatList.length) {
            return Double.NaN;
        }

        double innerProduct = 0.0, thisPower2 = 0.0, thatPower2 = 0.0;
        for (int i = 0; i < thisList.length; i++) {
            innerProduct += thisList[i] * thatList[i];
            thisPower2 += thisList[i] * thisList[i];
            thatPower2 += thatList[i] * thatList[i];
        }
        return innerProduct / Math.sqrt(thisPower2 * thatPower2);
    }
}
