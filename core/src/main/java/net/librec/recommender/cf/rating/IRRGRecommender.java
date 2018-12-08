/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.cf.rating;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.Vector;
import net.librec.recommender.MatrixFactorizationRecommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zhu Sun, Guibing Guo, and Jie Zhang <strong>Exploiting Implicit Item Relationships for
 * Recommender Systems</strong>, UMAP 2015.
 *
 * @author SunYatong
 */
public class IRRGRecommender extends MatrixFactorizationRecommender {

    /** item relationship regularization coefficient */
    private double alpha;

    /** adjust the reliability */
    private double C = 50.0;

    /** k nearest neighborhoods */
    private int K = 50;

    /** store itemsets which contain three items */
    private ArrayList<ArrayList> mylist = new ArrayList<>();

    /** store co-occurence between two items. */
    private Table<Integer, Integer, Integer> itemCount = HashBasedTable.create();

    /** store item-to-item AR */
    private Table<Integer, Integer, Double> itemCorrsAR = HashBasedTable.create();

    /** store sorted item-to-item AR */
    private Table<Integer, Integer, Double> itemCorrsAR_Sorted = HashBasedTable.create();

    /** store the complementary item-to-item AR */
    private Table<Integer, Integer, Double> itemCorrsAR_added = HashBasedTable.create();

    /** store group-to-item AR */
    private Map<Integer, List<Number>> itemCorrsGAR = new HashMap<>();

    /** store sorted group-to-item AR */
    private Map<Integer,Table<Integer,Integer,Double>> itemCorrsGAR_Sorted = new HashMap<>();


    @Override
    protected void setup() throws LibrecException {
        super.setup();
        alpha = conf.getDouble("rec.alpha");
        userFactors.init(0.8);
        itemFactors.init(0.8);
        preprocess();
    }

    @Override
    protected void trainModel() throws LibrecException {

        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0;

            DenseMatrix PS = new DenseMatrix(numUsers, numFactors);
            DenseMatrix QS = new DenseMatrix(numItems, numFactors);
            for (MatrixEntry me : trainMatrix) {
                int u = me.row();
                int j = me.column();
                double ruj = me.get();
                if (ruj <= 0) continue;
                double pred = innerPredict(u, j);
                double euj = Maths.logistic(pred) - Maths.normalize(ruj, minRate, maxRate);
                double csgd = Maths.logisticGradientValue(pred) * euj;

                loss += euj * euj;
                for (int f = 0; f < numFactors; f++) {
                    double puf = userFactors.get(u, f);
                    double qjf = itemFactors.get(j, f);
                    PS.plus(u, f, csgd * qjf + regUser * puf);
                    QS.plus(j, f, csgd * puf + regItem * qjf);
                    loss += regUser * puf * puf + regItem * qjf * qjf;
                }
            }

            for (int j : itemCorrsAR_added.columnKeySet()) { // complementary item-to-item AR
                for (int k : itemCorrsAR_added.column(j).keySet()) {
                    double skj = itemCorrsAR_added.get(k, j);
                    for (int f = 0; f < numFactors; f++) {
                        double ekj = itemFactors.get(j, f) - itemFactors.get(k, f);
                        QS.plus(j, f, alpha * skj * ekj);
                        loss += alpha * skj * ekj * ekj;
                    }
                }
                for(int g : itemCorrsAR_added.row(j).keySet()){
                    double sjg = itemCorrsAR_added.get(j, g);
                    for(int  f = 0; f < numFactors; f++){
                        double ejg = itemFactors.get(j, f) - itemFactors.get(g, f);
                        QS.plus(j, f, alpha * sjg * ejg);
                    }
                }
            }

            for (int j : itemCorrsGAR_Sorted.keySet()) { // group-to-item AR
                Table<Integer, Integer, Double> temp =  HashBasedTable.create();
                temp = itemCorrsGAR_Sorted.get(j);
                for (int g : temp.rowKeySet()) {
                    Map<Integer, Double> col = temp.row(g);
                    for (int k : col.keySet()) {
                        for (int f = 0; f < numFactors; f++) {
                            double egkj = itemFactors.get(j, f) - (itemFactors.get(g, f) + itemFactors.get(k, f)) / Math.sqrt(2.0);
                            double egkj_1 = alpha * col.get(k) * egkj;
                            QS.plus(j, f, egkj_1);
                            loss += egkj_1 * egkj;
                        }
                    }
                }
                for (int k : itemCorrsGAR_Sorted.keySet()) {
                    if (k != j) {
                        Table<Integer, Integer, Double> temp1 =  HashBasedTable.create();
                        temp1 = itemCorrsGAR_Sorted.get(k);
                        Map<Integer, Double> row = temp1.row(j);
                        for (int g : row.keySet()) {
                            for (int f = 0; f < numFactors; f++){
                                double ejgk = itemFactors.get(k, f) - (itemFactors.get(j, f) + itemFactors.get(g, f)) / Math.sqrt(2.0);
                                double ejgk_1 = -alpha * row.get(g) * ejgk / Math.sqrt(2.0);
                                QS.plus(j, f, ejgk_1);
                            }
                        }
                    }
                }
            }

            userFactors = userFactors.plus(PS.times(-learnRate));
            itemFactors = itemFactors.plus(QS.times(-learnRate));

            loss *= 0.5;
            if (isConverged(iter) && earlyStop) {
                break;
            }
            updateLRate(iter);
        }
    }

    protected double innerPredict(int userIdx, int itemIdx) throws LibrecException {
        double pred = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));

        return pred;
    }

    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        double pred = userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
        pred = Maths.logistic(pred);
        pred = minRate + pred * (maxRate - minRate);

        return pred;
    }

    /**
     * compute item-to-item AR and store them into table itemCorrsAR
     */
    protected void computeAR() {

        for (int x=0; x < numItems; x++) {
            SequentialSparseVector qx = trainMatrix.column(x);
            int[] x_users = qx.getIndices();
            int total = qx.getNumEntries();
            for (int y=0; y<numItems; y++) {
                if (x == y) {
                    continue;
                }
                double conf;
                int count = 0;
                int[] y_users = trainMatrix.column(y).getIndices();
                for (int u : x_users) {
                    for (int y_user : y_users) {
                        if (u == y_user) {
                            count++;
                        }
                    }
                }
                double shrink = count/(count + C);
                conf = shrink * (count + 0.0) / total;
                if (conf > 0.0) {
                    itemCorrsAR.put(x, y, conf);
                    itemCount.put(x, y, count);
                }
            }
        }
    }

    /**
     * Store item-to-item AR into table itemCorrsAR_Sorted
     * @param size
     * @param temp
     */
    protected void storeAR(int size, double temp[][]) {
        for (int i = 0; i < size; i++) {
            int x_id = (int) (temp[i][0]);
            int y_id = (int) (temp[i][1]);
            itemCorrsAR_Sorted.put(x_id, y_id, temp[i][2]);
        }
    }

    /**
     * sort the k-nearest neighbors according to the AR values
     * and store the results into table itemCorrsAR_Sorted
     */
    protected void sortAR() {
        for (int x : itemCorrsAR.columnKeySet()) {
            int size = itemCorrsAR.column(x).size();
            double temp[][] = new double [size][3];
            int flag = 0;
            for ( int y : itemCorrsAR.column(x).keySet()) {
                temp[flag][0] = y;
                temp[flag][1] = x;
                temp[flag][2] = itemCorrsAR.get(y,x);
                flag++;
            }
            if (size > K) {
                for (int i = 0; i < K; i++) { //sort k nearest neighbors
                    for (int j = i + 1; j < size; j++) {
                        if (temp[i][2] < temp[j][2]) {
                            for (int k = 0; k < 3; k++) {
                                double trans = temp [i][k];
                                temp [i][k] = temp [j][k];
                                temp [j][k] = trans;
                            }
                        }
                    }
                }
                storeAR (K, temp);
            }
            else {
                storeAR (size, temp);
            }
        }
    }

    /**
     * Find out itemsets which contain three items and store them into mylist.
     */
    protected void computeItemset() {
        for (int y : itemCorrsAR.columnKeySet()) {
            Object[] x = itemCorrsAR_Sorted.column(y).keySet().toArray();
            for (int i = 0; i < x.length - 1; i++) {
                for (int j = i+1; j < x.length; j++) {
                    if (itemCount.contains(x[i], x[j])) {
                        ArrayList list = new ArrayList<>(3);
                        list.add(y);
                        list.add(x[i]);
                        list.add(x[j]);
                        mylist.add(list);
                    }
                }
            }
        }
    }

    /**
     * function to compute group-to-item AR
     *
     * @param a
     * @param b
     * @param c
     * @param count
     */
    protected void compute(int a, int b, int c, int count) {
        double shrink = count / (count + C);
        int co_bc = itemCount.get(b, c);
        double conf = shrink * (count + 0.0) / co_bc;
        if (itemCorrsGAR.containsKey(a)) {
            itemCorrsGAR.get(a).add(b);
            itemCorrsGAR.get(a).add(c);
            itemCorrsGAR.get(a).add(conf);
        }
        else {
            ArrayList list = new ArrayList();
            list.add(b);
            list.add(c);
            list.add(conf);
            itemCorrsGAR.put(a, list);
        }
    }

    /**
     * Compute group-to-item AR and store them into map itemCorrsGAR
     */
    protected void computeGAR() {
        for (int i = 0; i < mylist.size(); i++) {
            int a = (int) mylist.get(i).get(0);
            int b = (int) mylist.get(i).get(1);
            int c = (int) mylist.get(i).get(2);
            SequentialSparseVector qx = trainMatrix.column(a);
            int count = 0;
            for (Vector.VectorEntry ve : qx) {
                int u = ve.index();
                int[] u_items = trainMatrix.row(u).getIndices();

                boolean rub = false, ruc = false;
                for (int u_item : u_items) {
                    if (u_item == b) {
                        rub = true;
                    }
                    if (u_item == c) {
                        ruc = true;
                    }
                }
                if (rub && ruc)
                    count++;
            }
            if (count > 0) {
                compute(a, b, c, count);
            }
        }
    }

    /**
     * Function to store group-to-item AR
     * @param a
     * @param size
     * @param list
     */
    protected void storeGAR(int a, int size, List list) {
        for (int i = 0; i < size; i = i+3) {
            int b = (int) list.get(i);
            int c = (int) list.get(i + 1);
            double conf = (double) list.get(i + 2);
            if (itemCorrsGAR_Sorted.containsKey(a)) {
                itemCorrsGAR_Sorted.get(a).put(b, c, conf);
            }
            else {
                Table<Integer, Integer, Double> temp =  HashBasedTable.create();
                temp.put(b, c, conf);
                itemCorrsGAR_Sorted.put(a, temp);
            }
        }
    }

    /**
     * Order group-to-item AR and store them into map itemCorrsGAR_Sorted
     */
    protected void sortGAR() {
        for (int a : itemCorrsGAR.keySet()) {
            List list = itemCorrsGAR.get(a);
            if (list.size() / 3 > K) {
                for (int i = 0; i < 3 * K; i = i + 3) {
                    for (int j = i+3; j < list.size(); j = j + 3) {
                        double conf1 = (double) list.get(i + 2);
                        double conf2 = (double) list.get(j + 2);
                        if (conf1 < conf2) {
                            for (int x = 0; x < 2; x++) {
                                int temp = (int) list.get(i + x);
                                list.set(i + x, list.get(j+x));
                                list.set(j + x, temp);
                            }
                            list.set(i + 2, conf2);
                            list.set(j + 2, conf1);
                        }
                    }
                }
                storeGAR (a, 3 * K, list);
            }
            else {
                storeGAR (a, list.size(), list);
            }
        }
    }

    /**
     * Function to store complementary item-to-item AR into table itemCorrsAR_added.
     *
     * @param j
     */
    protected void storeCAR( int j ) {
        for (int id : itemCorrsAR_Sorted.column(j).keySet()) {
            double value = itemCorrsAR_Sorted.get(id, j);
            itemCorrsAR_added.put(id, j, value);
        }
    }

    /**
     * Select item-to-item AR to complement group-to-item AR
     */
    protected void addAR() {
        for(int j=0; j<numItems; j++) {
            Table<Integer, Integer, Double> temp =  HashBasedTable.create();
            temp = itemCorrsGAR_Sorted.get(j);
            if(temp != null) {
                int group_size = temp.size();
                if (group_size < K) {
                    int add_size = K - group_size;
                    int item_size = itemCorrsAR_Sorted.column(j).size();
                    double [][] trans = new double [item_size][2];
                    if (item_size > add_size) {
                        int count = 0;
                        for (int id : itemCorrsAR_Sorted.column(j).keySet()) {
                            double value = itemCorrsAR_Sorted.get(id, j);
                            trans[count][0] = id;
                            trans[count][1] = value;
                            count++;
                        }
                        for (int x = 0; x < add_size; x++) {
                            for (int y = x + 1; y < trans.length; y++) {
                                double x_value = trans[x][1];
                                double y_value = trans[y][1];
                                if (x_value < y_value){
                                    for (int z = 0; z < 2; z++){
                                        double tran = trans[x][z];
                                        trans[x][z] = trans[y][z];
                                        trans[y][z] = tran;
                                    }
                                }
                            }
                        }
                        for (int x = 0; x < add_size; x++) {
                            int id = (int) (trans[x][0]);
                            double value = trans[x][1];
                            itemCorrsAR_added.put(id, j, value);
                        }
                    }
                    else {
                        storeCAR(j);
                    }
                }
            }
            else {
                storeCAR(j);
            }
        }
    }

    /**
     * function to implement the whole preprocess
     */
    protected void preprocess() {
        computeAR();
        sortAR();
        computeItemset();
        computeGAR();
        sortGAR();
        addAR();
    }


}
