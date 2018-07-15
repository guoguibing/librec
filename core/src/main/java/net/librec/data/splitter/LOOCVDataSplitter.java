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
import com.google.common.collect.Table;
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
 * Leave one out Splitter<br>
 * Leave random or the last one user/item out as test set and the rest treated<br>
 * as the train set.
 *
 * @author WangYuFeng, Liuxz and Keqiang Wang
 */
public class LOOCVDataSplitter extends AbstractDataSplitter {

    /**
     * The rate dataset for splitting
     */
    private SequentialAccessSparseMatrix preferenceMatrix;

    /**
     * The datetime dataset for splitting
     */
    private SequentialAccessSparseMatrix datetimeMatrix;

    /**
     * wrap kcv into leave-one-out if leave every rate out
     */
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
     * @param dataConvertor the convertor for the splitter.
     * @param conf          the configuration for the splitter.
     */
    public LOOCVDataSplitter(DataConvertor dataConvertor, Configuration conf) {
        this.dataConvertor = dataConvertor;
        this.conf = conf;
    }

    /**
     * Types of the LOOCVDataSplitter
     */
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
//            case "rate": {
//                if (null == kcv) {
//                    conf.setInt("data.splitter.cv.number", dataConvertor.getPreferenceMatrix().size());
//                    kcv = new KCVDataSplitter(dataConvertor, conf);
//                    kcv.splitFolds();
//                }
//                kcv.splitData();
//                trainMatrix = kcv.getTrainData();
//                testMatrix = kcv.getTestData();
//                break;
//            }
            default:{
                LOG.info("Please check ");
            }
        }
    }

    /**
     * Split ratings into two parts where one rating per user is preserved as
     * the test set and the remaining data as the training set.
     */
    public void getLOOByUser() {
        trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();

        for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
            int numColumnEntries = preferenceMatrix.row(rowIndex).getNumEntries();
            if (numColumnEntries == 0){
                continue;
            }
            int randomRowPosition = (int) (numColumnEntries * Randoms.uniform());
            this.preferenceMatrix = dataConvertor.getPreferenceMatrix();

            trainMatrix.setAtColumnPosition(rowIndex, randomRowPosition, 0.0D);

            dataTable.put(rowIndex, preferenceMatrix.row(rowIndex).getIndexAtPosition(randomRowPosition),
                    preferenceMatrix.getAtColumnPosition(rowIndex, randomRowPosition));
        }

        trainMatrix.reshape();
        testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix.rowSize(), preferenceMatrix.columnSize(), dataTable);
    }

    /**
     * Split ratings into two parts where the last user according to date is
     * preserved as the test set and the remaining data as the training set.
     */
    public void getLOOByUserDate() {
        trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();

        for (int rowIndex = 0, rowSize = preferenceMatrix.rowSize(); rowIndex < rowSize; rowIndex++) {
            SequentialSparseVector itemRatingVector = preferenceMatrix.row(rowIndex);
            if (itemRatingVector.getNumEntries() == 0){
                continue;
            }

            List<RatingContext> ratingContexts = new ArrayList<>(itemRatingVector.getNumEntries());
            for (Vector.VectorEntry vectorEntry: itemRatingVector) {
                ratingContexts.add(new RatingContext(rowIndex, vectorEntry.position(),
                        (long) datetimeMatrix.getAtColumnPosition(rowIndex, vectorEntry.position())));
            }
            Collections.sort(ratingContexts);
            int columnPosition = ratingContexts.get(ratingContexts.size() - 1).getItem();
            trainMatrix.setAtColumnPosition(rowIndex, columnPosition, 0.0D);
            dataTable.put(rowIndex, preferenceMatrix.row(rowIndex).getIndexAtPosition(columnPosition),
                    preferenceMatrix.getAtColumnPosition(rowIndex, columnPosition));
        }
        trainMatrix.reshape();
        testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix.rowSize(), preferenceMatrix.columnSize(), dataTable);
    }

    /**
     * Split ratings into two parts where one rating per item is preserved as
     * the test set and the remaining data as the training set.
     */
    public void getLOOByItems() {
        trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();

        for (int columnIndex = 0, columnSize = preferenceMatrix.columnSize(); columnIndex < columnSize; columnIndex++) {
            int numRowEntries = preferenceMatrix.column(columnIndex).getNumEntries();
            if (numRowEntries == 0){
                continue;
            }
            int randomRowPosition = (int) (numRowEntries * Randoms.uniform());

            trainMatrix.setAtRowPosition(randomRowPosition, columnIndex, 0.0D);
            dataTable.put(preferenceMatrix.column(columnIndex).getIndexAtPosition(randomRowPosition),
                    columnIndex, preferenceMatrix.getAtRowPosition(randomRowPosition, columnIndex));
        }

        trainMatrix.reshape();
        testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix.rowSize(), preferenceMatrix.columnSize(), dataTable);
    }

    /**
     * Split ratings into two parts where the last item according to date is
     * preserved as the test set and the remaining data as the training set.
     */
    public void getLooByItemsDate() {
        trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();

        for (int columnIndex = 0, columnSize = preferenceMatrix.columnSize(); columnIndex < columnSize; columnIndex++) {
            SequentialSparseVector userRatingVector = preferenceMatrix.column(columnIndex);
            if (userRatingVector.getNumEntries() == 0){
                continue;
            }

            List<RatingContext> ratingContexts = new ArrayList<>();
            for (Vector.VectorEntry vectorEntry : userRatingVector) {
                ratingContexts.add(new RatingContext(vectorEntry.position(), columnIndex,
                        (long) datetimeMatrix.getAtRowPosition(vectorEntry.position(), columnIndex)));
            }
            Collections.sort(ratingContexts);
            int rowPosition = ratingContexts.get(ratingContexts.size() - 1).getUser();

            trainMatrix.setAtRowPosition(rowPosition, columnIndex, 0.0D);
            dataTable.put(preferenceMatrix.column(columnIndex).getIndexAtPosition(rowPosition), columnIndex,
                    preferenceMatrix.getAtRowPosition(rowPosition, columnIndex));
        }

        trainMatrix.reshape();
        testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix.rowSize(), preferenceMatrix.columnSize(), dataTable);
    }

}
