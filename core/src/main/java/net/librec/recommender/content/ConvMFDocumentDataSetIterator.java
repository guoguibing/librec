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

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.primitives.Pair;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * DataSetIterator for ConvMFRecommender
 * Donghyun Kim et al., Convolutional Matrix Factorization for Document Context-aware Recommendation
 * {@code Proceedings of the 10th ACM Conference on Recommender Systems. ACM, 2016.}.
 *
 * @author SunYatong
 */
public class ConvMFDocumentDataSetIterator implements DataSetIterator {
    private static final String UNKNOWN_WORD_SENTINEL = "UNKNOWN_WORD_SENTINEL";
    private ConvMFDocumentProvider documentProvider;
    private WordVectors wordVectors;
    private TokenizerFactory tokenizerFactory;
    private ConvMFDocumentDataSetIterator.UnknownWordHandling unknownWordHandling;
    private boolean useNormalizedWordVectors;
    private int minibatchSize;
    private int maxSentenceLength;
    private boolean sentencesAlongHeight;
    private DataSetPreProcessor dataSetPreProcessor;
    private int wordVectorSize;
    private INDArray unknown;
    private int cursor;
    private int labelSize;
    private int numDoc;

    private ConvMFDocumentDataSetIterator(ConvMFDocumentDataSetIterator.Builder builder) {
        this.documentProvider = null;
        this.cursor = 0;
        this.documentProvider = builder.documentProvider;
        this.wordVectors = builder.wordVectors;
        this.tokenizerFactory = builder.tokenizerFactory;
        this.unknownWordHandling = builder.unknownWordHandling;
        this.useNormalizedWordVectors = builder.useNormalizedWordVectors;
        this.minibatchSize = builder.minibatchSize;
        this.maxSentenceLength = builder.maxSentenceLength;
        this.sentencesAlongHeight = builder.sentencesAlongHeight;
        this.dataSetPreProcessor = builder.dataSetPreProcessor;
        this.labelSize = this.documentProvider.getLabelSize();
        this.numDoc = this.documentProvider.getNumDoc();

        if(this.unknownWordHandling == ConvMFDocumentDataSetIterator.UnknownWordHandling.UseUnknownVector) {
            if(this.useNormalizedWordVectors) {
                this.wordVectors.getWordVectorMatrixNormalized(this.wordVectors.getUNK());
            } else {
                this.wordVectors.getWordVectorMatrix(this.wordVectors.getUNK());
            }
        }

        this.wordVectorSize = this.wordVectors.getWordVector(this.wordVectors.vocab().wordAtIndex(0)).length;
    }

    public INDArray loadSingleSentence(String sentence) {
        List<String> tokens = this.tokenizeSentence(sentence);
        int[] featuresShape = new int[]{1, 1, 0, 0};
        if(this.sentencesAlongHeight) {
            featuresShape[2] = Math.min(this.maxSentenceLength, tokens.size());
            featuresShape[3] = this.wordVectorSize;
        } else {
            featuresShape[2] = this.wordVectorSize;
            featuresShape[3] = Math.min(this.maxSentenceLength, tokens.size());
        }

        INDArray features = Nd4j.create(featuresShape);
        int length = this.sentencesAlongHeight?featuresShape[2]:featuresShape[3];

        for(int i = 0; i < length; ++i) {
            INDArray vector = this.getVector(tokens.get(i));
            INDArrayIndex[] indices = new INDArrayIndex[]{NDArrayIndex.point(0), NDArrayIndex.point(0), null, null};
            if(this.sentencesAlongHeight) {
                indices[2] = NDArrayIndex.point(i);
                indices[3] = NDArrayIndex.all();
            } else {
                indices[2] = NDArrayIndex.all();
                indices[3] = NDArrayIndex.point(i);
            }
            features.put(indices, vector);
        }
        return features;
    }

    private INDArray getVector(String word) {
        INDArray vector;
        if(this.unknownWordHandling == ConvMFDocumentDataSetIterator.UnknownWordHandling.UseUnknownVector && word == "UNKNOWN_WORD_SENTINEL") {
            vector = this.unknown;
        } else if(this.useNormalizedWordVectors) {
            vector = this.wordVectors.getWordVectorMatrixNormalized(word);
        } else {
            vector = this.wordVectors.getWordVectorMatrix(word);
        }

        return vector;
    }

    private List<String> tokenizeSentence(String sentence) {
        Tokenizer t = this.tokenizerFactory.create(sentence);
        ArrayList tokens = new ArrayList();

        while(true) {
            String token;
            label22:
            while(true) {
                if(!t.hasMoreTokens()) {
                    return tokens;
                }

                token = t.nextToken();
                if(this.wordVectors.hasWord(token)) {
                    break;
                }
            }
            tokens.add(token);
        }
    }

    public boolean hasNext() {
        if(this.documentProvider == null) {
            throw new UnsupportedOperationException("Cannot do next/hasNext without a sentence provider");
        } else {
            return this.documentProvider.hasNext();
        }
    }

    public DataSet next() {
        return this.next(this.minibatchSize);
    }

    public DataSet next(int num) {
        if(this.documentProvider == null) {
            throw new UnsupportedOperationException("Cannot do next/hasNext without a sentence provider");
        } else {
            List<Pair<List<String>, double[]>> tokenizedSentences = new ArrayList(num);
            int maxLength = -1;
            int minLength = 2147483647;

            int currMinibatchSize;
            for(currMinibatchSize = 0; currMinibatchSize < num && this.documentProvider.hasNext(); ++currMinibatchSize) {
                Pair<String, double[]> p = this.documentProvider.nextSentence();
                List<String> tokens = this.tokenizeSentence(p.getFirst());
                maxLength = Math.max(maxLength, tokens.size());
                tokenizedSentences.add(new Pair(tokens, p.getSecond()));
            }

            if(this.maxSentenceLength > 0 && maxLength > this.maxSentenceLength) {
                maxLength = this.maxSentenceLength;
            }

            currMinibatchSize = tokenizedSentences.size();

            double[][] labelsData = new double[currMinibatchSize][this.labelSize];
            for(int i = 0; i < tokenizedSentences.size(); ++i) {
                double[] labelArray = tokenizedSentences.get(i).getSecond();
                labelsData[i] = labelArray;
            }
            INDArray labels = new NDArray(labelsData);

            int[] featuresShape = new int[]{currMinibatchSize, 1, 0, 0};
            if(this.sentencesAlongHeight) {
                featuresShape[2] = maxLength;
                featuresShape[3] = this.wordVectorSize;
            } else {
                featuresShape[2] = this.wordVectorSize;
                featuresShape[3] = maxLength;
            }

            INDArray features = Nd4j.create(featuresShape);

            int sentenceLength;
            for(int i = 0; i < currMinibatchSize; ++i) {
                List<String> currSentence = tokenizedSentences.get(i).getFirst();

                for(sentenceLength = 0; sentenceLength < currSentence.size() && sentenceLength < this.maxSentenceLength; ++sentenceLength) {
                    INDArray vector = this.getVector(currSentence.get(sentenceLength));
                    INDArrayIndex[] indices = new INDArrayIndex[]{NDArrayIndex.point(i), NDArrayIndex.point(0), null, null};
                    if(this.sentencesAlongHeight) {
                        indices[2] = NDArrayIndex.point(sentenceLength);
                        indices[3] = NDArrayIndex.all();
                    } else {
                        indices[2] = NDArrayIndex.all();
                        indices[3] = NDArrayIndex.point(sentenceLength);
                    }
                    features.put(indices, vector);
                }
            }

            INDArray featuresMask = null;
            if(minLength != maxLength) {
                featuresMask = Nd4j.create(currMinibatchSize, maxLength);

                for(int i = 0; i < currMinibatchSize; ++i) {
                    sentenceLength = ((List)((Pair)tokenizedSentences.get(i)).getFirst()).size();
                    if(sentenceLength >= maxLength) {
                        featuresMask.getRow(i).assign(Double.valueOf(1.0D));
                    } else {
                        featuresMask.get(new INDArrayIndex[]{NDArrayIndex.point(i), NDArrayIndex.interval(0, sentenceLength)}).assign(Double.valueOf(1.0D));
                    }
                }
            }

            DataSet ds = new DataSet(features, labels, featuresMask, null);
            if(this.dataSetPreProcessor != null) {
                this.dataSetPreProcessor.preProcess(ds);
            }

            this.cursor += ds.numExamples();
            return ds;
        }
    }

    public INDArray loadAllDocuments() {
        reset();
        List<Pair<List<String>, double[]>> tokenizedSentences = new ArrayList(numDoc);
        int maxLength = -1;

        int currMinibatchSize;
        for(currMinibatchSize = 0; this.documentProvider.hasNext(); ++currMinibatchSize) {
            Pair<String, double[]> p = this.documentProvider.nextSentence();
            List<String> tokens = this.tokenizeSentence(p.getFirst());
            maxLength = Math.max(maxLength, tokens.size());
            tokenizedSentences.add(new Pair(tokens, p.getSecond()));
        }

        if(this.maxSentenceLength > 0 && maxLength > this.maxSentenceLength) {
            maxLength = this.maxSentenceLength;
        }

        currMinibatchSize = tokenizedSentences.size();
        int[] featuresShape = new int[]{currMinibatchSize, 1, 0, 0};
        if(this.sentencesAlongHeight) {
            featuresShape[2] = maxLength;
            featuresShape[3] = this.wordVectorSize;
        } else {
            featuresShape[2] = this.wordVectorSize;
            featuresShape[3] = maxLength;
        }

        INDArray features = Nd4j.create(featuresShape);

        int sentenceLength;
        for(int i = 0; i < currMinibatchSize; ++i) {
            List<String> currSentence = tokenizedSentences.get(i).getFirst();

            for(sentenceLength = 0; sentenceLength < currSentence.size() && sentenceLength < this.maxSentenceLength; ++sentenceLength) {
                INDArray vector = this.getVector(currSentence.get(sentenceLength));
                INDArrayIndex[] indices = new INDArrayIndex[]{NDArrayIndex.point(i), NDArrayIndex.point(0), null, null};
                if(this.sentencesAlongHeight) {
                    indices[2] = NDArrayIndex.point(sentenceLength);
                    indices[3] = NDArrayIndex.all();
                } else {
                    indices[2] = NDArrayIndex.all();
                    indices[3] = NDArrayIndex.point(sentenceLength);
                }
                features.put(indices, vector);
            }
        }
        return features;
    }

    public int totalExamples() {
        return this.documentProvider.totalNumSentences();
    }

    public int inputColumns() {
        return this.wordVectorSize;
    }

    public boolean resetSupported() {
        return true;
    }

    public boolean asyncSupported() {
        return true;
    }

    public void reset() {
        this.cursor = 0;
        this.documentProvider.reset();
    }

    public int batch() {
        return this.minibatchSize;
    }

    public int cursor() {
        return this.cursor;
    }

    public int numExamples() {
        return this.totalExamples();
    }

    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        this.dataSetPreProcessor = preProcessor;
    }

    public DataSetPreProcessor getPreProcessor() {
        return this.dataSetPreProcessor;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported");
    }

    public int totalOutcomes() {
        return this.labelSize;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @ConstructorProperties({"documentProvider", "wordVectors", "tokenizerFactory", "unknownWordHandling", "useNormalizedWordVectors", "minibatchSize", "maxSentenceLength", "sentencesAlongHeight", "dataSetPreProcessor", "wordVectorSize", "unknown", "cursor", "labelSize"})
    public ConvMFDocumentDataSetIterator(ConvMFDocumentProvider documentProvider, WordVectors wordVectors, TokenizerFactory tokenizerFactory, ConvMFDocumentDataSetIterator.UnknownWordHandling unknownWordHandling, boolean useNormalizedWordVectors, int minibatchSize, int maxSentenceLength, boolean sentencesAlongHeight, DataSetPreProcessor dataSetPreProcessor, int wordVectorSize, INDArray unknown, int cursor, int labelSize) {
        this.documentProvider = null;
        this.cursor = 0;
        this.documentProvider = documentProvider;
        this.wordVectors = wordVectors;
        this.tokenizerFactory = tokenizerFactory;
        this.unknownWordHandling = unknownWordHandling;
        this.useNormalizedWordVectors = useNormalizedWordVectors;
        this.minibatchSize = minibatchSize;
        this.maxSentenceLength = maxSentenceLength;
        this.sentencesAlongHeight = sentencesAlongHeight;
        this.dataSetPreProcessor = dataSetPreProcessor;
        this.wordVectorSize = wordVectorSize;
        this.unknown = unknown;
        this.cursor = cursor;
        this.labelSize = labelSize;
    }

    public static class Builder {
        private ConvMFDocumentProvider documentProvider = null;
        private WordVectors wordVectors;
        private TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
        private ConvMFDocumentDataSetIterator.UnknownWordHandling unknownWordHandling;
        private boolean useNormalizedWordVectors;
        private int maxSentenceLength;
        private int minibatchSize;
        private boolean sentencesAlongHeight;
        private DataSetPreProcessor dataSetPreProcessor;

        public Builder() {
            this.unknownWordHandling = ConvMFDocumentDataSetIterator.UnknownWordHandling.RemoveWord;
            this.useNormalizedWordVectors = true;
            this.maxSentenceLength = -1;
            this.minibatchSize = 32;
            this.sentencesAlongHeight = true;
        }

        public ConvMFDocumentDataSetIterator.Builder documentProvider(ConvMFDocumentProvider documentProvider) {
            this.documentProvider = documentProvider;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder wordVectors(WordVectors wordVectors) {
            this.wordVectors = wordVectors;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder tokenizerFactory(TokenizerFactory tokenizerFactory) {
            this.tokenizerFactory = tokenizerFactory;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder unknownWordHandling(ConvMFDocumentDataSetIterator.UnknownWordHandling unknownWordHandling) {
            this.unknownWordHandling = unknownWordHandling;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder minibatchSize(int minibatchSize) {
            this.minibatchSize = minibatchSize;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder useNormalizedWordVectors(boolean useNormalizedWordVectors) {
            this.useNormalizedWordVectors = useNormalizedWordVectors;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder maxSentenceLength(int maxSentenceLength) {
            this.maxSentenceLength = maxSentenceLength;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder sentencesAlongHeight(boolean sentencesAlongHeight) {
            this.sentencesAlongHeight = sentencesAlongHeight;
            return this;
        }

        public ConvMFDocumentDataSetIterator.Builder dataSetPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
            this.dataSetPreProcessor = dataSetPreProcessor;
            return this;
        }

        public ConvMFDocumentDataSetIterator build() {
            if(this.wordVectors == null) {
                throw new IllegalStateException("Cannot build ConvMFDocumentDataSetIterator without a WordVectors instance");
            } else {
                return new ConvMFDocumentDataSetIterator(this);
            }
        }
    }

    public enum UnknownWordHandling {
        RemoveWord,
        UseUnknownVector;

        UnknownWordHandling() {
        }
    }
}
