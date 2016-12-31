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

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.librec.common.LibrecException;
import net.librec.conf.Configured;
import net.librec.data.DataContext;
import net.librec.data.DataConvertor;
import net.librec.data.DataFeature;
import net.librec.data.DataModel;
import net.librec.data.DataSplitter;
import net.librec.data.splitter.KCVDataSplitter;
import net.librec.math.structure.DataSet;
import net.librec.util.DriverClassUtil;
import net.librec.util.ReflectionUtil;

/**
 * A <tt>AbstractDataModel</tt> represents a data access class to the input
 * file.
 *
 * @author WangYuFeng
 */
public abstract class AbstractDataModel extends Configured implements DataModel {
    /**
     * LOG
     */
    protected final Log LOG = LogFactory.getLog(this.getClass());
    /**
     * context
     */
    protected DataContext context;
    /**
     * train DataSet
     */
    protected DataSet trainDataSet;
    /**
     * test DataSet
     */
    protected DataSet testDataSet;
    /**
     * valid DataSet
     */
    protected DataSet validDataSet;

    /** The convertor of the model {@link net.librec.data.DataConvertor} */
    protected DataConvertor dataConvertor;

    /**
     * Data Splitter {@link net.librec.data.DataSplitter}
     */
    public DataSplitter dataSplitter;
    /**
     * Data Splitter {@link net.librec.data.DataFeature}
     */
    public DataFeature dataFeature;

    /**
     * Build Convert.
     *
     * @throws LibrecException
     *             if error occurs when building convert.
     */
    protected abstract void buildConvert() throws LibrecException;

    /**
     * Build Splitter.
     *
     * @throws LibrecException
     *             if error occurs when building splitter.
     */
    protected void buildSplitter() throws LibrecException {
        String splitter = conf.get("data.model.splitter");
        try {
            dataSplitter = (DataSplitter) ReflectionUtil.newInstance(DriverClassUtil.getClass(splitter), conf);
            if (dataSplitter != null) {
                dataSplitter.setDataConvertor(dataConvertor);
                if (dataSplitter instanceof KCVDataSplitter) {
                    ((KCVDataSplitter) dataSplitter).splitFolds();
                }
                dataSplitter.splitData();
                trainDataSet = dataSplitter.getTrainData();
                testDataSet = dataSplitter.getTestData();
            }
        } catch (ClassNotFoundException e) {
            throw new LibrecException(e);
        }
    }

    /**
     * Build feature data.
     * 
     * @throws LibrecException
     *             if error occurs when building feature.
     */
    protected void buildFeature() throws LibrecException {
        String feature = conf.get("data.feature.format");
        if (StringUtils.isNotBlank(feature)) {
            try {
                dataFeature = (DataFeature) ReflectionUtil.newInstance(DriverClassUtil.getClass(feature), conf);
                dataFeature.setUserMappingData(getUserMappingData());
                dataFeature.setItemMappingData(getItemMappingData());
                dataFeature.processData();
            } catch (ClassNotFoundException e) {
                throw new LibrecException(e);
            } catch (IOException e) {
                throw new LibrecException(e);
            }
        }
    }

    /**
     * Build data model.
     *
     * @throws LibrecException
     *             if error occurs when building model.
     */
    @Override
    public void buildDataModel() throws LibrecException {
        context = new DataContext(conf);
        if (!conf.getBoolean("data.convert.read.ready")) {
            buildConvert();
            LOG.info("Transform data to Convertor successfully!");
            conf.setBoolean("data.convert.read.ready", true);
        }
        buildSplitter();
        LOG.info("Split data to train Set and test Set successfully!");
        if (trainDataSet != null && trainDataSet.size() > 0 && testDataSet != null && testDataSet.size() > 0) {
            LOG.info("Data size of training is " + trainDataSet.size());
            LOG.info("Data size of testing is " + testDataSet.size());
        }
        if (StringUtils.isNotBlank(conf.get("data.feature.format")) && !conf.getBoolean("data.feature.read.ready")) {
            buildFeature();
            LOG.info("Transform data to Feature successfully!");
            conf.setBoolean("data.feature.read.ready", true);
        }
    }

    /**
     * Load data model.
     *
     * @throws LibrecException
     *             if error occurs during loading
     */
    @Override
    public void loadDataModel() throws LibrecException {
        // TODO Auto-generated method stub

    }

    /**
     * Save data model.
     *
     * @throws LibrecException
     *             if error occurs during saving
     */
    @Override
    public void saveDataModel() throws LibrecException {
        // TODO Auto-generated method stub

    }

    /**
     * Get train data set.
     *
     * @return the train data set of data model.
     */
    @Override
    public DataSet getTrainDataSet() {
        return trainDataSet;
    }

    /**
     * Get test data set.
     *
     * @return the test data set of data model.
     */
    @Override
    public DataSet getTestDataSet() {
        return testDataSet;
    }

    /**
     * Get valid data set.
     *
     * @return the valid data set of data model.
     */
    @Override
    public DataSet getValidDataSet() {
        return validDataSet;
    }

    /**
     * Get data splitter.
     *
     * @return the splitter of data model.
     */
    @Override
    public DataSplitter getDataSplitter() {
        return dataSplitter;
    }

    /**
     * Get data feature.
     *
     * @return the feature of data model.
     */
    @Override
    public DataFeature getDataFeature() {
        return dataFeature;
    }

    /**
     * Get data context.
     *
     * @return the context see {@link net.librec.data.DataContext}.
     */
    @Override
    public DataContext getContext() {
        return context;
    }
}
