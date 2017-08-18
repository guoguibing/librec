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
import net.librec.math.structure.SparseMatrix;
import net.librec.util.Lists;
import net.librec.util.RatingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GivenN Data Splitter<br>
 * Split dataset into train set and test set by given number.<br>
 *
 * @author WangYuFeng and liuxz
 */
public class GivenNDataSplitter extends AbstractDataSplitter {

    /** The rate dataset for splitting */
    private SparseMatrix preferenceMatrix;

    /** The datetime dataset for splitting */
    private SparseMatrix datetimeMatrix;

    /**
     * Empty constructor.
     */
    public GivenNDataSplitter() {
    }

    /**
     * Initializes a newly created {@code GivenNDataSplitter} object
     * with configuration.
     *
     * @param dataConvertor  data convertor
     * @param conf           the configuration for the splitter.
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
        this.preferenceMatrix = dataConvertor.getPreferenceMatrix();
        this.datetimeMatrix = dataConvertor.getDatetimeMatrix();
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

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
                List<Integer> items = preferenceMatrix.getColumns(u);
                int numRated = items.size();

                if (numRated > numGiven) {
                    int[] givenIndex = Randoms.nextIntArray(numGiven, numRated);

                    for (int i = 0, j = 0; j < numRated; j++) {
                        if (i < givenIndex.length && givenIndex[i] == j) {
                            testMatrix.set(u, items.get(j), 0.0);
                            i++;
                        } else {
                            trainMatrix.set(u, items.get(j), 0.0);
                        }
                    }
                } else {
                    for (int j : items)
                        testMatrix.set(u, j, 0.0);
                }
            }
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
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

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
                List<Integer> items = preferenceMatrix.getColumns(u);
                List<RatingContext> rcs = new ArrayList<>(Lists.initSize(items.size()));
                for (int j : items)
                    rcs.add(new RatingContext(u, j, (long) datetimeMatrix.get(u, j)));
                Collections.sort(rcs);

                for (int i = 0; i < rcs.size(); i++) {
                    RatingContext rc = rcs.get(i);
                    int j = rc.getItem();

                    if (i < numGiven)
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
     * ratings where {@code numGiven} ratings are preserved for each item, and
     * the rest are used as the testing data.
     *
     * @param numGiven given number
     * @throws Exception if error occurs
     */
    public void getGivenNByItem(int numGiven) throws Exception {
        if (numGiven > 0) {

            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int j = 0, jm = preferenceMatrix.numColumns(); j < jm; j++) {
                List<Integer> users = preferenceMatrix.getRows(j);
                int numRated = users.size();
                if (numRated > numGiven) {

                    int[] givenIndex = Randoms.nextIntArray(numGiven, numRated);
                    for (int i = 0, k = 0; k < numRated; k++) {
                        if (i < givenIndex.length && givenIndex[i] == k) {
                            testMatrix.set(users.get(k), j, 0.0);
                            i++;
                        } else {
                            trainMatrix.set(users.get(k), j, 0.0);
                        }
                    }
                } else {
                    for (int u : users)
                        testMatrix.set(u, j, 0.0);
                }
            }
            SparseMatrix.reshape(trainMatrix);
            SparseMatrix.reshape(testMatrix);
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
            trainMatrix = new SparseMatrix(preferenceMatrix);
            testMatrix = new SparseMatrix(preferenceMatrix);

            for (int j = 0, jm = preferenceMatrix.numRows(); j < jm; j++) {
                List<Integer> users = preferenceMatrix.getRows(j);
                List<RatingContext> rcs = new ArrayList<>(Lists.initSize(users.size()));

                for (int u : users)
                    rcs.add(new RatingContext(u, j, (long) datetimeMatrix.get(u, j)));

                Collections.sort(rcs);
                for (int i = 0; i < rcs.size(); i++) {
                    RatingContext rc = rcs.get(i);
                    int u = rc.getUser();

                    if (i < numGiven)
                        testMatrix.set(u, j, 0.0);
                    else
                        trainMatrix.set(u, j, 0.0);
                }
                SparseMatrix.reshape(trainMatrix);
                SparseMatrix.reshape(testMatrix);
            }
        }
    }


}
