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
package net.librec.recommender.content;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Maths;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseStringMatrix;
import net.librec.recommender.cf.rating.BiasedMFRecommender;
import net.librec.util.FileUtil;
import net.librec.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * HFT Recommender
 *
 * @author ChenXu
 */
public class HFTRecommender extends BiasedMFRecommender {

    protected DenseMatrix Y;
    protected SparseStringMatrix reviewMatrix;
    protected DenseMatrix topicToWord;
    protected SparseStringMatrix topicAssignment;
    protected int K = 10;
    protected int numberOfWords;

    protected int numberOfUsers;
    protected int numberOfItems;
    protected StringUtil str = new StringUtil();
    protected Randoms rn = new Randoms();
    protected double[][] thetaus;
    protected double[][] phiks;
    

    public HFTRecommender() {
        super();
    }

    public HFTRecommender(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
        super();
    }

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        Map<String, String> iwDict = new HashMap<String, String>();
        Map<String, String> userDict = new HashMap<String, String>();
        Map<String, String> itemDict = new HashMap<String, String>();
        
        
        List<BufferedReader> readerList = new ArrayList<BufferedReader>();
        try {
            readerList = FileUtil.getReader(conf.get("dfs.data.dir")+"/"+conf.get("data.input.path"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        numberOfWords = 0;
        numberOfUsers = 0;
        numberOfItems = 0;
        String user = "";
        String item = "";
        int r = 0;
        String line = null;
        Table<Integer, Integer, String> res = HashBasedTable.create();
        Table<Integer, Integer, Double> ratings = HashBasedTable.create();
        
        try {
        	for (BufferedReader reader : readerList) {
                while ((line = reader.readLine()) != null) { 
                    String[] recordList = line.split(" ");
                    user = recordList[0];
                	item = recordList[1];
                    String[] fSPList = recordList[3].split(":");
                    
                    if (!userDict.containsKey(user)) {
                        userDict.put(user, String.valueOf(numberOfUsers));
                        numberOfUsers++;
                    }

                    if (!itemDict.containsKey(item)) {
                        itemDict.put(item, String.valueOf(numberOfItems));
                        numberOfItems++;
                    }

                    for (String p : fSPList) {
                        if (!iwDict.containsKey(p)) {
                        	iwDict.put(p, String.valueOf(numberOfWords));
                        	numberOfWords++;
                        }        
                    }
                    res.put(Integer.parseInt(user), Integer.parseInt(item), recordList[3]);
                    ratings.put(Integer.parseInt(user), Integer.parseInt(item), (double) Integer.parseInt(recordList[2]));
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        reviewMatrix = new SparseStringMatrix(numUsers, numItems, res);
        trainMatrix = new SparseMatrix(numUsers, numItems, ratings);
        topicToWord = new DenseMatrix(K, numberOfWords);
        topicToWord.init(initMean, initStd);
        topicAssignment = new SparseStringMatrix(reviewMatrix);
        thetaus = new double[numberOfUsers][K];
        phiks = new double[K][numberOfWords];
        //testMatrix = new SparseMatrix(numUsers, numItems, ratings);
        
        
        for (MatrixEntry me : trainMatrix) {
            int u = me.row(); // user
            int j = me.column(); // item
            String words = reviewMatrix.get(u, j);
            String[] wordsList = words.split(":");
            String[] topicList = new String[wordsList.length];
            for (int i = 0; i < wordsList.length; i++) {
                topicList[i] = Integer.toString(rn.uniform(K));
            }
            String s = str.toString(topicList, ":");
            topicAssignment.set(u, j, s);
        }

        for (int i = 0; i < numUsers; i++) {
            try {
                thetaus[i] = theta(i);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (int i = 0; i < K; i++) {
            try {
                phiks[i] = phi(i);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void sampleZ() throws Exception {
        //double[][] thetaus = new double[numUsers][K];
        //double[][] phiks = new double[K][numberOfWords];

        for (int i = 0; i < numUsers; i++) {
            thetaus[i] = theta(i);
        }
        for (int i = 0; i < K; i++) {
            phiks[i] = phi(i);
        }

        for (MatrixEntry me : trainMatrix) {
            int u = me.row(); // user
            int j = me.column(); // item
            String words = reviewMatrix.get(u, j);
            String[] wordsList = words.split(":");
            String[] topicList = new String[wordsList.length];

            for (int i = 0; i < wordsList.length; i++) {
                double[] topicDistribute = new double[K];
                for (int s = 0; s < K; s++) {
                    topicDistribute[s] = thetaus[u][s] * phiks[s][Integer.parseInt(wordsList[i])];
                }
                topicDistribute = Maths.norm(topicDistribute);
                topicList[i] = Integer.toString(rn.discrete(topicDistribute));
            }
            String s = str.toString(topicList, ":");
            topicAssignment.set(u, j, s);
        }
    }

    protected double[] theta(int u) throws Exception {
        return Maths.softmax(userFactors.row(u).getData());
    }

    protected double[] phi(int k) throws Exception {
        return Maths.softmax(topicToWord.row(k).getData());
    }

    @Override
    protected void trainModel() {
        for (int iter = 1; iter <= 2; iter++) {
            // SGD training
            for (int sgditer = 1; sgditer <= conf.getDouble("rec.iterator.maximum"); sgditer++) {
                double loss = 0;
                for (MatrixEntry me : trainMatrix) {

                    int u = me.row(); // user
                    int j = me.column(); // item
                    double ruj = me.get();
                    String[] ws = reviewMatrix.get(u, j).split(":");
                    String[] wk = topicAssignment.get(u, j).split(":");

                    double pred = predict(u, j);
                    double euj = ruj - pred;

                    loss += euj * euj;

                    // update factors
                    double bu = userBiases.get(u);
                    double sgd = euj - regBias * bu;
                    userBiases.add(u, learnRate * sgd);
                    // loss += regB * bu * bu;
                    double bj = itemBiases.get(j);
                    sgd = euj - regBias * bj;
                    itemBiases.add(j, learnRate * sgd);
                    // loss += regB * bj * bj;

                    for (int f = 0; f < numFactors; f++) {
                        double puf = userFactors.get(u, f);
                        double qjf = itemFactors.get(j, f);

                        double sgd_u = euj * qjf - regUser * puf;
                        double sgd_j = euj * (puf) - regItem * qjf;

                        userFactors.add(u, f, learnRate * sgd_u);

                        itemFactors.add(j, f, learnRate * sgd_j);

                        for (int x = 0; x < ws.length; x++) {
                            int k = Integer.parseInt(wk[x]);
                            if (f == k)
                                userFactors.add(u, f, learnRate * (1 - thetaus[u][k]));
                            else
                                userFactors.add(u, f, learnRate * (-thetaus[u][k]));
                            loss -= Maths.log(thetaus[u][k], 2) + Maths.log(phiks[k][Integer.parseInt(ws[x])], 2);
                        }

                    }
                    for (int x = 0; x < ws.length; x++) {
                        int k = Integer.parseInt(wk[x]);
                        for (int ss = 0; ss < numberOfWords; ss++) {
                            if (ss == Integer.parseInt(ws[x]))
                                topicToWord.add(k, Integer.parseInt(ws[x]), learnRate * (-1 + phiks[k][Integer.parseInt(ws[x])]));
                            else
                                topicToWord.add(k, Integer.parseInt(ws[x]), learnRate * (phiks[k][Integer.parseInt(ws[x])]));
                        }
                    }
                }
                loss *= 0.5;
            } // end of SGDtraining
            try {
                sampleZ();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    protected double predict(int u, int j) {
        double pred = globalMean + userBiases.get(u) + itemBiases.get(j) + DenseMatrix.rowMult(userFactors, u, itemFactors, j);
        return pred;
    }

}
