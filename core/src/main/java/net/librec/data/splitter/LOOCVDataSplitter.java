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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataConvertor;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.SparseMatrix;
import net.librec.util.RatingContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leave one out Splitter<br>
 * Leave random or the last one user/item out as test set and the rest treated<br>
 * as the train set.
 *
 * @author WangYuFeng and Liuxz
 */
public class LOOCVDataSplitter extends AbstractDataSplitter {

    /** The rate dataset for splitting */
    private SparseMatrix preferenceMatrix;

    /** The datetime dataset for splitting */
    private SparseMatrix datetimeMatrix;

    /** wrap kcv into leave-one-out if leave every rate out  */
    private KCVDataSplitter kcv;
    /**
     * Empty constructor.
     */
    public LOOCVDataSplitter() {
    }

    /**
     * Initializes a newly created {@code LOOCVDataSplitter} object
     * with convertor and configuration.
     *
     * @param dataConvertor
     *          the convertor for the splitter.
     * @param conf
     *          the configuration for the splitter.
     */
    public LOOCVDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /** Types of the LOOCVDataSplitter */
    enum LOOCVType {
        LOOByUser, LOOByItem
    }

    /**
     * Split the data.
     *
     * @throws LibrecException if error occurs
     */
    @Override
    public void splitData() throws LibrecException {
        preferenceMatrix = dataConvertor.getPreferenceMatrix();
        datetimeMatrix = dataConvertor.getDatetimeMatrix();
        String splitter = conf.get("data.splitter.loocv");
        switch (splitter.toLowerCase()) {
            case "user": {
                getLOOByUser();
                break;
            }
            case "item": {
                getLOOByItems();
                break;
            }
            case "userdate": {
                getLOOByUserDate();
                break;
            }
            case "itemdate": {
                getLooByItemsDate();
                break;
            }
            case "rate":{
                if (null == kcv) {
                    conf.setInt("data.splitter.cv.number", dataConvertor.getPreferenceMatrix().size());
                    kcv = new KCVDataSplitter(dataConvertor, conf);
                    kcv.splitFolds();
                }
                kcv.splitData();
                trainMatrix = kcv.getTrainData();
                testMatrix = kcv.getTestData();
            }
        }
    }

    /**
     * Split ratings into two parts where one rating per user is preserved as
     * the test set and the remaining data as the training set.
     */
    public void getLOOByUser() {
        trainMatrix = new SparseMatrix(preferenceMatrix);
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
            List<Integer> items = preferenceMatrix.getColumns(u);

            int randId = (int) (items.size() * Randoms.uniform());
            int i = items.get(randId);
            this.preferenceMatrix = dataConvertor.getPreferenceMatrix();

            trainMatrix.set(u, i, 0);

            dataTable.put(u, i, preferenceMatrix.get(u, i));
            colMap.put(i, u);
        }

        SparseMatrix.reshape(trainMatrix);
        testMatrix = new SparseMatrix(preferenceMatrix.numRows(), preferenceMatrix.numColumns(), dataTable, colMap);
    }

    /**
     * Split ratings into two parts where the last user according to date is
     * preserved as the test set and the remaining data as the training set.
     */
    public void getLOOByUserDate() {
        trainMatrix = new SparseMatrix(preferenceMatrix);
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
            List<Integer> items = preferenceMatrix.getColumns(u);
            int i = -1;

            List<RatingContext> rcs = new ArrayList<>();
            for (int j : items) {
                rcs.add(new RatingContext(u, j, (long) datetimeMatrix.get(u, j)));
            }
            Collections.sort(rcs);
            i = rcs.get(rcs.size() - 1).getItem();
            trainMatrix.set(u, i, 0);
            dataTable.put(u, i, preferenceMatrix.get(u, i));
            colMap.put(i, u);
        }
        SparseMatrix.reshape(trainMatrix);
        testMatrix = new SparseMatrix(preferenceMatrix.numRows(), preferenceMatrix.numColumns(), dataTable, colMap);
    }

    /**
     * Split ratings into two parts where one rating per item is preserved as
     * the test set and the remaining data as the training set.
     */
    public void getLOOByItems() {
        trainMatrix = new SparseMatrix(preferenceMatrix);

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for (int i = 0, im = preferenceMatrix.numColumns(); i < im; i++) {
            List<Integer> users = preferenceMatrix.getRows(i);

            int randId = (int) (users.size() * Randoms.uniform());
            int u = users.get(randId);

            trainMatrix.set(u, i, 0);
            dataTable.put(u, i, preferenceMatrix.get(u, i));
            colMap.put(i, u);
        }

        SparseMatrix.reshape(trainMatrix);
        testMatrix = new SparseMatrix(preferenceMatrix.numRows(), preferenceMatrix.numColumns(), dataTable, colMap);
    }

    /**
     * Split ratings into two parts where the last item according to date is
     * preserved as the test set and the remaining data as the training set.
     */
    public void getLooByItemsDate() {
        trainMatrix = new SparseMatrix(preferenceMatrix);

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        Multimap<Integer, Integer> colMap = HashMultimap.create();

        for (int i = 0, im = preferenceMatrix.numColumns(); i < im; i++) {
            List<Integer> users = preferenceMatrix.getRows(i);
            int u = -1;

            List<RatingContext> rcs = new ArrayList<>();
            for (int v : users) {
                rcs.add(new RatingContext(v, i, (long) datetimeMatrix.get(v, i)));
            }
            Collections.sort(rcs);
            u = rcs.get(rcs.size() - 1).getUser();

            trainMatrix.set(u, i, 0);
            dataTable.put(u, i, preferenceMatrix.get(u, i));
            colMap.put(i, u);
        }

        SparseMatrix.reshape(trainMatrix);
        testMatrix = new SparseMatrix(preferenceMatrix.numRows(), preferenceMatrix.numColumns(), dataTable, colMap);
    }

}
