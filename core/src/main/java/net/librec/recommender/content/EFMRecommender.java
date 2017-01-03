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
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseMatrix;
import net.librec.recommender.cf.rating.BiasedMFRecommender;
import net.librec.util.FileUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EFM Recommender
 *
 * @author ChenXu
 */
public class EFMRecommender extends BiasedMFRecommender {
    protected int numberOfFeatures;
    protected int numberOfUsers;
    protected int numberOfItems;
    protected int featureFactor = 5;
    protected int scoreScale = 5;
    protected DenseMatrix featureMatrix;
    protected DenseMatrix userFeatureMatrix;
    protected DenseMatrix userHiddenMatrix;
    protected DenseMatrix itemFeatureMatrix;
    protected DenseMatrix itemHiddenMatrix;
    protected DenseMatrix userFeatureAttention;
    protected DenseMatrix itemFeatureQuality;
    protected DenseMatrix rating;
    protected SparseMatrix trainData,testData;
    protected double lambdaX;
    protected double lambdaY;
    protected double lambdaU;
    protected double lambdaH;
    protected double lambdaV;

    /*
     * (non-Javadoc)
     *
     * @see net.librec.recommender.AbstractRecommender#setup()
     */
    @Override
    protected void setup() throws LibrecException {
        super.setup();
        Map<String, String> featureDict = new HashMap<String, String>();
        Map<String, String> userDict = new HashMap<String, String>();
        Map<String, String> itemDict = new HashMap<String, String>();

        Map<String, String> userFeatureDict = new HashMap<String, String>();
        Map<String, String> itemFeatureDict = new HashMap<String, String>();

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
        numberOfFeatures = 0;
        numberOfUsers = 0;
        numberOfItems = 0;
        String user = "";
        String item = "";
        int r = 0;
        String line = null;

        try {
            for (BufferedReader reader : readerList) {
                while ((line = reader.readLine()) != null) {
                    String[] recordList = line.split(" ");
                    user = recordList[0];
                    item = recordList[1];
                    String featureSentimentPairsString = recordList[3];
                    String[] fSPList = featureSentimentPairsString.split(",");
                    if (!userDict.containsKey(user)) {
                        userDict.put(user, String.valueOf(numberOfUsers));
                        numberOfUsers++;
                    }

                    if (!itemDict.containsKey(item)) {
                        itemDict.put(item, String.valueOf(numberOfItems));
                        numberOfItems++;
                    }

                    for (String p : fSPList) {
                        String k = p.split(":")[0];
                        if (!featureDict.containsKey(k)) {
                            featureDict.put(k, String.valueOf(numberOfFeatures));
                            numberOfFeatures++;
                        }
                        if (userFeatureDict.containsKey(user)) {
                            userFeatureDict.put(user, userFeatureDict.get(user) + "," + featureSentimentPairsString);
                        } else {
                            userFeatureDict.put(user, featureSentimentPairsString);
                        }
                        if (itemFeatureDict.containsKey(item)) {
                            itemFeatureDict.put(item, itemFeatureDict.get(item) + featureSentimentPairsString);
                        } else {
                            itemFeatureDict.put(item, featureSentimentPairsString);
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        // Create V,U1,H1,U2,H2
        featureMatrix = new DenseMatrix(numberOfFeatures, featureFactor);
        userFactors = new DenseMatrix(numberOfUsers, numFactors);
        itemFactors = new DenseMatrix(numberOfItems, numFactors);
        
        
        featureMatrix.init(initMean, initStd);
        userFeatureMatrix = userFactors.getSubMatrix(0, userFactors.numRows() - 1, 0, featureFactor - 1);
        userHiddenMatrix = userFactors.getSubMatrix(0, userFactors.numRows() - 1, featureFactor, userFactors.numColumns() - 1);
        itemFeatureMatrix = itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, 0, featureFactor - 1);
        itemHiddenMatrix = itemFactors.getSubMatrix(0, itemFactors.numRows() - 1, featureFactor, itemFactors.numColumns() - 1);

        userFeatureAttention = new DenseMatrix(userFactors.numRows(), numberOfFeatures);
        userFeatureAttention.init(0);
        itemFeatureQuality = new DenseMatrix(itemFactors.numRows(), numberOfFeatures);
        itemFeatureQuality.init(0);
        

        // compute UserFeatureAttention
        double[] featureValues = new double[numberOfFeatures];
        for (int i = 0; i < numberOfFeatures; i++) {
            featureValues[i] = 0.0;
        }
        for (String u : userFeatureDict.keySet()) {

            String[] fList = userFeatureDict.get(u).split(",");
            for (String a : fList) {
                int fin = Integer.parseInt(featureDict.get(a.split(":")[0]));
                featureValues[fin] += 1;
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) * (2 / (1 + Math.exp(-featureValues[i])) - 1);
                    userFeatureAttention.set(Integer.parseInt(userDict.get(u)), i, v);
                }
            }
        }

        // Compute ItemFeatureQuality
        for (int i = 0; i < numberOfFeatures; i++) {
            featureValues[i] = 0.0;
        }
        for (String p : itemFeatureDict.keySet()) {
            String[] fList = itemFeatureDict.get(p).split(",");
            for (String a : fList) {
                int fin = Integer.parseInt(featureDict.get(a.split(":")[0]));
                featureValues[fin] += Double.parseDouble(a.split(":")[1]);
            }
            for (int i = 0; i < numberOfFeatures; i++) {
                if (featureValues[i] != 0.0) {
                    double v = 1 + (scoreScale - 1) / (1 + Math.exp(-featureValues[i]));
                    itemFeatureQuality.set(Integer.parseInt(itemDict.get(p)), i, v);
                }
            }
        }

    }

    @Override
    protected void trainModel() throws LibrecException {
        for (int iter = 1; iter <= 10; iter++) {
            loss = 0.0;
            // Update featureMatrix
            for (int i = 0; i < featureMatrix.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double updateValue = ((userFeatureAttention.transpose().mult(userFeatureMatrix).scale(lambdaX)).add(itemFeatureQuality.transpose().mult(itemFeatureMatrix).scale(lambdaX))).get(i, j);
                    updateValue /= featureMatrix.mult((userFeatureMatrix.transpose().mult(userFeatureMatrix).scale(lambdaX)).add(itemFeatureMatrix.transpose().mult(itemFeatureMatrix).scale(lambdaY))
                    		.add(DenseMatrix.eye(featureFactor).scale(lambdaV))).get(i, j);
                    updateValue = Math.sqrt(updateValue);
                    featureMatrix.set(i, j, featureMatrix.get(i, j) * updateValue);
                }
            }
            // Update UserFeatureMatrix
            for (int i = 0; i < userFactors.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double updateValue = ((itemFeatureMatrix.transpose().mult(trainMatrix.transpose())).transpose().add(userFeatureAttention.mult(featureMatrix).scale(lambdaX))).get(i, j);
                    updateValue /= (userFeatureMatrix.mult(itemFeatureMatrix.transpose()).add(userHiddenMatrix.mult(itemHiddenMatrix.transpose())).mult(itemFeatureMatrix)
                            .add(userFeatureMatrix.mult(featureMatrix.transpose().mult(featureMatrix).scale(lambdaX).add(DenseMatrix.eye(featureFactor).scale(lambdaU))))).get(i, j);
                    updateValue = Math.sqrt(updateValue);
                    userFeatureMatrix.set(i, j, userFeatureMatrix.get(i, j) * updateValue);
                }
            }
            // Update ItemFeatureMatrix
            for (int i = 0; i < itemFactors.numRows(); i++) {
                for (int j = 0; j < featureFactor; j++) {
                    double updateValue = (userFeatureMatrix.transpose().mult(trainMatrix).transpose().add(itemFeatureQuality.mult(featureMatrix).scale(lambdaY))).get(i, j);
                    updateValue /= (itemFeatureMatrix.mult(userFeatureMatrix.transpose()).add(itemHiddenMatrix.mult(userHiddenMatrix.transpose())).mult(userFeatureMatrix)
                            .add(itemFeatureMatrix.mult(featureMatrix.transpose().mult(featureMatrix).scale(lambdaX).add(DenseMatrix.eye(featureFactor).scale(lambdaU))))).get(i, j);
                    updateValue = Math.sqrt(updateValue);
                    itemFeatureMatrix.set(i, j, itemFeatureMatrix.get(i, j) * updateValue);
                }
            }
            // Update UserHiddenMatrix
            for (int i = 0; i < userFactors.numRows(); i++) {
                for (int j = 0; j < userFactors.numColumns() - featureFactor; j++) {

                    double updateValue = (itemHiddenMatrix.transpose().mult(trainMatrix.transpose()).transpose()).get(i, j);

                    updateValue /= (userFeatureMatrix.mult(itemFeatureMatrix.transpose()).add(userHiddenMatrix.mult(itemHiddenMatrix.transpose())).mult(itemHiddenMatrix).add(userHiddenMatrix.scale(lambdaH))).get(i, j);

                    updateValue = Math.sqrt(updateValue);

                    userHiddenMatrix.set(i, j, userHiddenMatrix.get(i, j) * updateValue);
                }
            }
            // Update ItemHiddenMatrix
            for (int i = 0; i < itemFactors.numRows(); i++) {
                for (int j = 0; j < itemFactors.numColumns() - featureFactor; j++) {

                    double updateValue = (userHiddenMatrix.transpose().mult(trainMatrix).transpose()).get(i, j);

                    updateValue /= (itemFeatureMatrix.mult(userFeatureMatrix.transpose()).add(itemHiddenMatrix.mult(userHiddenMatrix.transpose())).mult(userHiddenMatrix).add(itemHiddenMatrix.scale(lambdaH))).get(i, j);
                    updateValue = Math.sqrt(updateValue);
                    itemHiddenMatrix.set(i, j, itemHiddenMatrix.get(i, j) * updateValue);
                }
            }

            // Compute loss value
            loss += Math.pow((userFeatureMatrix.mult(itemFeatureMatrix.transpose()).minus(userHiddenMatrix.mult(itemHiddenMatrix.transpose()))).minus(trainMatrix).norm(), 2);
            loss += lambdaX * Math.pow((userFeatureMatrix.mult(featureMatrix.transpose()).minus(userFeatureAttention)).norm(), 2);
            loss += lambdaY * Math.pow((itemFeatureMatrix.mult(featureMatrix.transpose()).minus(itemFeatureQuality)).norm(), 2);
            loss += lambdaU * Math.pow((userFeatureMatrix.norm() + itemFeatureMatrix.norm()), 2);
            loss += lambdaH * (Math.pow(userHiddenMatrix.norm(), 2) + Math.pow(itemHiddenMatrix.norm(), 2));
            loss += lambdaV * Math.pow(featureMatrix.norm(), 2);
        }
    }

    @Override
    protected double predict(int u, int j) {
        double pred = DenseMatrix.rowMult(userFeatureMatrix, u, itemFeatureMatrix, j) + DenseMatrix.rowMult(userHiddenMatrix, u, itemHiddenMatrix, j);
        return pred;
    }
}
