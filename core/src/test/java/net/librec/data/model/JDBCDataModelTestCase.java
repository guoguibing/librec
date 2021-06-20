package net.librec.data.model;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configured;
import net.librec.data.DataModel;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JDBCDataModel TestCase {@link net.librec.data.model.JDBCDataModel}
 *
 * @author jiajingzhe
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JDBCDataModelTestCase extends BaseTestCase {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        conf.set("data.convert.jbdc.driverName","com.mysql.jdbc.Driver");
        conf.set("data.convert.jbdc.URL","jdbc:mysql://<ip>:<port>/librec_test?useSSL=false");
        conf.set("data.convert.jbdc.user","root");
        conf.set("data.convert.jbdc.password","password");
        conf.set("data.convert.jbdc.tableName","test");
        conf.set("data.convert.jbdc.userColName","usercol");
        conf.set("data.convert.jbdc.itemColName","itemcol");
        conf.set("data.convert.jbdc.ratingColName","ratingcol");

        conf.set("data.model.splitter", "ratio");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("data.splitter.ratio", "rating");

    }
    /**
     * test the function of convertor part
     * {@link net.librec.data.convertor.JDBCDataConvertor} input data subject to
     * format UIR: userId itemId rating
     *
     * @throws LibrecException
     */
    @Test
    public void test01ColumnFormatUIR() throws LibrecException {
        conf.set("data.column.format","UIR");
        JDBCDataModel dataModel = new JDBCDataModel(conf);
        dataModel.buildDataModel();
        assertEquals(getDataSize(dataModel), 5);
    }
    @Test
    public void test02ColumnFormatUIRT() throws LibrecException {
        conf.set("data.column.format","UIRT");
        conf.set("data.convert.jbdc.datetimeColName","datacol");
        JDBCDataModel dataModel = new JDBCDataModel(conf);
        dataModel.buildDataModel();
        assertEquals(getDataSize(dataModel), 5);
    }
    @Test
    public void test03HiveUIRT() throws LibrecException {
        conf.set("data.convert.jbdc.driverName","org.apache.hive.jdbc.HiveDriver");
        conf.set("data.convert.jbdc.URL","jdbc:hive2://<ip>:<port>/default");
        conf.set("data.convert.jbdc.user","root");
        conf.set("data.convert.jbdc.password","password");
        conf.set("data.convert.jbdc.tableName","librec_jdbc_test");
        conf.set("data.convert.jbdc.userColName","usercol");
        conf.set("data.convert.jbdc.itemColName","itemcol");
        conf.set("data.convert.jbdc.ratingColName","ratingcol");
        conf.set("data.column.format","UIRT");
        conf.set("data.convert.jbdc.datetimeColName","datacol");
        JDBCDataModel dataModel = new JDBCDataModel(conf);
        dataModel.buildDataModel();
        assertEquals(getDataSize(dataModel), 5);
    }
    /**
     * Test the function of splitter part.
     * {@link net.librec.data.splitter.RatioDataSplitter} Sort all ratings by
     * date,and split the data by rating ratio.
     *
     * @throws LibrecException
     */
    @Test
    public void test04RatingDateRatio() throws LibrecException {
        conf.set("data.model.splitter", "ratio");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("data.splitter.ratio", "ratingdate");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        conf.set("data.convert.jbdc.datetimeColName","datacol");

        JDBCDataModel dataModel = new JDBCDataModel(conf);
        dataModel.buildDataModel();

        double actualRatio = getTrainRatio(dataModel);
        assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
    }

    /**
     * Test the function of splitter part.
     * {@link net.librec.data.splitter.RatioDataSplitter} Sort each user's
     * ratings by date, and split by user ratio.
     *
     * @throws LibrecException
     */
    @Test
    public void test05UserDateRatio() throws LibrecException {
        conf.set("data.model.splitter", "ratio");
        conf.set("data.splitter.trainset.ratio", "0.8");
        conf.set("data.splitter.ratio", "userdate");
        conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
        conf.set("data.convert.jbdc.datetimeColName","datacol");

        TextDataModel dataModel = new TextDataModel(conf);
        dataModel.buildDataModel();

        double actualRatio = getTrainRatio(dataModel);
        assertTrue(Math.abs(actualRatio - 0.8) <= 0.02);
    }

    /**
     * Returns the size of preference matrix of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the size of preference matrix of a specified DataModel object
     */
    public int getDataSize(DataModel dataModel) {
        int sum = 0;
        int train = getTrainSize(dataModel);
        int test = getTestSize(dataModel);
        if (null != dataModel.getDataSplitter().getValidData()) {
            int valid = getValidSize(dataModel);
            sum += valid;
        }
        sum = sum + train + test;
        return sum;
    }

    /**
     * Returns the size of training matrix of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the size of training matrix of a specified DataModel object
     */
    public int getTrainSize(DataModel dataModel) {
        return dataModel.getDataSplitter().getTrainData().size();
    }

    /**
     * Returns the size of test matrix of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the size of test matrix of a specified DataModel object
     */
    public int getTestSize(DataModel dataModel) {
        return dataModel.getDataSplitter().getTestData().size();
    }

    /**
     * Returns the size of validation matrix of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the size of validation matrix of a specified DataModel object
     */
    public int getValidSize(DataModel dataModel) {
        return dataModel.getDataSplitter().getValidData().size();
    }

    /**
     * calculate the ratio of training set of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the ratio of training set of a specified DataModel object
     */
    public double getTrainRatio(DataModel dataModel) {
        double trainSize = getTrainSize(dataModel);
        double totalSize = getDataSize(dataModel);

        return trainSize / totalSize;
    }

    /**
     * calculate the ratio of validation set of a specified DataModel object
     *
     * @param dataModel
     *            a DataModel object
     * @return the ratio of validation set of a specified DataModel object
     */
    public double getValidRatio(DataModel dataModel) {
        double validSize = getValidSize(dataModel);
        double totalSize = getDataSize(dataModel);

        return validSize / totalSize;
    }
}
