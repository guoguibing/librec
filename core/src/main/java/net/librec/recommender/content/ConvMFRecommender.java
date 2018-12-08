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

import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.conf.Configured;
import net.librec.math.structure.*;
import net.librec.recommender.TensorRecommender;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.PoolingType;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConvMF Recommender
 * Donghyun Kim et al., Convolutional Matrix Factorization for Document Context-aware Recommendation
 * {@code Proceedings of the 10th ACM Conference on Recommender Systems. ACM, 2016.}.
 *
 * @author SunYatong
 */
public class ConvMFRecommender extends TensorRecommender {

    protected SequentialAccessSparseMatrix trainMatrix;
    /**
     * user latent factors
     */
    protected DenseMatrix userFactors;

    /**
     * item latent factors
     */
    protected DenseMatrix itemFactors;
    /**
     * user regularization
     */
    protected float lambda_u;

    /**
     * item regularization
     */
    protected double lambda_v;

    /**
     * path to the pre-trained word2vec file
     */
    protected String pretrain_w2v_path;

    /**
     * dimension of the word vector
     */
    protected int w2v_dim;

    /**
     * Max length of the document
     */
    protected int max_len;

    /**
     * Number of feature maps / channels / depth for each CNN layer
     */
    protected int featureMapNum;

    /**
     * review index in tensor and its string content
     */
    public BiMap<Integer, String> reviewMappingData;

    /**
     * Each itemIdx mapping to a String of its all reviews
     */
    protected Map<Integer, StringBuilder> itemIdx2document;

    /**
     * The CNN module
     */
    protected CNN_Module cnn_module;

    @Override
    protected void setup() throws LibrecException {
        super.setup();
        reviewMappingData = DataFrame.getInnerMapping("review").inverse();
        lambda_u = conf.getFloat("rec.user.regularization", 0.1f);
        lambda_v = conf.getFloat("rec.item.regularization", 0.1f);
        trainTensor = (SparseTensor) getDataModel().getTrainDataSet();
        userFactors = new DenseMatrix(numUsers, numFactors);
        itemFactors = new DenseMatrix(numItems, numFactors);
        userFactors.init(1);
        itemFactors.init(1);

        pretrain_w2v_path = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + conf.get("rec.word2vec.path");
        w2v_dim = conf.getInt("rec.word2vec.dimension");
        max_len = conf.getInt("rec.document.length");
        featureMapNum = conf.getInt("rec.featuremap.num");

        trainMatrix = trainTensor.rateMatrix();

        itemIdx2document = new HashMap<>();
        for (int i=0; i<numItems; i++) {
            itemIdx2document.put(i, new StringBuilder());
        }

        for (TensorEntry te : trainTensor) {
            int[] entryKeys = te.keys();
            int itemIndex = entryKeys[1];
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.get(reviewIndex);
            String reviewContentString = reviewContent.replaceAll(":", " ").replaceAll("#", " ");
            itemIdx2document.get(itemIndex).append(reviewContentString).append(".");
        }

        for (TensorEntry te : testTensor) {
            int[] entryKeys = te.keys();
            int itemIndex = entryKeys[1];
            int reviewIndex = entryKeys[2];
            String reviewContent = reviewMappingData.get(reviewIndex);
            String reviewContentString = reviewContent.replaceAll(":", " ").replaceAll("#", " ");
            itemIdx2document.get(itemIndex).append(reviewContentString).append(".");
        }

        cnn_module = new CNN_Module();
    }

    @Override
    protected void trainModel() throws LibrecException {
        // build a identity matrix
        DenseMatrix identify = new DenseMatrix(numFactors, numFactors);
        identify.init(0.0);
        for (int factorIdx=0; factorIdx<numFactors; factorIdx++) {
            identify.set(factorIdx, factorIdx, 1.0);
        }
        // training
        for (int iter = 1; iter <= numIterations; iter++) {
            loss = 0.0;
            cnn_module.trainCNN();
            // fix item matrix M, solve user matrix userFactors
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                // number of items rated by user userIdx
                int u_numItems = trainMatrix.row(userIdx).getNumEntries();
                DenseMatrix M = new DenseMatrix(u_numItems, numFactors);
                int[] itemIdices = trainMatrix.row(userIdx).getIndices();
                SequentialSparseVector userVec = trainMatrix.row(userIdx);
                int index = 0;
                for (int itemIdx : itemIdices) {
                    M.set(index++, itemFactors.row(itemIdx));
                }
                // step 1:
                DenseMatrix A = M.transpose().times(M).plus(identify.times(lambda_u).times(u_numItems));
                // step 2:
                // ratings of this userIdx
                int index1 = 0;
                VectorBasedDenseVector userVector = new VectorBasedDenseVector(u_numItems);
                for (Vector.VectorEntry ve : userVec) {
                    double realRating = ve.get();
                    userVector.set(index1++, realRating);
                }

                // step 3: the updated user matrix wrt user j
                userFactors.set(userIdx, A.inverse().times(M.transpose().times(userVector)));
            }

            // fix user matrix userFactors, solve item matrix M
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                // latent factor of users that have rated item itemIdx
                // number of users rate item j
                int i_numUsers = trainMatrix.column(itemIdx).getNumEntries();
                DenseMatrix U = new DenseMatrix(i_numUsers, numFactors);
                int[] userIdices = trainMatrix.column(itemIdx).getIndices();
                SequentialSparseVector itemVec = trainMatrix.column(itemIdx);
                int index = 0;
                for (int userIdx : userIdices) {
                    U.set(index++, userFactors.row(userIdx));
                }
                if (i_numUsers == 0)
                    continue;
                // step 1:
                DenseMatrix A = U.transpose().times(U).plus(identify.times(lambda_v).times(i_numUsers));
                // step 2:
                // ratings of this item
                VectorBasedDenseVector itemVector = new VectorBasedDenseVector(i_numUsers);
                int index1 = 0;
                for (Vector.VectorEntry ve : itemVec) {
                    double realRating = ve.get();
                    itemVector.set(index1++, realRating);
                }
                // step 3: the updated item matrix wrt item j
                itemFactors.set(itemIdx, A.inverse().times(U.transpose().times(itemVector).plus(cnn_module.getOutput(itemIdx2document.get(itemIdx).toString()).times(lambda_v))));
            }

            // compute rating loss
            for (MatrixEntry me : trainMatrix) {
                int userIdx = me.row();
                int itemIdx = me.column();
                double error = me.get() - userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
                loss += error * error;
            }

            LOG.info("iter: " + iter + ", loss: " + loss);
        }
    }

    @Override
    protected double predict(int[] indices) {
        return predict(indices[0], indices[1]);
    }

    protected double predict(int userIdx, int itemIdx) {
        return userFactors.row(userIdx).dot(itemFactors.row(itemIdx));
    }

    private class CNN_Module {

        ComputationGraph net;
        ConvMFDocumentDataSetIterator trainIter;
        int nEpochs = 5;
        int batchSize = 32;
        int vectorSize = w2v_dim;
        int truncateReviewsToLength = max_len;
        int cnnLayerFeatureMaps = featureMapNum;

        CNN_Module () {

            PoolingType globalPoolingType = PoolingType.MAX;

            ComputationGraphConfiguration config = new NeuralNetConfiguration.Builder()
                    .weightInit(WeightInit.RELU)
                    .activation(Activation.LEAKYRELU)
                    .updater(Updater.ADADELTA)
                    .convolutionMode(ConvolutionMode.Same)
                    .regularization(true).dropOut(0.2)
                    .learningRate(learnRate)
                    .graphBuilder()
                    .addInputs("input")
                    .addLayer("cnn3", new ConvolutionLayer.Builder()
                            .kernelSize(3,vectorSize)
                            .stride(1,vectorSize)
                            .nIn(1)
                            .nOut(cnnLayerFeatureMaps)
                            .build(), "input")
                    .addLayer("cnn4", new ConvolutionLayer.Builder()
                            .kernelSize(4,vectorSize)
                            .stride(1,vectorSize)
                            .nIn(1)
                            .nOut(cnnLayerFeatureMaps)
                            .build(), "input")
                    .addLayer("cnn5", new ConvolutionLayer.Builder()
                            .kernelSize(5,vectorSize)
                            .stride(1,vectorSize)
                            .nIn(1)
                            .nOut(cnnLayerFeatureMaps)
                            .build(), "input")
                    .addVertex("merge", new MergeVertex(), "cnn3", "cnn4", "cnn5")      //Perform depth concatenation
                    .addLayer("globalPool", new GlobalPoolingLayer.Builder()
                            .poolingType(globalPoolingType)
                            .build(), "merge")
                    .addLayer("out", new OutputLayer.Builder()
                            .lossFunction(LossFunctions.LossFunction.MSE)
                            .activation(Activation.RELU)
                            .nIn(3*cnnLayerFeatureMaps)
                            .nOut(numFactors)
                            .build(), "globalPool")
                    .setOutputs("out")
                    .build();

            this.net = new ComputationGraph(config);
            this.net.init();
            // Load word vectors and get the DataSetIterators for training
            System.out.println("Loading word vectors and creating DataSetIterators");
            WordVectors wordVectors = WordVectorSerializer.loadStaticModel(new File(pretrain_w2v_path));
            trainIter = getDataSetIterator(wordVectors, batchSize, truncateReviewsToLength);
        }

        void trainCNN() {
            for (int i = 0; i < nEpochs; i++) {
                net.fit(trainIter);
            }
        }

        VectorBasedDenseVector getOutput(String inputDocument) {
            INDArray predictions = net.outputSingle(trainIter.loadSingleSentence(inputDocument));
            VectorBasedDenseVector outputVec = new VectorBasedDenseVector(numFactors);
            for (int i=0; i<numFactors; i++) {
                outputVec.set(i, predictions.getDouble(i));
            }
            return outputVec;
        }

        private ConvMFDocumentDataSetIterator getDataSetIterator(WordVectors wordVectors, int minibatchSize, int maxSentenceLength) {
            List<String> documents = new ArrayList<>();
            for (int i=0; i<numItems; i++) {
                documents.add(itemIdx2document.get(i).toString());
            }

            List<double[]> labelsForDocuments = new ArrayList<>();
            for (int i = 0; i<numItems; i++) {
                labelsForDocuments.add(itemFactors.row(i).getValues());
            }

            ConvMFDocumentProvider documentProvider = new ConvMFDocumentProvider(documents, labelsForDocuments);

            return new ConvMFDocumentDataSetIterator.Builder()
                    .documentProvider(documentProvider)
                    .wordVectors(wordVectors)
                    .minibatchSize(minibatchSize)
                    .maxSentenceLength(maxSentenceLength)
                    .useNormalizedWordVectors(false)
                    .build();
        }
    }
}
