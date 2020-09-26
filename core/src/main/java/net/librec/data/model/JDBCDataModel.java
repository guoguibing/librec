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
import net.librec.data.convertor.JDBCDataConvertor;
import net.librec.math.structure.DataSet;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A <tt>JDBCDataModel</tt> represents a data access class to the database format
 * input.
 *
 * @author Jiajingzhe
 */
public class JDBCDataModel extends AbstractDataModel {

    /**
     * Empty constructor.
     */
    public JDBCDataModel() {
    }
    /**
     * Initializes a newly created {@code JDBCDataModel} object with
     * configuration.
     *
     * @param conf the configuration for the model.
     */
    public JDBCDataModel(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Build Convert.
     *
     * @throws LibrecException if error occurs during building
     */
    @Override
    protected void buildConvert() throws LibrecException {
        String driverName = conf.get("data.convert.jbdc.driverName","com.mysql.jdbc.Driver"); // The default is mysql
        String URL = conf.get("data.convert.jbdc.URL");
        String user = conf.get("data.convert.jbdc.user");
        String password = conf.get("data.convert.jbdc.password");
        String tableName = conf.get("data.convert.jbdc.tableName");
        String userColName = conf.get("data.convert.jbdc.userColName");
        String itemColName = conf.get("data.convert.jbdc.itemColName");
        String ratingColName =conf.get("data.convert.jbdc.ratingColName");
        String datetimeColName = conf.get("data.convert.jbdc.datetimeColName"," ");

        dataConvertor = new JDBCDataConvertor(driverName,URL,user,password,tableName,userColName,itemColName,ratingColName,datetimeColName);
        try{
            dataConvertor.processData();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
    @Override
    public DataSet getDatetimeDataSet() {
        return dataConvertor.getDatetimeMatrix();
    }
}
