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
package net.librec.data.convertor;

import com.google.common.collect.BiMap;
import net.librec.math.structure.DataFrame;
import net.librec.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A <tt>JDBCDataConvertor</tt>is a class to convert
 * a data file from Database format to a target format.
 *
 * @author jiajingzhe
 */
public class JDBCDataConvertor extends AbstractDataConvertor {

    /**
     * Log
     */
    private static final Log LOG = LogFactory.getLog(JDBCDataConvertor.class);

    /**
     * The attributes of JDBCDataConvertor
     */
    private String driverName = ""; // The default is mysql
    private String URL = "";
    private String user = "";
    private String password = "";
    private String tableName = "";
    private String userColName = "";
    private String itemColName = "";
    private String ratingColName = "";
    private String datetimeColName = "";

    private Connection conn = null;
    private ResultSet result = null;
    private PreparedStatement pst = null;
    private String[] header;
    private String[] attr;
    private float fileRate;
    /**
     * the threshold to binarize a rating. If a rating is greater than the threshold, the value will be 1;
     * otherwise 0. To disable this appender, i.e., keep the original rating value, set the threshold a negative value
     */
    private double binThold = -1.0;

    /**
     * user/item {raw id, inner id} map
     */
    private BiMap<String, Integer> userIds, itemIds;

    /**
     * time unit may depend on data sets, e.g. in MovieLens, it is unix seconds
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;


    public JDBCDataConvertor(String driverName, String URL, String user, String password, String tableName, String userColName, String itemColName, String ratingColName, String datetimeColName) {
        this.driverName = driverName;
        this.URL = URL;
        this.user = user;
        this.password = password;
        this.tableName = tableName;
        this.userColName = userColName;
        this.itemColName = itemColName;
        this.ratingColName = ratingColName;
        this.datetimeColName = datetimeColName;

        if (this.isNotBlank()) {
            try {
                Class.forName(this.driverName);
                this.conn = DriverManager.getConnection(this.URL, this.user, this.password);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        else{
            LOG.error("Incomplete database connection configuration information!");
        }
    }

    /**
     * Check whether each required parameter is null, empty .
     * And fields other than datetimeColName cannot be spaces
     */
    public boolean isNotBlank() {
        return StringUtils.isNotBlank(this.driverName) &&
                StringUtils.isNotBlank(this.URL) &&
                StringUtils.isNotBlank(this.user) &&
                StringUtils.isNotBlank(this.password) &&
                StringUtils.isNotBlank(this.tableName) &&
                StringUtils.isNotBlank(this.userColName) &&
                StringUtils.isNotBlank(this.itemColName) &&
                StringUtils.isNotBlank(this.ratingColName) &&
                StringUtils.isNotEmpty(this.datetimeColName);
    }

    @Override
    public void processData() throws IOException, SQLException {
        selectData();
    }

    private void selectData() throws SQLException {
        LOG.info(String.format("Dataset: %s", this.URL + "/" + this.tableName));
        matrix = new DataFrame();
        if (Objects.isNull(header)) {
            if (StringUtils.isNotBlank(this.datetimeColName)) {
                header = new String[]{"user", "item", "rating", "datetime"};
                attr = new String[]{"STRING", "STRING", "NUMERIC", "DATE"};
            } else {
                header = new String[]{"user", "item", "rating"};
                attr = new String[]{"STRING", "STRING", "NUMERIC"};
            }
        }
        matrix.setAttrType(attr);
        matrix.setHeader(header);

        // Get the number of entries of all data
        int numEntries = -1;
        try {
            String SQL = String.format("SELECT count(1) AS numEntries FROM %s;", this.tableName);
            pst = this.conn.prepareStatement(SQL);
            ResultSet rs = pst.executeQuery();
            if (rs.next()){
                numEntries = rs.getInt("numEntries");

            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // Get Data
        int cur = 0;
        try {
            String SQL = "";
            if (StringUtils.isNotBlank(this.datetimeColName)) {
                SQL = String.format("SELECT %s,%s,%s,%s FROM %s", this.userColName, this.itemColName, this.ratingColName, this.datetimeColName, this.tableName);
            } else {
                SQL = String.format("SELECT %s,%s,%s FROM %s", this.userColName, this.itemColName, this.ratingColName, this.tableName);
            }
            PreparedStatement pst = this.conn.prepareStatement(SQL);

            result = pst.executeQuery();
            String[] paras = new String[header.length];
            while (result.next()) {
                paras[0] = result.getString(this.userColName);
                paras[1] = result.getString(this.itemColName);
                paras[2] = result.getString(this.ratingColName);
                if (header.length > 3) {
                    paras[3] = result.getString(this.datetimeColName);
                }
                for (int i = 0; i < header.length; i++) {
                    if (Objects.equals(attr[i], "STRING")) {
                        DataFrame.setId(paras[i], matrix.getHeader(i));
                    }
                }
                matrix.add(paras);

                cur++;
                fileRate = cur / numEntries;
            }
            LOG.info(String.format("DataSet: %s is finished", this.tableName));

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            // 关闭连接，释放资源
            if (this.result != null) {
                this.result.close();
            }
            if (this.pst != null) {
                this.pst.close();
            }
            if (this.conn != null) {
                this.conn.close();
            }
        }
        List<Double> ratingScale = matrix.getRatingScale();
        if (ratingScale != null) {
            LOG.info(String.format("rating Scale: %s", ratingScale.toString()));
        }
        LOG.info(String.format("user number: %d,\t item number is: %d", matrix.numUsers(), matrix.numItems()));
    }

    @Override
    public void progress() {
        getJobStatus().setProgress(fileRate);
    }
}
