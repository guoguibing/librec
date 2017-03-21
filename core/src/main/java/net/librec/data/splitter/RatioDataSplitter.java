/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.splitter;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.VectorEntry;
import net.librec.util.RatingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ratio Data Splitter.<br>
 * Split dataset into train set, test set, valid set by ratio.<br>
 *
 * @author WangYuFeng and Liuxz
 */
public class RatioDataSplitter extends AbstractDataSplitter {

    /** The rate dataset for splitting */
    private SparseMatrix preferenceMatrix;

    /** The datetime dataset for splitting */
    private SparseMatrix datetimeMatrix;

    /**
     * Empty constructor.
     */
    public RatioDataSplitter() {
    }

    /**
     * Initializes a newly created {@code RatioDataSplitter} object
     * with convertor and configuration.
     *
     * @param dataConvertor
     *          the convertor for the splitter.
     * @param conf
     *          the configuration for the splitter.
     */
    public RatioDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /**
     * Split the dataset according to the configuration file.<br>
     *
     * @throws LibrecException if error occurs
     */
    @Override
    public void splitData() throws LibrecException {
        this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
        this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
        String splitter = conf.get("data.splitter.ratio");
        switch (splitter.toLowerCase()) {
            case "rating": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByRating(ratio);
                break;
            }
            case "user": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByUser(ratio);
                break;
            }
            case "userfixed": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getFixedRatioByUser(ratio);
                break;
            }
            case "item": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByItem(ratio);
                break;
            }
            case "valid": {
                double trainRatio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                double validationRaito = Double.parseDouble(conf.get("data.splitter.validset.ratio"));
                getRatio(trainRatio, validationRaito);
                break;
            }
            case "ratingdate": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByRatingDate(ratio);
                break;
            }
            case "userdate": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByUserDate(ratio);
                break;
            }
            case "itemdate": {
                double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
                getRatioByItemDate(ratio);
                break;
            }
        }
    }

    /**
     * Split ratings into two parts: (ratio) training, (1-ratio) test subsets.
     *
     * @param ratio the ratio of training data over all the ratings
     */
    public void getRatioByRating(double ratio) {
        if (ratio > 0 && ratio < 1) {

            testMatrix = new SparseMatrix(preferenceMatrix);
            trainMatrix = new SparseMatrix(preferenceMatrix);

            for(MatrixEntry matrixEntry: preferenceMatrix){
                int userIdx = matrixEntry.row();
                int itemIdx = matrixEntry.column();

                double rdm = Randoms.uniform();

                if (rdm < ratio) {
                    testMatrix.set(userIdx, itemIdx, 0.0);
                } else {
                    trainMatrix.set(userIdx, itemIdx, 0.0);
                }
            }

            SparseMatrix.reshape(testMatrix);
            SparseMatrix.reshape(trainMatrix);
        }
    }

    /**
     * Split the ratings (by date) into two parts: (ratio) training, (1-ratio)
     * test subsets.
     *
     * @param ratio the ratio of training data
     */
    public void getRatioByRatingDate(double ratio) {
        if (ratio > 0 && ratio < 1) {

            testMatrix = new SparseMatrix(preferenceMatrix);
            trainMatrix = new SparseMatrix(preferenceMatrix);

            List<RatingContext> rcs = new ArrayList<>(datetimeMatrix.size());
            for (MatrixEntry me : preferenceMatrix)
                rcs.add(new RatingContext(me.row(), me.column(), (long) datetimeMatrix.get(me.row(), me.column())));
            Collections.sort(rcs);

            int trainSize = (int) (rcs.size() * ratio);
            for (int i = 0; i < rcs.size(); i++) {
                RatingContext rc = rcs.get(i);
                int u = rc.getUser();
                int j = rc.getItem();

                if (i < trainSize)
                    testMatrix.set(u, j, 0.0);
                else
                    trainMatrix.set(u, j, 0.0);
            }

            rcs = null;
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code ratio} percentage of ratings are preserved for each
     * user, and the rest are used as the testing data.
     *
     * @param ratio  the ratio of training data
     */
    public void getRatioByUser(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {

                List<Integer> items = preferenceMatrix.getColumns(u);

                for (int j : items) {
                    if (Randoms.uniform() < ratio) {
                        testMatrix.set(u, j, 0.0);
                    } else {
                        trainMatrix.set(u, j, 0.0);
                    }
                }
            }

            SparseMatrix.reshape(testMatrix);
            SparseMatrix.reshape(trainMatrix);
        }
    }


    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where a fixed number of ratings corresponding to the given
     * {@code ratio} are preserved for each user as training data with the rest
     * as test.
     *
     *  @param ratio  the ratio of training data
     */
    public void getFixedRatioByUser(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {

                List<Integer> items = preferenceMatrix.getColumns(u);
                // k is the test set, this will be smaller, so we want these indices in the list
                int k = (int) Math.floor(items.size() * (1 - ratio));
                try {
                    List<Integer> testIndexes = Randoms.randInts(k, 0, items.size());

                    for (int j : items) {
                        if (testIndexes.contains(j)) {
                            trainMatrix.set(u, j, 0.0);
                        } else {
                            testMatrix.set(u, j, 0.0);
                        }
                    }
                } catch (java.lang.Exception e) {
                    LOG.error("This error should not happen because k cannot be outside of the range if ratio is " + ratio);
                }
            }

            SparseMatrix.reshape(testMatrix);
            SparseMatrix.reshape(trainMatrix);
        }
    }

    /**
     * Split the ratings of each user (by date) into two parts: (ratio)
     * training, (1-ratio) test subsets
     *
     * @param ratio the ratio of train data
     */
    public void getRatioByUserDate(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int user = 0, um = preferenceMatrix.numRows(); user < um; user++) {
                List<Integer> unsortedItems = preferenceMatrix.getColumns(user);

                int size = unsortedItems.size();

                List<RatingContext> rcs = new ArrayList<>(size);
                for (int item : unsortedItems) {
                    rcs.add(new RatingContext(user, item, (long) datetimeMatrix.get(user, item)));
                }
                Collections.sort(rcs);

                int trainSize = (int) (rcs.size() * ratio);
                for (int i = 0; i < rcs.size(); i++) {
                    RatingContext rc = rcs.get(i);
                    int u = rc.getUser();
                    int j = rc.getItem();
                    if (i < trainSize)
                        testMatrix.set(u, j, 0.0);
                    else
                        trainMatrix.set(u, j, 0.0);
                }
            }
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
        }

    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code ratio} percentage of ratings are preserved for each
     * item, and the rest are used as the testing data.
     *
     * @param ratio  the ratio of training data
     */
    public void getRatioByItem(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int i = 0, im = preferenceMatrix.numColumns(); i < im; i++) {

                List<Integer> users = preferenceMatrix.getRows(i);

                for (int u : users) {
                    if (Randoms.uniform() < ratio) {
                        testMatrix.set(u, i, 0.0);
                    } else {
                        trainMatrix.set(u, i, 0.0);
                    }
                }
            }
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
        }
    }

    /**
     * Split the ratings of each item (by date) into two parts: (ratio)
     * training, (1-ratio) test subsets.
     *
     * @param ratio the ratio of training data
     */
    public void getRatioByItemDate(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int item = 0, im = preferenceMatrix.numColumns(); item < im; item++) {
                List<Integer> unsortedUsers = preferenceMatrix.getRows(item);

                int size = unsortedUsers.size();
                List<RatingContext> rcs = new ArrayList<>(size);
                for (int user : unsortedUsers) {
                    rcs.add(new RatingContext(user, item, (long) datetimeMatrix.get(user, item)));
                }
                Collections.sort(rcs);

                int trainSize = (int) (rcs.size() * ratio);
                for (int i = 0; i < rcs.size(); i++) {
                    RatingContext rc = rcs.get(i);
                    int u = rc.getUser();
                    int j = rc.getItem();

                    if (i < trainSize)
                        testMatrix.set(u, j, 0.0);
                    else
                        trainMatrix.set(u, j, 0.0);
                }
            }
            SparseMatrix.reshape(testMatrix);
            SparseMatrix.reshape(trainMatrix);
        }
    }

    /**
     * Split the rating into : (train-ratio) training, (validation-ratio)
     * validation, and test three subsets.
     *
     * @param trainRatio      training ratio
     * @param validationRatio validation ratio
     */
    public void getRatio(double trainRatio, double validationRatio) {
        if ((trainRatio > 0 && validationRatio > 0) && (trainRatio + validationRatio) < 1) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            validationMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {

                SparseVector uv = preferenceMatrix.row(u);
                for (VectorEntry j : uv) {
                    double rdm = Randoms.uniform();
                    if (rdm < trainRatio) {
                        // training
                        validationMatrix.set(u, j.index(), 0.0);
                        testMatrix.set(u, j.index(), 0.0);
                    } else if (rdm < trainRatio + validationRatio) {
                        // validation
                        trainMatrix.set(u, j.index(), 0.0);
                        testMatrix.set(u, j.index(), 0.0);
                    } else {
                        // test
                        trainMatrix.set(u, j.index(), 0.0);
                        validationMatrix.set(u, j.index(), 0.0);
                    }
                }
            }

            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(validationMatrix);
            SparseMatrix.reshape(testMatrix);
        }
    }
}