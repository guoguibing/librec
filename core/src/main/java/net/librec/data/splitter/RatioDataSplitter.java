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
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.util.RatingContext;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ratio Data Splitter.<br>
 * Split dataset into train set, test set, valid set by ratio.<br>
 *
 * @author WangYuFeng, Liuxz and Keqiang Wang
 */
public class RatioDataSplitter extends AbstractDataSplitter {


    /**
     * The datetime dataset for splitting
     */
    private SequentialAccessSparseMatrix datetimeMatrix;

    /**
     * Empty constructor.
     */
    public RatioDataSplitter() {
    }

    /**
     * Initializes a newly created {@code RatioDataSplitter} object
     * with convertor and configuration.
     *
     * @param dataConvertor the convertor for the splitter.
     * @param conf          the configuration for the splitter.
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
        if (null == this.preferenceMatrix) {
            this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf);
            if ((!(dataConvertor instanceof ArffDataConvertor))
                    &&(StringUtils.equals(conf.get("data.column.format"), "UIRT"))) {
                this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
            }
        }

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
            default:
                break;
        }
    }

    /**
     * Split ratings into two parts: (ratio) training, (1-ratio) test subsets.
     *
     * @param ratio the ratio of training data over all the ratings
     */
    public void getRatioByRating(double ratio) {
        if (ratio > 0 && ratio < 1) {

            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (MatrixEntry matrixEntry : preferenceMatrix) {

                double rdm = Randoms.uniform();

                if (rdm < ratio) {
                    testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                } else {
                    trainMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                }
            }

            testMatrix.reshape();
            trainMatrix.reshape();
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

            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            List<RatingContext> ratingContexts = new ArrayList<>(datetimeMatrix.size());
            for (MatrixEntry matrixEntry : preferenceMatrix) {
                ratingContexts.add(new RatingContext(matrixEntry.row(), matrixEntry.columnPosition(),
                        (long) datetimeMatrix.getAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition())));
            }
            Collections.sort(ratingContexts);

            int trainSize = (int) (ratingContexts.size() * ratio);
            for (int index = 0; index < ratingContexts.size(); index++) {
                RatingContext rc = ratingContexts.get(index);
                int rowIndex = rc.getUser();
                int columnPosition = rc.getItem();

                if (index < trainSize)
                    testMatrix.setAtColumnPosition(rowIndex, columnPosition, 0.0D);
                else
                    trainMatrix.setAtColumnPosition(rowIndex, columnPosition, 0.0D);
            }

            trainMatrix.reshape();
            testMatrix.reshape();
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code ratio} percentage of ratings are preserved for each
     * user, and the rest are used as the testing data.
     *
     * @param ratio the ratio of training data
     */
    public void getRatioByUser(double ratio) {
        if (ratio > 0 && ratio < 1) {
            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
                for (Vector.VectorEntry vectorEntry : preferenceMatrix.row(rowIndex)) {
                    if (Randoms.uniform() < ratio) {
                        testMatrix.setAtColumnPosition(rowIndex, vectorEntry.position(), 0.0D);
                    } else {
                        trainMatrix.setAtColumnPosition(rowIndex, vectorEntry.position(), 0.0D);
                    }
                }
            }
        }
        testMatrix.reshape();
        trainMatrix.reshape();
    }


    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where a fixed number of ratings corresponding to the given
     * {@code ratio} are preserved for each user as training data with the rest
     * as test.
     *
     * @param ratio the ratio of training data
     */
    public void getFixedRatioByUser(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
                int numRated = preferenceMatrix.row(rowIndex).getNumEntries();

                // k is the test set, this will be smaller, so we want these indices in the list
                int numRatio = (int) Math.floor(numRated * (1 - ratio));
                if (numRatio < 1) {
                    continue;
                }
                try {
                    int[] givenPositions = Randoms.nextIntArray(numRatio, numRated);

                    for (int testColumnPosition = 0, columnPosition = 0; columnPosition < numRated; columnPosition++) {
                        if (testColumnPosition < givenPositions.length && givenPositions[testColumnPosition] == columnPosition) {
                            testMatrix.setAtColumnPosition(rowIndex, testColumnPosition, 0.0D);
                            testColumnPosition++;
                        } else {
                            trainMatrix.setAtColumnPosition(rowIndex, testColumnPosition, 0.0D);
                        }
                    }
                } catch (java.lang.Exception e) {
                    LOG.error("This error should not happen because k cannot be outside of the range if ratio is " + ratio);
                }
            }

            testMatrix.reshape();
            trainMatrix.reshape();
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

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
                SequentialSparseVector itemRatingVector = preferenceMatrix.row(rowIndex);
                if (itemRatingVector.getNumEntries() < 1) {
                    continue;
                }
                List<RatingContext> itemRatingList = new ArrayList<>(itemRatingVector.getNumEntries());

                for (Vector.VectorEntry vectorEntry : itemRatingVector) {
                    itemRatingList.add(new RatingContext(rowIndex, vectorEntry.position(), (long) vectorEntry.get()));
                }
                int trainSize = (int) (itemRatingList.size() * ratio);

                Collections.sort(itemRatingList);

                for (int index = 0; index < itemRatingList.size(); index++) {
                    if (index < trainSize) {
                        testMatrix.setAtColumnPosition(rowIndex, itemRatingList.get(index).getItem(), 0.0D);
                    } else {
                        trainMatrix.setAtColumnPosition(rowIndex, itemRatingList.get(index).getItem(), 0.0D);
                    }
                }
            }
            trainMatrix.reshape();
            testMatrix.reshape();
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code ratio} percentage of ratings are preserved for each
     * item, and the rest are used as the testing data.
     *
     * @param ratio the ratio of training data
     */
    public void getRatioByItem(double ratio) {

        if (ratio > 0 && ratio < 1) {

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int columnIndex = 0, columnSize = preferenceMatrix.columnSize(); columnIndex < columnSize; columnIndex++) {
                for (Vector.VectorEntry vectorEntry : preferenceMatrix.column(columnIndex)) {
                    if (Randoms.uniform() < ratio) {
                        testMatrix.setAtRowPosition(vectorEntry.position(), columnIndex, 0.0D);
                    } else {
                        trainMatrix.setAtRowPosition(vectorEntry.position(), columnIndex, 0.0D);
                    }
                }
            }
            trainMatrix.reshape();
            testMatrix.reshape();
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

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int columnIndex = 0, columnSize = preferenceMatrix.columnSize(); columnIndex < columnSize; columnIndex++) {
                SequentialSparseVector userRatingVector = preferenceMatrix.column(columnIndex);

                if (userRatingVector.getNumEntries() < 1) {
                    continue;
                }

                List<RatingContext> ratingContexts = new ArrayList<>(userRatingVector.getNumEntries());

                for (Vector.VectorEntry vectorEntry : userRatingVector) {
                    ratingContexts.add(new RatingContext(vectorEntry.position(), columnIndex,
                            (long) datetimeMatrix.getAtRowPosition(vectorEntry.position(), columnIndex)));
                }

                Collections.sort(ratingContexts);
                int trainSize = (int) (ratingContexts.size() * ratio);

                for (int rowPosition = 0; rowPosition < ratingContexts.size(); rowPosition++) {
                    RatingContext ratingContext = ratingContexts.get(rowPosition);
                    if (rowPosition < trainSize)
                        testMatrix.setAtRowPosition(ratingContext.getUser(), columnIndex, 0.0D);
                    else
                        trainMatrix.setAtRowPosition(ratingContext.getUser(), columnIndex, 0.0D);
                }
            }
            testMatrix.reshape();
            trainMatrix.reshape();
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

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            validationMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

//            for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
//                SequentialAccessSparseVector userRatingVector = preferenceMatrix.row(rowIndex);
            for (MatrixEntry matrixEntry : preferenceMatrix) {
                double rdm = Randoms.uniform();
                if (rdm < trainRatio) {
                    // training
                    validationMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                    testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                } else if (rdm < trainRatio + validationRatio) {
                    // validation
                    trainMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                    testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                } else {
                    // test
                    trainMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                    validationMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
                }
            }

            trainMatrix.reshape();
            validationMatrix.reshape();
            testMatrix.reshape();
        }
    }
}