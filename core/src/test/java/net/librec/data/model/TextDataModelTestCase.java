/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.data.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import net.librec.data.DataModel;
import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;

/**
 * TextDataMode TestCase
 * {@link net.librec.data.model.TextDataModel}
 *
 * @author Liuxz and Sunyt
 */
public class TextDataModelTestCase extends BaseTestCase{

	@Before
	public void setUp() throws Exception {
		super.setUp();
		conf = new Configuration();
		conf.set(Configured.CONF_DFS_DATA_DIR, "../data/Test");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "ratings.txt");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "ratingratio");
	}

	/**
	 * test the function of convertor part
	 * {@link net.librec.data.convertor.TextDataConvertor}
	 * input data subject to format UIR: userId itemId rating
	 *
	 * @throws LibrecException
     */
	@Test
	public void testColumnFormatUIR() throws LibrecException {
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTest4by4.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getDataSize(dataModel), 13);
	}

	/**
	 * test the function of convertor part
	 * {@link net.librec.data.convertor.TextDataConvertor}
	 * input data subject to format UIRT: userId itemId rating date
	 *
	 * @throws LibrecException
     */
	@Test
	public void testColumnFormatUIRT() throws LibrecException {
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getDataSize(dataModel), 13);
	}

	/**
	 * test the function of convertor part
	 * {@link net.librec.data.convertor.TextDataConvertor}
	 * read all files in the specified directory and its subdirectories
	 *
	 * @throws LibrecException
     */
	@Test
	public void testSubDir() throws LibrecException{
		conf.set(Configured.CONF_DATA_INPUT_PATH, "testSubDir");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getDataSize(dataModel), 26);
	}

	/**
	 * test the function of convertor part
	 * {@link net.librec.data.convertor.TextDataConvertor}
	 * test three different types of CSV : ","  " " and "\t"
	 * @throws LibrecException
     */
	@Test
	public void testCSV() throws LibrecException{
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestCSV.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getDataSize(dataModel), 13);
	}

	/**
	 * test the function of splitter part
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * split the data by rating ratio
	 * @throws LibrecException
     */
	@Test
	public void testRatingRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "ratingratio");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * test the function of splitter part
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * split the data by user ratio
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testUserRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "userratio");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * test the function of splitter part
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * split the data by item ratio
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testItemRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "itemratio");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * test the function of splitter part
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * split the data by rating ratio into 3 sets: train,test,valid.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testvalidRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("ratio.data.splitter", "validratio");
		conf.set("data.splitter.train", "0.5");
		conf.set("data.splitter.valid", "0.3");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualTrainRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualTrainRatio-0.5)<=0.01);
		double actualValidRatio = getValidRatio(dataModel);
		assertTrue(Math.abs(actualValidRatio-0.3)<=0.05);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * Sort all ratings by date,and split the data by rating ratio.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testRatingDateRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "ratingdateratio");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "ratingsDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * Sort each user's ratings by date, and split by user ratio.
	 *
	 * @throws LibrecException
     */
	@Test
	public void testUserDateRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "userdateratio");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "ratingsDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.02);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.RatioDataSplitter}
	 * Sort each item's ratings by date, and split by item ratio.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testItemDateRatio() throws LibrecException{
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "0.8");
		conf.set("ratio.data.splitter", "itemdateratio");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "ratingsDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		double actualRatio = getTrainRatio(dataModel);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.04);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.KCVDataSplitter}
	 * Split all ratings for k-fold cross validation.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testKCV() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.KCVDataSplitter");
		conf.set("data.splitter.cv.number", "6");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDateA.txt");

		for (int i=1; i<=6; i++){
			conf.set("data.splitter.cv.index", i+"");

			TextDataModel dataModel = new TextDataModel(conf);
			dataModel.buildDataModel();
			System.out.println("index: " + i);
			assertEquals(getTrainSize(dataModel), 10);
			assertEquals(getTestSize(dataModel), 2);
		}
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.LOOCVDataSplitter}
	 * Each user splits out a rating for test set,the rest for train set.
	 *
	 * @throws LibrecException
     */
	@Test
	public void testLOOByUser() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.LOOCVDataSplitter");
		conf.set("loocv.data.splitter", "loobyuser");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTest4by4.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 9);
		assertEquals(getTestSize(dataModel), 4);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.LOOCVDataSplitter}
	 * Each item splits out a rating for test set,the rest for train set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testLOOByItem() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.LOOCVDataSplitter");
		conf.set("loocv.data.splitter", "loobyitem");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTest4by4.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 9);
		assertEquals(getTestSize(dataModel), 4);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.LOOCVDataSplitter}
	 * Each user splits out a rating with biggest date value for test set,the rest for train set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testLOOByUserDate() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.LOOCVDataSplitter");
		conf.set("loocv.data.splitter", "loobyuserdate");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 9);
		assertEquals(getTestSize(dataModel), 4);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.LOOCVDataSplitter}
	 * Each item splits out a rating with biggest date value for test set,the rest for train set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testLOOByItemDate() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.LOOCVDataSplitter");
		conf.set("loocv.data.splitter", "loobyitemdate");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 9);
		assertEquals(getTestSize(dataModel), 4);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.GivenNDataSplitter}
	 * Each user splits out N ratings for test set,the rest for training set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testGivenNByUser() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.GivenNDataSplitter");
		conf.set("givenn.data.splitter", "getgivennbyuser");
		conf.set("data.splitter.given.n", "1");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTest4by4.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 4);
		assertEquals(getTestSize(dataModel), 9);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.GivenNDataSplitter}
	 * Each item splits out N ratings for test set,the rest for training set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testGivenNByItem() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.GivenNDataSplitter");
		conf.set("givenn.data.splitter", "getgivennbyitem");
		conf.set("data.splitter.given.n", "1");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTest4by4.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 4);
		assertEquals(getTestSize(dataModel), 9);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.GivenNDataSplitter}
	 * each user split out N ratings with biggest value of date for test set,the rest for training set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testGivenNByUserDate() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.GivenNDataSplitter");
		conf.set("givenn.data.splitter", "getgivennbyuserdate");
		conf.set("data.splitter.given.n", "1");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 4);
		assertEquals(getTestSize(dataModel), 9);
	}

	/**
	 * Test the function of splitter part.
	 * {@link net.librec.data.splitter.GivenNDataSplitter}
	 * Each item split out N ratings with biggest value of date for test set,the rest for training set.
	 *
	 * @throws LibrecException
	 */
	@Test
	public void testGivenNByItemDate() throws LibrecException{
		conf.set("data.model.splitter", "net.librec.data.splitter.GivenNDataSplitter");
		conf.set("givenn.data.splitter", "getgivennbyitemdate");
		conf.set("data.splitter.given.n", "1");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "sytTestDate.txt");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		assertEquals(getTrainSize(dataModel), 4);
		assertEquals(getTestSize(dataModel), 9);
	}

	/**
	 * Returns the size of preference matrix of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the size of preference matrix of a specified DataModel object
     */
	public int getDataSize(DataModel dataModel){
		int sum = 0;
		int train = getTrainSize(dataModel);
		int test = getTestSize(dataModel);
		if (null!=dataModel.getDataSplitter().getValidData()){
			int valid = getValidSize(dataModel);
			sum += valid;
		}
		sum = sum + train + test;
		return sum;
	}

	/**
	 * Returns the size of training matrix of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the size of training matrix of a specified DataModel object
	 */
	public int getTrainSize(DataModel dataModel){
		return dataModel.getDataSplitter().getTrainData().size();
	}

	/**
	 * Returns the size of test matrix of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the size of test matrix of a specified DataModel object
	 */
	public int getTestSize(DataModel dataModel){
		return dataModel.getDataSplitter().getTestData().size();
	}

	/**
	 * Returns the size of validation matrix of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the size of validation matrix of a specified DataModel object
	 */
	public int getValidSize(DataModel dataModel){
		return dataModel.getDataSplitter().getValidData().size();
	}

	/**
	 * calculate the ratio of training set of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the ratio of training set of a specified DataModel object
	 */
	public double getTrainRatio(DataModel dataModel){
		double trainSize = getTrainSize(dataModel);
		double totalSize = getDataSize(dataModel);

		return trainSize/totalSize;
	}

	/**
	 * calculate the ratio of validation set of a specified DataModel object
	 *
	 * @param dataModel a DataModel object
	 * @return the ratio of validation set of a specified DataModel object
	 */
	public double getValidRatio(DataModel dataModel){
		double validSize = getValidSize(dataModel);
		double totalSize = getDataSize(dataModel);

		return validSize/totalSize;
	}
}
