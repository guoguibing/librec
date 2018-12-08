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

import lombok.NonNull;
import org.datavec.api.util.RandomUtils;
import org.nd4j.linalg.primitives.Pair;

import java.util.List;
import java.util.Random;

/**
 * DocumentProvider for ConvMFRecommender
 * Donghyun Kim et al., Convolutional Matrix Factorization for Document Context-aware Recommendation
 * {@code Proceedings of the 10th ACM Conference on Recommender Systems. ACM, 2016.}.
 *
 * @author SunYatong
 */
public class ConvMFDocumentProvider {
    private final List<String> documents;
    private final List<double[]> labels;
    private final Random rng;
    private final int[] order;
    private int cursor;
    private int labelSize;
    private int numDoc;

    public ConvMFDocumentProvider(@NonNull List<String> documents, @NonNull List<double[]> labelsForSentences) {
        this(documents, labelsForSentences, new Random());
        if(documents == null) {
            throw new NullPointerException("document");
        } else if(labelsForSentences == null) {
            throw new NullPointerException("labelsForDocuments");
        }
    }

    public ConvMFDocumentProvider(@NonNull List<String> documents, @NonNull List<double[]> labelsForDocuments, Random rng) {
        this.cursor = 0;
        if(documents == null) {
            throw new NullPointerException("documents");
        } else if(labelsForDocuments == null) {
            throw new NullPointerException("labelsForDocuments");
        } else if(documents.size() != labelsForDocuments.size()) {
            throw new IllegalArgumentException("Documents and labels must be same cardinality (documents cardinality: " + documents.size() + ", labels cardinality: " + labelsForDocuments.size() + ")");
        } else {
            this.documents = documents;
            this.labels = labelsForDocuments;
            this.labelSize = labelsForDocuments.get(0).length;
            this.rng = rng;
            this.numDoc = this.documents.size();
            if(rng == null) {
                this.order = null;
            } else {
                this.order = new int[documents.size()];

                for(int i = 0; i < documents.size(); this.order[i] = i++) {
                }

                RandomUtils.shuffleInPlace(this.order, rng);
            }

        }
    }

    public boolean hasNext() {
        return this.cursor < this.documents.size();
    }

    public Pair<String, double[]> nextSentence() {
        int idx;
        if(this.rng == null) {
            idx = this.cursor++;
        } else {
            idx = this.order[this.cursor++];
        }

        return new Pair<>(this.documents.get(idx), this.labels.get(idx));
    }

    public void reset() {
        this.cursor = 0;
        if(this.rng != null) {
            RandomUtils.shuffleInPlace(this.order, this.rng);
        }

    }

    public int totalNumSentences() {
        return this.documents.size();
    }

    public int getLabelSize() {
        return labelSize;
    }

    public int getNumDoc() {
        return numDoc;
    }

}
