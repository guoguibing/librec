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
package net.librec.data.model;

import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataModel;
import net.librec.data.DataSplitter;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SparseMatrix;
import net.librec.math.structure.SparseTensor;
import net.librec.util.DriverClassUtil;
import net.librec.util.ReflectionUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A <tt>ArffDataModel</tt> represents a data access class
 * to the ARFF format input.
 *
 * @author WangYuFeng TangJiaxi and Ma Chen
 */
public class ArffDataModel extends AbstractDataModel implements DataModel {

    /** The convertor of the model */
    // private ArffDataConvertor dataConvertor;

    /**
     * Empty constructor.
     */
    public ArffDataModel() {
    }

    /**
     * Initializes a newly created {@code ArffDataModel} object
     * with configuration.
     *
     * @param conf
     *          the configuration for the model.
     */
    public ArffDataModel(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Build model.
     *
     * @throws LibrecException if error occurs during building
     */
    @Override
    public void buildConvert() throws LibrecException {
        String splitter = conf.get("data.model.splitter");
        String dfsDataDir = conf.get(Configured.CONF_DFS_DATA_DIR);
        String inputDataPath = dfsDataDir + "/" + conf.get(Configured.CONF_DATA_INPUT_PATH);
        dataConvertor = new ArffDataConvertor(inputDataPath);
        try {
            dataConvertor.processData();
            dataSplitter = (DataSplitter) ReflectionUtil.newInstance(DriverClassUtil.getClass(splitter), conf);
        } catch (IOException e) {
            throw new LibrecException(e);
        } catch (ClassNotFoundException e) {
            throw new LibrecException(e);
        }
    }

    /**
     * Build Splitter.
     *
     * @throws LibrecException
     *             if error occurs when building splitter.
     */
    @Override
    protected void buildSplitter() throws LibrecException {
        super.buildSplitter();
        if (dataConvertor != null && dataSplitter != null) {
            SparseTensor totalTensor = dataConvertor.getSparseTensor();
            // SparseMatrix trainMatrix = dataSplitter.getTrainData();
            SparseMatrix testMatrix = dataSplitter.getTestData();
            // construct train/test tensor from test sparse matrix
            SparseTensor trainTensor = totalTensor.clone();
            int[] dimensions = trainTensor.dimensions();
            SparseTensor testTensor = new SparseTensor(dimensions);
            testTensor.setUserDimension(trainTensor.getUserDimension());
            testTensor.setItemDimension(trainTensor.getItemDimension());

            for (MatrixEntry me : testMatrix) {
                int u = me.row();
                int i = me.column();

                List<Integer> indices = totalTensor.getIndices(u, i);

                for (int index : indices) {
                    int[] keys = totalTensor.keys(index);
                    try {
                        testTensor.set(totalTensor.value(index), keys);
                        trainTensor.remove(keys);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            trainDataSet = trainTensor;
            testDataSet = testTensor;
        }
    }
    /**
     * Get user mapping data.
     *
     * @return  the user {raw id, inner id} map of data model.
     */
    @Override
    public BiMap<String, Integer> getUserMappingData() {
        return ((ArffDataConvertor)dataConvertor).getUserIds();
    }

    /**
     * Get item mapping data.
     *
     * @return  the item {raw id, inner id} map of data model.
     */
    @Override
    public BiMap<String, Integer> getItemMappingData() {
        return ((ArffDataConvertor)dataConvertor).getItemIds();
    }

    @Override
    public DataSet getDatetimeDataSet() {
        return null;
    }

    /**
     * Get all features mapping data.
     *
     * @return  the item {raw id, inner id} map of data model.
     */
    public ArrayList<BiMap<String, Integer>> getAllFeaturesMappingData() {
        return ((ArffDataConvertor)dataConvertor).getAllFeatureIds();
    }
}
