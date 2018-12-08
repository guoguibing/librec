package net.librec.recommender.poi;

import com.google.common.collect.*;
import net.librec.common.LibrecException;
import net.librec.data.convertor.appender.LocationDataAppender;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.*;
import net.librec.math.structure.Vector;
import net.librec.math.structure.Vector.VectorEntry;
import net.librec.recommender.MatrixFactorizationRecommender;
import net.librec.recommender.item.KeyValue;
import net.librec.util.Lists;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Li, Xutao,Gao Cong, et al. "Rank-geofm: A ranking based geographical factorization method for point of interest recommendation." SIGIR2015
 * @author Yuanyuan Jin
 */
public class RankGeoFMRecommender extends MatrixFactorizationRecommender {
    /**
     * user latent factors for user-preference score
     */
    protected DenseMatrix userFactors;

    /**
     * user latent factors for geographical influence score
     */
    protected DenseMatrix geoUserFactors;

    /**
     * poi latent factors
     */
    protected DenseMatrix poiFactors;

    /**
     * the number of pois
     */
    protected int numPois;

    /**
     * weights between poi and its neighbors
     */
    protected SequentialAccessSparseMatrix poiKNNWeightMatrix;

    /**
     * margin for ranking
     */
    double epsilon;

    /**
     * radius for regularization
     */
    double C;

    /**
     * weight of the radius C
     */
    double alpha;

    /**
     * number of neighbors for each poi
     */
    int knn;

    /**
     * knn influence matrix for calculating the geographical influence score
     */
    DenseMatrix geoInfluenceMatrix;

    /**
     * array for converting value i into E[i], for loss calculation
     */
    double[] E;

    protected List<Set<Integer>> userPoisSet;
    KeyValue<Double, Double>[] locationCoordinates;


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        numPois = numItems;
        epsilon = conf.getDouble("rec.ranking.epsilon", 0.3d);
        C = conf.getDouble("rec.regularization.C", 1.0d);
        alpha = conf.getDouble("rec.regularization.alpha", 0.2d);
        knn = conf.getInt("rec.item.knn", 300);

        geoInfluenceMatrix = new DenseMatrix(numPois, numFactors);

        userFactors = new DenseMatrix(numUsers, numFactors);
        geoUserFactors = new DenseMatrix(numUsers, numFactors);
        poiFactors = new DenseMatrix(numPois, numFactors);

        double initStd = 0.1;
        userFactors.init(initMean, initStd);
        geoUserFactors.init(initMean, initStd);
        poiFactors.init(initMean, initStd);

        userPoisSet = getUserPoisSet(trainMatrix);
        locationCoordinates = ((LocationDataAppender) getDataModel().getDataAppender()).getLocationAppender();
        poiKNNWeightMatrix = getPoiKNNWeightMatrix(knn);

        E = new double[numPois + 1];
        for (int i = 1; i <= numPois; i++) {
            E[i] = E[i-1] + 1.0 / i ;
        }
    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= numIterations; iter++) {
            geoInfluenceMatrix = updateGeoInfluenceMatrix();
            loss = 0.0d;
            DenseMatrix tempUserFactors = new DenseMatrix(userFactors);
            DenseMatrix tempGeoUserFactors = new DenseMatrix(geoUserFactors);
            DenseMatrix tempPoiFactors = new DenseMatrix(poiFactors);

            int userIdx, posPoiIdx;
            double posRealRating;

            for (MatrixEntry trainMatrixEntry : trainMatrix) {
                userIdx = trainMatrixEntry.row();
                posPoiIdx = trainMatrixEntry.column();
                posRealRating = trainMatrixEntry.get();

                // for (userIdx, posPoiIdx), draw the negPoiIdx
                int sampleCount = 0;
                double posPredictRating = tempUserFactors.row(userIdx).dot(tempPoiFactors.row(posPoiIdx))
                        + tempGeoUserFactors.row(userIdx).dot(geoInfluenceMatrix.row(posPoiIdx));
                int negPoiIdx;
                double negPredictRating;
                double negRealRating;
                int incompatibility;
                while (true) {
                    negPoiIdx = Randoms.uniform(0, numPois);
                    negPredictRating = tempUserFactors.row(userIdx).dot(tempPoiFactors.row(negPoiIdx))
                            + tempGeoUserFactors.row(userIdx).dot(geoInfluenceMatrix.row(negPoiIdx));
                    Set<Integer> poisSet = userPoisSet.get(userIdx);

                    Map<Integer, Integer> poisPosList = new HashMap<>();
                    int[] poiIndices = trainMatrix.row(userIdx).getIndices();
                    for (int i = 0; i < poiIndices.length; i++) {
                        poisPosList.put(poiIndices[i], i);
                    }

                    if(poisSet.contains(negPoiIdx)) {
                        negRealRating = trainMatrix.row(userIdx).getAtPosition(poisPosList.get(negPoiIdx));
                    }else{
                        negRealRating = 0.0;
                    }

                    sampleCount++;
                    incompatibility = indicator(posRealRating, negRealRating) * indicator(negPredictRating + epsilon, posPredictRating);
                    if (incompatibility == 1 || sampleCount > numPois)
                        break;
                }

                if (incompatibility == 1) {
                    int lowerBound = numPois / sampleCount;
                    double s = Maths.logistic(negPredictRating + epsilon - posPredictRating);
                    loss += E[lowerBound] * s;
                    double uij = s * (1 - s);
                    double ita = E[lowerBound] * uij;

                    //update userFactors and geoUserFactors
                    DenseVector updateUserVec = (poiFactors.row(negPoiIdx).minus(poiFactors.row(posPoiIdx)))
                            .times(learnRate * ita);
                    userFactors.set(userIdx, userFactors.row(userIdx).minus(updateUserVec));
                    DenseVector updateGeoUserVec = (geoInfluenceMatrix.row(negPoiIdx).minus(geoInfluenceMatrix
                            .row(posPoiIdx))).times(learnRate * ita);
                    geoUserFactors.set(userIdx, geoUserFactors.row(userIdx).minus(updateGeoUserVec));

                    //update poiFactors
                    DenseVector updatePoiVec = userFactors.row(userIdx).times(learnRate * ita);
                    poiFactors.set(posPoiIdx, poiFactors.row(posPoiIdx).plus(updatePoiVec));
                    poiFactors.set(negPoiIdx, poiFactors.row(negPoiIdx).minus(updatePoiVec));

                    //regulize  userFactors and geoUserFactors
                    double userVectorNorm = userFactors.row(userIdx).norm(2);
                    if (userVectorNorm > C) {
                        userFactors.set(userIdx, userFactors.row(userIdx).times(C / userVectorNorm));
                    }
                    double geoUserVectorNorm = geoUserFactors.row(userIdx).norm(2);
                    if (geoUserVectorNorm > alpha * C) {
                        geoUserFactors.set(userIdx, geoUserFactors.row(userIdx).times(alpha * C / geoUserVectorNorm));
                    }

                    //regulize  poiFactors
                    double posPoiVectorNorm = poiFactors.row(posPoiIdx).norm(2);
                    if (posPoiVectorNorm > C) {
                        poiFactors.set(posPoiIdx, poiFactors.row(posPoiIdx).times(C / posPoiVectorNorm));
                    }
                    double negPoiVectorNorm = poiFactors.row(negPoiIdx).norm(2);
                    if (negPoiVectorNorm > C) {
                        poiFactors.set(negPoiIdx, poiFactors.row(negPoiIdx).times(C / negPoiVectorNorm));
                    }
                }
            }

            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    public SequentialAccessSparseMatrix getPoiKNNWeightMatrix(Integer kNearest) {
        SequentialAccessSparseMatrix poiKNNWeightMatrix;
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        for (int poiIdx = 0; poiIdx < numPois; poiIdx++) {
            List<Map.Entry<Integer, Double>> locationNeighbors = new ArrayList<>(numPois);
            KeyValue location = locationCoordinates[poiIdx];
            for (int neighborItemIdx = 0; neighborItemIdx < numPois; neighborItemIdx++) {
                if (poiIdx != neighborItemIdx) {
                    KeyValue neighborLocation = locationCoordinates[neighborItemIdx];
                    double distance = getDistance((double) location.getKey(), (double) location.getValue(), (double) neighborLocation.getKey(), (double) neighborLocation.getValue());
                    locationNeighbors.add(new AbstractMap.SimpleImmutableEntry<>(neighborItemIdx, distance));
                }
            }
            locationNeighbors = Lists.sortListTopK(locationNeighbors, false, kNearest);

            for (int index = 0; index < locationNeighbors.size(); index++) {
                int neighborItemIdx = locationNeighbors.get(index).getKey();
                double weight;
                if (locationNeighbors.get(index).getValue() < 0.5) {
                    weight = 1 / 0.5;
                } else {
                    weight = 1 / (locationNeighbors.get(index).getValue());
                }

                dataTable.put(poiIdx, neighborItemIdx, weight);
            }
        }

        poiKNNWeightMatrix = new SequentialAccessSparseMatrix(numPois, numPois, dataTable);
        //normalize poiKNNWeightMatrix for each row
        Table<Integer, Integer, Double> normalizedDataTable = HashBasedTable.create();
        for (int itemIdx = 0; itemIdx < numPois; itemIdx++) {
            double rowSum = poiKNNWeightMatrix.row(itemIdx).sum();
            Iterator<VectorEntry> colIterator = poiKNNWeightMatrix.row(itemIdx).iterator();
            while (colIterator.hasNext()) {
                Vector.VectorEntry vectorEntry = colIterator.next();
                normalizedDataTable.put(itemIdx, vectorEntry.index(), vectorEntry.get() / rowSum);
            }
        }
        poiKNNWeightMatrix = new SequentialAccessSparseMatrix(numPois, numPois, normalizedDataTable);
        return poiKNNWeightMatrix;
    }

    public DenseMatrix updateGeoInfluenceMatrix() throws LibrecException {
        DenseMatrix geoInfluenceMatrix = new DenseMatrix(numPois, numFactors);
        for (int poiIdx = 0; poiIdx < numPois; poiIdx++) {
            Iterator<VectorEntry> colItr = poiKNNWeightMatrix.row(poiIdx).iterator();
            while (colItr.hasNext()) {
                VectorEntry vectorEntry = colItr.next();
                geoInfluenceMatrix.set(poiIdx, geoInfluenceMatrix.row(poiIdx).plus(poiFactors.row(vectorEntry.index()).times(vectorEntry.get())));
            }
        }
        return  geoInfluenceMatrix;
    }

    /**
     * calculate the spherical distance between location(lat1, long1) and location (lat2, long2)
     * @param lat1
     * @param long1
     * @param lat2
     * @param long2
     * @return
     */
    public double getDistance(double lat1, double long1, double lat2, double long2) {
        double a, b, R;
        //earth radius
        R = 6378137;
        lat1 = lat1 * Math.PI / 180.0;
        lat2 = lat2 * Math.PI / 180.0;
        a = lat1 - lat2;
        b = (long1 - long2) * Math.PI / 180.0;
        double sina, sinb;
        sina = Math.sin(a / 2.0);
        sinb = Math.sin(b / 2.0);
        double distance;
        distance = 2 * R
                * Math.asin(Math.sqrt(sina * sina + Math.cos(lat1)
                * Math.cos(lat2) * sinb * sinb));
        return distance / 1000;
    }

    /**
     * indicator function, which returns 0 or 1
     * @param i
     * @param j
     * @return
     */
    public int indicator(double i, double j) {
        return  i > j ? 1 : 0;
    }

    @Override
    protected double predict(int userIdx, int poiIdx) {
        return userFactors.row(userIdx).dot(poiFactors.row(poiIdx)) + geoUserFactors.row(userIdx).dot(geoInfluenceMatrix.row(poiIdx));
    }

    private List<Set<Integer>> getUserPoisSet(SequentialAccessSparseMatrix sparseMatrix) {
        List<Set<Integer>> userPoisSet = new ArrayList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            int[] itemIndexes = sparseMatrix.row(userIdx).getIndices();
            Integer[] inputBoxed = ArrayUtils.toObject(itemIndexes);
            List<Integer> itemList = Arrays.asList(inputBoxed);
            userPoisSet.add(new HashSet(itemList));
        }
        return userPoisSet;
    }
}
