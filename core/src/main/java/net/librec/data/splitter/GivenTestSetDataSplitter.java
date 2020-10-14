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
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;

import java.io.IOException;
import java.sql.SQLException;

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
    private SequentialAccessSparseMatrix preferenceMatrix;

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

    @Override
    public void splitData() throws LibrecException{
        DataConvertor testConvertor = null;
        String dataFormat = conf.get("data.model.format");
        String[] inputDataPath = conf.get("data.testset.path").trim().split(":");
        for (int i = 0; i < inputDataPath.length; i++) {
            inputDataPath[i] = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + inputDataPath[i];
        }
        switch (dataFormat.toLowerCase()){
            case "text":

                String dataColumnFormat = conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
                testConvertor = new TextDataConvertor(dataColumnFormat,
                        inputDataPath,"[\t;, ]");
                break;
            case "arff":
                testConvertor = new ArffDataConvertor(inputDataPath);
                break;

            default:
                LOG.info("Not implement now or please check data.model.format");
        }

        try {
            testConvertor.processData();
        }catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        testMatrix = testConvertor.getPreferenceMatrix(conf);
        trainMatrix = dataConvertor.getPreferenceMatrix(conf);

        // remove test elements from trainMatrix
        for (MatrixEntry me : testMatrix) {
            int rowIdx = me.row();
            int colIdx = me.column();
            trainMatrix.set(rowIdx, colIdx, 0.0);
        }
        trainMatrix.reshape();

//        if (Objects.equals(conf.get("data.convert.columns"), null)){
//            testMatrix = testConvertor.getPreferenceMatrix();
//        }else{
//            testMatrix =testConvertor.getPreferenceMatrix(conf.get("data.convert.columns").split(","));
//        }
//       if (Objects.equals(conf.get("data.convert.columns"), null)) {
//            trainMatrix= dataConvertor.getPreferenceMatrix();
//        } else {
//            trainMatrix = dataConvertor.getPreferenceMatrix(conf.get("data.convert.columns").split(","));
//       }
    }
}
