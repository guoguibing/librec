package net.librec.recommender.poi;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.Ints;
import net.librec.common.LibrecException;
import net.librec.data.convertor.appender.LocationDataAppender;
import net.librec.data.structure.AbstractBaseDataEntry;
import net.librec.data.structure.LibrecDataList;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Ye M, Yin P, Lee W C, et al. Exploiting geographical influence for collaborative point-of-interest recommendation[C]//
 * International ACM SIGIR Conference on Research and Development in Information Retrieval. ACM, 2011:325-334.
 * @author Yuanyuan Jin
 *
 * ###special notes###
 * 1. prediction for all user, please set:
 * data.testset.path = poi/Gowalla/checkin/Gowalla_test.txt
 * and delete the para setting for "rec.limit.userNum" in usg.properties
 *
 * 2. prediction for small user set like userids in [0, 100],
 * in usg.properties, please set:
 * data.testset.path = poi/Gowalla/checkin/testDataFor101users.txt
 * rec.limit.userNum = 101
 * In EntropyEvaluator and NoveltyEvaluator, you also need to reset the variable "numUsers" = your limited userNum
 */
public class USGRecommender extends AbstractRecommender {
    private SequentialAccessSparseMatrix socialSimilarityMatrix;
    private SequentialAccessSparseMatrix userSimilarityMatrix;
    private SequentialAccessSparseMatrix socialMatrix;
    private SequentialAccessSparseMatrix trainMatrix;
    private SequentialAccessSparseMatrix testMatrix;
    /**
     * weight of the social score part
     */
    private double alpha;

    /**
     * weight of the geographical score part
     */
    private double beta;

    /**
     * tuning parameter in social similarity
     */
    private double eta;

    /**
     * linear coefficients for modeling the "log-log scale" power-law distribution
     */
    private double w0;
    private double w1;

    /**
     * number of pois
     */
    private int numPois;

    /**
     * number of users
     */
    private int numUsers;

    /**
     * for limiting test user cardinality
     */
    private int limitUserNum;

    private static final int BSIZE = 1024 * 1024;
    private String socialPath;
    private KeyValue<Double, Double>[] locationCoordinates;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        BiMap<Integer, String> userIds = this.userMappingData.inverse();
        BiMap<Integer, String> itemIds = this.itemMappingData.inverse();

        numPois = itemMappingData.size();
        numUsers = userMappingData.size();

        trainMatrix = (SequentialAccessSparseMatrix) getDataModel().getTrainDataSet();
        testMatrix = (SequentialAccessSparseMatrix) getDataModel().getTestDataSet();

        alpha = conf.getDouble("rec.alpha", 0.1d);
        beta = conf.getDouble("rec.beta", 0.1d);
        eta = conf.getDouble("rec.eta", 0.05d);
        //default value is numUsers
        limitUserNum = conf.getInt("rec.limit.userNum", numUsers);
        locationCoordinates = ((LocationDataAppender) getDataModel().getDataAppender()).getLocationAppender();
        userSimilarityMatrix = context.getSimilarity().getSimilarityMatrix().toSparseMatrix();
        socialPath = conf.get("dfs.data.dir") + "/" + conf.get("data.social.path");

        // for AUCEvaluator and nDCGEvaluator
        int[] numDroppedItemsArray = new int[numUsers];
        int maxNumTestItemsByUser = 0;
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            numDroppedItemsArray[userIdx] = numPois - trainMatrix.row(userIdx).getNumEntries();
            int numTestItemsByUser = testMatrix.row(userIdx).getNumEntries();
            maxNumTestItemsByUser = maxNumTestItemsByUser < numTestItemsByUser ? numTestItemsByUser : maxNumTestItemsByUser;
        }
        conf.setInts("rec.eval.auc.dropped.num", numDroppedItemsArray);
        conf.setInt("rec.eval.key.test.max.num", maxNumTestItemsByUser);

        // for EntropyEvaluator
        conf.setInt("rec.eval.item.num", testMatrix.columnSize());

        // for NoveltyEvaluator
        int[] itemPurchasedCount = new int[numPois];
        for (int itemIdx = 0; itemIdx < numPois; ++itemIdx) {
            int userNum = 0;
            int[] userArray = trainMatrix.column(itemIdx).getIndices();
            for (int userIdx : userArray) {
                if (userIdx >= 0 && userIdx < limitUserNum) {
                    userNum++;
                }
            }
            userArray = testMatrix.column(itemIdx).getIndices();
            for (int userIdx : userArray) {
                if (userIdx >= 0 && userIdx < limitUserNum) {
                    userNum++;
                }
            }
            itemPurchasedCount[itemIdx] = userNum;
        }
        conf.setInts("rec.eval.item.purchase.num", itemPurchasedCount);
    }

    @Override
    protected void trainModel() throws LibrecException {
        LOG.info("start buliding socialmatrix" + new Date());
        try {
            buildSocialMatrix(socialPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOG.info("start buliding socialSimilarityMatrix" + new Date());
        buildSocialSimilarity();

        LOG.info("start fitting the powerlaw distribution" + new Date());
        fitPowerLaw();
    }

    public double[] predictScore(int userIdx, int itemIdx) {
        //score array for three aspects: user preference, social influence and  geographical influence
        double[] predictScore = new double[]{0.0d, 0.0d, 0.0d};

        int[] userArray = trainMatrix.column(itemIdx).getIndices();
        List<Integer> userList = Ints.asList(userArray);

        /*---------start user preference socre calculation--------*/
        //iterator to iterate other similar users for each user
        Iterator<Vector.VectorEntry> userSimIter = userSimilarityMatrix.row(userIdx).iterator();

        //similarities between userIdx and its neighbors
        List<Double> neighborSimis = new ArrayList<>();
        while (userSimIter.hasNext()) {
            Vector.VectorEntry userRatingEntry = userSimIter.next();
            int similarUserIdx = userRatingEntry.index();
            if (!userList.contains(similarUserIdx)) {
                continue;
            }
            neighborSimis.add(userRatingEntry.get());
        }
        if (neighborSimis.size() == 0) {
            predictScore[0] = 0.0d;
        } else {
            double sum = 0.0d;
            for (int i = 0; i < neighborSimis.size(); i++) {
                sum += neighborSimis.get(i);
            }
            predictScore[0] = sum;
        }
        /*---------end user preference socre calculation--------*/

        /*---------start social influence socre calculation--------*/
        //social similarities between userIdx and its social neighbors
        List<Double> socialNeighborSimis = new ArrayList<>();
        Iterator<Vector.VectorEntry> friendIter = socialSimilarityMatrix.row(userIdx).iterator();
        while (friendIter.hasNext()) {
            Vector.VectorEntry userRatingEntry = friendIter.next();
            int similarUserIdx = userRatingEntry.index();
            if (!userList.contains(similarUserIdx)) {
                continue;
            }
            socialNeighborSimis.add(userRatingEntry.get());
        }
        if (socialNeighborSimis.size() == 0) {
            predictScore[1] = 0.0d;
        } else {
            double sum = 0.0d;
            for (int i = 0; i < socialNeighborSimis.size(); i++) {
                sum += socialNeighborSimis.get(i);
            }
            predictScore[1] = sum;
        }
        /*---------end social influence socre calculation--------*/

        /*---------start geo influence socre calculation--------*/
        double geoScore = 1.0d;
        int[] itemList = trainMatrix.row(userIdx).getIndices();
        if (itemList.length == 0) {
            geoScore = 0.0d;
        } else {
            for (int visitedPOI : itemList) {
                double distance = getDistance(locationCoordinates[visitedPOI].getKey(), locationCoordinates[visitedPOI].getValue(),
                        locationCoordinates[itemIdx].getKey(), locationCoordinates[itemIdx].getValue());
                if (distance < 0.01) {
                    distance = 0.01;
                }
                geoScore *= w0 * Math.pow(distance, w1);
            }
        }
        predictScore[2] = geoScore;
        /*---------end geo influence socre calculation--------*/

        return predictScore;
    }

    public void buildSocialSimilarity() {
        Table<Integer, Integer, Double> socialSimilarityTable = HashBasedTable.create();
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            SequentialSparseVector userVector = trainMatrix.row(userIdx);
            if (userVector.getNumEntries() == 0) {
                continue;
            }
            int[] socialNeighborList = socialMatrix.column(userIdx).getIndices();
            for (int socialNeighborIdx : socialNeighborList) {
                if (userIdx < socialNeighborIdx) {
                    SequentialSparseVector socialVector = trainMatrix.row(socialNeighborIdx);
                    int[] friendList = socialMatrix.column(socialNeighborIdx).getIndices();
                    if (socialVector.getNumEntries() == 0 || friendList.length == 0) {
                        continue;
                    }
                    if (getCorrelation(userVector, socialVector) > 0.0 && getCorrelation(socialNeighborList, friendList) > 0.0) {
                        double sim = (1 - eta) * getCorrelation(userVector, socialVector) + eta * getCorrelation(socialNeighborList, friendList);
                        if (!Double.isNaN(sim) && sim != 0.0) {
                            socialSimilarityTable.put(userIdx, socialNeighborIdx, sim);
                        }
                    }
                }
            }
        }
        socialSimilarityMatrix = new SequentialAccessSparseMatrix(numUsers, numUsers, socialSimilarityTable);
    }

    /**
     * fit the "log-log" scale power law distribution
     */
    public void fitPowerLaw() {
        Map<Integer, Double> distanceMap = new HashMap<>();
        Map<Double, Double> logdistanceMap = new HashMap<>();
        int pairNum = 0;

        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            int[] itemList = trainMatrix.row(userIdx).getIndices();
            if (itemList.length == 0) {
                continue;
            }

            for (int i = 0; i < itemList.length - 1; i++) {
                for (int j = i + 1; j < itemList.length; j++) {
                    double distance = getDistance(locationCoordinates[itemList[i]].getKey(), locationCoordinates[itemList[i]].getValue(),
                            locationCoordinates[itemList[j]].getKey(), locationCoordinates[itemList[j]].getValue());
                    if ((int) distance > 0) {
                        int intDistance = (int) distance;
                        if (!distanceMap.containsKey(intDistance)) {
                            distanceMap.put(intDistance, 0.0d);
                        }
                        distanceMap.put(intDistance, distanceMap.get(intDistance) + 1.0d);
                    }
                    pairNum++;
                }
            }
        }

        for (Map.Entry<Integer, Double> distanceEntry : distanceMap.entrySet()) {
            logdistanceMap.put(Math.log10(distanceEntry.getKey()), Math.log10(distanceEntry.getValue() * 1.0 / pairNum));
        }

        /*-------start gradient descent--------*/
        w0 = Randoms.random();
        w1 = Randoms.random();
        //regularization coefficient
        double reg = 0.1;
        //learn rate
        double lrate = 0.00001;
        //max number of iterations
        int maxIterations = 2000;

        for (int i = 0; i < maxIterations; i++) {
            //gradients of w0 and w1
            double w0Gradient = 0.0d;
            double w1Gradient = 0.0d;

            for (Map.Entry<Double, Double> distanceEntry : logdistanceMap.entrySet()) {
                double distance = distanceEntry.getKey();
                double probability = distanceEntry.getValue();
                w0Gradient += (w0 + w1 * distance - probability);
                w1Gradient += (w0 + w1 * distance - probability) * distance;
            }
            w0 -= lrate * (w0Gradient + reg * w0);
            w1 -= lrate * (w1Gradient + reg * w1);
        }
        /*-------end gradient descent--------*/

        w0 = Math.pow(10, w0);
    }

    /**
     * calculate the spherical distance between location(lat1, long1) and location (lat2, long2)
     * @param lat1
     * @param long1
     * @param lat2
     * @param long2
     * @return
     */
    protected double getDistance(Double lat1, Double long1, Double lat2, Double long2) {
        if (Math.abs(lat1 - lat2) < 1e-6 && Math.abs(long1 - long2) < 1e-6) {
            return 0.0d;
        }
        double degreesToRadius = Math.PI / 180.0;
        double phi1 = (90.0 - lat1) * degreesToRadius;
        double phi2 = (90.0 - lat2) * degreesToRadius;
        double theta1 = long1 * degreesToRadius;
        double theta2 = long2 * degreesToRadius;
        double cos = (Math.sin(phi1) * Math.sin(phi2) * Math.cos(theta1 - theta2) +
                Math.cos(phi1) * Math.cos(phi2));
        double arc = Math.acos(cos);
        double earthRadius = 6371;
        return arc * earthRadius;
    }

    public double getCorrelation(SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
        // compute jaccard similarity
        Set<Integer> elements = unionArrays(thisVector.getIndices(), thatVector.getIndices());
        int numAllElements = elements.size();
        int numCommonElements = thisVector.getIndices().length + thatVector.getIndices().length - numAllElements;
        return (numCommonElements + 0.0) / numAllElements;
    }

    public Set<Integer> unionArrays(int[] arr1, int[] arr2) {
        Set<Integer> set = new HashSet<>();
        for (int num : arr1) {
            set.add(num);
        }
        for (int num : arr2) {
            set.add(num);
        }
        return set;
    }

    public double getCorrelation(int[] thisList, int[] thatList) {
        // compute jaccard similarity
        Set<Integer> elements = new HashSet<Integer>();
        for (int num : thisList) {
            elements.add(num);
        }
        for (int num : thatList) {
            elements.add(num);
        }
        int numAllElements = elements.size();
        int numCommonElements = thisList.length + thatList.length
                - numAllElements;
        return (numCommonElements + 0.0) / numAllElements;
    }

    @Override
    public RecommendedList recommendRating(DataSet predictDataSet) throws LibrecException {
        return null;
    }

    @Override
    public RecommendedList recommendRating(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
        return null;
    }

    @Override
    public RecommendedList recommendRank() throws LibrecException {
        LOG.info("Eveluate for users from id 0 to id\t" + (limitUserNum-1));
        RecommendedList recommendedList = new RecommendedList(numUsers);
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            recommendedList.addList(new ArrayList<>());
        }

        List<Integer> userList = new ArrayList<>();
        for (int userIdx = 0; userIdx < limitUserNum; ++userIdx) {
            userList.add(userIdx);
        }

        userList.parallelStream().forEach((Integer userIdx) -> {
            List<Integer> itemList = Ints.asList(trainMatrix.row(userIdx).getIndices());
            List<KeyValue<Integer, double[]>> tempItemValueList = new ArrayList<>();
            double[] maxScore = new double[]{0.0d, 0.0d, 0.0d};
            for (int itemIdx = 0; itemIdx < numPois; ++itemIdx) {
                if (!itemList.contains(itemIdx)) {
                    double[] predictRating = predictScore(userIdx, itemIdx);
                    if (predictRating[0] >= maxScore[0]) {
                        maxScore[0] = predictRating[0];
                    }
                    if (predictRating[1] >= maxScore[1]) {
                        maxScore[1] = predictRating[1];
                    }
                    if (predictRating[2] >= maxScore[2]) {
                        maxScore[2] = predictRating[2];
                    }
                    tempItemValueList.add(new KeyValue<>(itemIdx, new double[]{predictRating[0], predictRating[1], predictRating[2]}));
                }
            }

            List<KeyValue<Integer, Double>> itemValueList = new ArrayList<>();

            //normalize scores
            for (KeyValue<Integer, double[]> entry : tempItemValueList) {
                double[] scores = entry.getValue();
                if (maxScore[0] != 0.0d) {
                    scores[0] = scores[0] / maxScore[0];
                }
                if (maxScore[1] != 0.0d) {
                    scores[1] = scores[1] / maxScore[1];
                }
                if (maxScore[2] != 0.0d) {
                    scores[2] = scores[2] / maxScore[2];
                }
                double predictRating = (1 - alpha - beta) * scores[0] + alpha * scores[1]
                        + beta * scores[2];
                itemValueList.add(new KeyValue<>(entry.getKey(), predictRating));
            }

            recommendedList.setList(userIdx, itemValueList);
            recommendedList.topNRankByIndex(userIdx, topN);
        });
        if (recommendedList.size() == 0) {
            throw new IndexOutOfBoundsException("No item is recommended, there is something error in the recommendation algorithm! Please check it!");
        }
        LOG.info("end recommendation");
        return recommendedList;
    }

    @Override
    public RecommendedList recommendRank(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
        return null;
    }

    /**
     * load social relation data
     * @param inputDataPath
     * @throws IOException
     */
    private void buildSocialMatrix(String inputDataPath) throws IOException {
        LOG.info("Now loading users' social relation data success! " + socialPath);
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        final List<File> files = new ArrayList<File>();
        final ArrayList<Long> fileSizeList = new ArrayList<Long>();
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileSizeList.add(file.toFile().length());
                files.add(file.toFile());
                return super.visitFile(file, attrs);
            }
        };
        Files.walkFileTree(Paths.get(inputDataPath), finder);
        long allFileSize = 0;
        for (Long everyFileSize : fileSizeList) {
            allFileSize = allFileSize + everyFileSize.longValue();
        }
        for (File dataFile : files) {
            FileInputStream fis = new FileInputStream(dataFile);
            FileChannel fileRead = fis.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(BSIZE);
            int len;
            String bufferLine = new String();
            byte[] bytes = new byte[BSIZE];
            while ((len = fileRead.read(buffer)) != -1) {
                buffer.flip();
                buffer.get(bytes, 0, len);
                bufferLine = bufferLine.concat(new String(bytes, 0, len)).replaceAll("\r", "\n");
                String[] bufferData = bufferLine.split("(\n)+");
                boolean isComplete = bufferLine.endsWith("\n");
                int loopLength = isComplete ? bufferData.length : bufferData.length - 1;
                for (int i = 0; i < loopLength; i++) {
                    String line = new String(bufferData[i]);
                    String[] data = line.trim().split("[ \t,]+");
                    String userA = data[0];
                    String userB = data[1];
                    Double rate = (data.length >= 3) ? Double.valueOf(data[2]) : 1.0;
                    if (this.userMappingData.containsKey(userA) && this.userMappingData.containsKey(userB)) {
                        int row = this.userMappingData.get(userA);
                        int col = this.userMappingData.get(userB);
                        dataTable.put(row, col, rate);
                        dataTable.put(col, row, rate);
                    }
                }
                if (!isComplete) {
                    bufferLine = bufferData[bufferData.length - 1];
                }
                buffer.clear();
            }
            fileRead.close();
            fis.close();
        }
        int numRows = this.userMappingData.size(), numCols = this.userMappingData.size();
        socialMatrix = new SequentialAccessSparseMatrix(numRows, numCols, dataTable);
        dataTable = null;
        LOG.info("Load users' social relation data success! " + socialPath);
    }
}
