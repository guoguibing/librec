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
import net.librec.conf.Configured;
import net.librec.data.DataConvertor;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseVector;
import net.librec.math.structure.TensorEntry;
import net.librec.math.structure.VectorEntry;

import java.io.IOException;

/**
 * Given Test Set Data Splitter<br>
 * Get test set from specified path<br>
 * Test set and train set should be in the same directory.
 *
 * @author liuxz and Ma Chen
 */
public class GivenTestSetDataSplitter extends AbstractDataSplitter {

    /**
     * The rate dataset for training
     */
    private SparseMatrix preferenceMatrix;

    /**
     * Empty constructor.
     */
    public GivenTestSetDataSplitter() {
    }

    /**
     * Initializes a newly created {@code GivenTestSetDataSplitter} object
     * with configuration.
     *
     * @param convertor data convertor
     * @param conf      the configuration for the splitter.
     */
    public GivenTestSetDataSplitter(DataConvertor convertor, Configuration conf) {
        this.dataConvertor = convertor;
        this.conf = conf;
    }

    /**
     * Split the data.
     *
     * @throws LibrecException if error occurs
     */
    @Override
    public void splitData() throws LibrecException {
        DataConvertor testConvertor = null;
        String dataFormat = conf.get("data.model.format");
        switch (dataFormat.toLowerCase()) {
            case "text":
                preferenceMatrix = dataConvertor.getPreferenceMatrix();
                trainMatrix = new SparseMatrix(preferenceMatrix);
                testMatrix = new SparseMatrix(preferenceMatrix);
                testConvertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR"),
                        conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + conf.get("data.testset.path"),
                        conf.getDouble("data.convert.binarize.threshold", -1.0),
                        ((TextDataConvertor) dataConvertor).getUserIds(),
                        ((TextDataConvertor) dataConvertor).getItemIds());
                try {
                    testConvertor.processData();
                } catch (IOException e) {
                    throw new LibrecException(e);
                }
                for (int u = 0, um = preferenceMatrix.numRows(); u < um; u++) {
                    SparseVector uv = preferenceMatrix.row(u);
                    for (VectorEntry j : uv) {
                        if (testConvertor.getPreferenceMatrix().get(u, j.index()) == 0) {
                            testMatrix.set(u, j.index(), 0.0);
                        } else {
                            trainMatrix.set(u, j.index(), 0.0);
                        }
                    }
                }
                SparseMatrix.reshape(trainMatrix);
                SparseMatrix.reshape(testMatrix);

                break;
            case "arff":
                testConvertor = new ArffDataConvertor(
                        conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + conf.get("data.testset.path"),
                        ((ArffDataConvertor) dataConvertor).getAllFeatureIds());
                try {
                    testConvertor.processData();
                } catch (IOException e) {
                    throw new LibrecException(e);
                }
                testMatrix = testConvertor.getSparseTensor().rateMatrix();

                break;
        }
    }
}
