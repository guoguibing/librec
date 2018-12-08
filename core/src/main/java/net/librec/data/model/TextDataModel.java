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

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.math.structure.DataSet;

import java.io.IOException;

/**
 * A <tt>TextDataModel</tt> represents a data access class to the CSV format
 * input.
 *
 * @author WangYuFeng
 */
public class TextDataModel extends AbstractDataModel {

    /**
     * Empty constructor.
     */
    public TextDataModel() {
    }

    /**
     * Initializes a newly created {@code TextDataModel} object with
     * configuration.
     *
     * @param conf the configuration for the model.
     */
    public TextDataModel(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Build Convert.
     *
     * @throws LibrecException if error occurs during building
     */
    @Override
    public void buildConvert() throws LibrecException {
//        String inputDataPath = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + conf.get(Configured.CONF_DATA_INPUT_PATH);
        String[] inputDataPath = conf.get(Configured.CONF_DATA_INPUT_PATH).trim().split(":");
        for (int i = 0; i < inputDataPath.length; i++) {
            inputDataPath[i] = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + inputDataPath[i];
        }
        String dataColumnFormat = conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
        dataConvertor = new TextDataConvertor(dataColumnFormat, inputDataPath, conf.get("data.convert.sep","[\t;, ]"));
        try {
            dataConvertor.processData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load data model.
     *
     * @throws LibrecException if error occurs during loading
     */
    @Override
    public void loadDataModel() throws LibrecException {

    }

    /**
     * Save data model.
     *
     * @throws LibrecException if error occurs during saving
     */
    @Override
    public void saveDataModel() throws LibrecException {

    }

    /**
     * Get datetime data set.
     *
     * @return the datetime data set of data model.
     */
    @Override
    public DataSet getDatetimeDataSet() {
        return dataConvertor.getDatetimeMatrix();
    }
}
