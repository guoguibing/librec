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
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.util.RatingContext;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * GivenN Data Splitter<br>
 * Split dataset into train set and test set by given number.<br>
 *
 * @author WangYuFeng, liuxz and Keqiang Wang
 */
public class GivenNDataSplitter extends AbstractDataSplitter {

    /**
     * The rate dataset for splitting
     */
    private SequentialAccessSparseMatrix preferenceMatrix;

    /**
     * The datetime dataset for splitting
     */
    private SequentialAccessSparseMatrix datetimeMatrix;

    /**
     * Empty constructor.
     */
    public GivenNDataSplitter() {
    }

    /**
     * Initializes a newly created {@code GivenNDataSplitter} object
     * with configuration.
     *
     * @param dataConvertor data convertor
     * @param conf          the configuration for the splitter.
     */
    public GivenNDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /**
     * Split the data.
     *
     * @throws LibrecException if error occurs
     */
    @Override
    public void splitData() throws LibrecException {
        if (null == this.preferenceMatrix) {
//            if (Objects.equals(conf.get("data.convert.columns"), null)) {
//                this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
//            } else {
//                this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf.get("data.convert.columns").split(","));
//            }
            this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf);
            if ((!(dataConvertor instanceof ArffDataConvertor))
                    &&(StringUtils.equals(conf.get("data.column.format"), "UIRT"))) {
                this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
            }
        }

        String splitter = conf.get("data.splitter.givenn");
        switch (splitter.toLowerCase()) {
            case "user": {
                try {
                    getGivenNByUser(Integer.parseInt(conf.get("data.splitter.givenn.n")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case "item": {
                try {
                    getGivenNByItem(Integer.parseInt(conf.get("data.splitter.givenn.n")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case "userdate": {
                try {
                    getGivenNByUserDate(Integer.parseInt(conf.get("data.splitter.givenn.n")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case "itemdate": {
                try {
                    getGivenNByItemDate(Integer.parseInt(conf.get("data.splitter.givenn.n")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code numGiven} ratings are preserved for each user, and
     * the rest are used as the testing data.
     *
     * @param numGiven given number
     * @throws Exception if error occurs
     */
    public void getGivenNByUser(int numGiven) throws Exception {
        if (numGiven > 0) {

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
                int numRated = preferenceMatrix.row(rowIndex).getNumEntries();

                if (numRated > numGiven) {
                    int[] givenPositions = Randoms.nextIntArray(numGiven, numRated);

                    for (int testColumnPosition = 0, columnPosition = 0; columnPosition < numRated; columnPosition++) {
                        if (testColumnPosition < givenPositions.length && givenPositions[testColumnPosition] == columnPosition) {
                            testMatrix.setAtColumnPosition(rowIndex, columnPosition, 0.0D);
                            testColumnPosition++;
                        } else {
                            trainMatrix.setAtColumnPosition(rowIndex, columnPosition, 0.0D);
                        }
                    }
                } else {
                    for (Vector.VectorEntry vectorEntry : preferenceMatrix.row(rowIndex)) {
                        testMatrix.setAtColumnPosition(rowIndex, vectorEntry.position(), 0.0D);
                    }
                }
            }
            trainMatrix.reshape();
            testMatrix.reshape();
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code numGiven} earliest ratings are preserved for each
     * user, and the rest are used as the testing data.
     *
     * @param numGiven given number
     */
    public void getGivenNByUserDate(int numGiven) {
        if (numGiven > 0) {

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

                Collections.sort(itemRatingList);

                for (int index = 0; index < itemRatingList.size(); index++) {
                    if (index < numGiven) {
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
     * ratings where {@code numGiven} ratings are preserved for each item, and
     * the rest are used as the testing data.
     *
     * @param numGiven given number
     * @throws Exception if error occurs
     */
    public void getGivenNByItem(int numGiven) throws Exception {
        if (numGiven > 0) {

            trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
            testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

            for (int columnIndex = 0, columnSize = preferenceMatrix.columnSize(); columnIndex < columnSize; columnIndex++) {
                int numRated = preferenceMatrix.column(columnIndex).getNumEntries();
                if (numRated > numGiven) {
                    int[] givenPositions = Randoms.nextIntArray(numGiven, numRated);
                    for (int testRowPosition = 0, rowPosition = 0; rowPosition < numRated; rowPosition++) {
                        if (testRowPosition < givenPositions.length && givenPositions[testRowPosition] == rowPosition) {
                            testMatrix.setAtRowPosition(rowPosition, columnIndex, 0.0D);
                            testRowPosition++;
                        } else {
                            trainMatrix.setAtRowPosition(rowPosition, columnIndex, 0.0D);
                        }
                    }
                } else {
                    for (int rowPosition = 0; rowPosition < numRated; rowPosition++) {
                        testMatrix.setAtRowPosition(rowPosition, columnIndex, 0.0D);
                    }
                }
            }

            trainMatrix.reshape();
            testMatrix.reshape();
        }
    }

    /**
     * Split ratings into two parts: the training set consisting of user-item
     * ratings where {@code numGiven} earliest ratings are preserved for each
     * item, and the rest are used as the testing data.
     *
     * @param numGiven given number
     */
    public void getGivenNByItemDate(int numGiven) {
        if (numGiven > 0) {
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
                for (int rowPosition = 0; rowPosition < ratingContexts.size(); rowPosition++) {
                    RatingContext ratingContext = ratingContexts.get(rowPosition);
                    if (rowPosition < numGiven)
                        testMatrix.setAtRowPosition(ratingContext.getUser(), columnIndex, 0.0D);
                    else
                        trainMatrix.setAtRowPosition(ratingContext.getUser(), columnIndex, 0.0D);
                }
            }
            trainMatrix.reshape();
            testMatrix.reshape();
        }
    }
}
