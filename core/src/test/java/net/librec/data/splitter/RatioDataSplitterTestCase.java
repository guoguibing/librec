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
package net.librec.data.splitter;

import net.librec.BaseTestCase;
import net.librec.conf.Configured;
import net.librec.data.convertor.TextDataConvertor;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertTrue;

/**
 * RatioDataSplitter TestCase
 * {@link net.librec.data.splitter.RatioDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RatioDataSplitterTestCase extends BaseTestCase{

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		conf.set("data.splitter.trainset.ratio", "0.8");

		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/ratings.txt");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		convertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"));

		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/ratings-date.txt");
		convertorWithDate = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"));
	}

	/**
	 * Test the methods splitData and getRatioByRating
	 *
	 * @throws Exception
     */
	@Test
	public void test01RatingRatio() throws Exception{
		conf.set("data.splitter.ratio", "rating");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertor.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertor);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * Test the methods splitData and getRatioByUser
	 *
	 * @throws Exception
	 */
	@Test
	public void test02UserRatio() throws Exception{
		conf.set("data.splitter.ratio", "user");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertor.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertor);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * Test the methods splitData and getRatioByItem
	 *
	 * @throws Exception
	 */
	@Test
	public void test03ItemRatio() throws Exception{
		conf.set("data.splitter.ratio", "item");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertor.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertor);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * Test the methods splitData and getRatio
	 *
	 * @throws Exception
	 */
	@Test
	public void test04ValidRatio() throws Exception{
		conf.set("data.splitter.ratio", "valid");
		conf.set("data.splitter.trainset.ratio", "0.5");
		conf.set("data.splitter.validset.ratio", "0.3");

		convertor.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertor, conf);
		splitter.splitData();

		double actualTrainRatio = calTrainRatio(splitter, convertor);
		assertTrue(Math.abs(actualTrainRatio - 0.5) <= 0.01);

		double actualValidRatio = calValidRatio(splitter, convertor);
		assertTrue(Math.abs(actualValidRatio - 0.3) <= 0.01);
	}

	/**
	 * Test the methods splitData and getRatioByRatingDate
	 *
	 * @throws Exception
	 */
	@Test
	public void test05RatingDateRatio() throws Exception{
		conf.set("data.splitter.ratio", "ratingdate");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertorWithDate.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertorWithDate);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.01);
	}

	/**
	 * Test the methods splitData and getRatioByUserDate
	 *
	 * @throws Exception
	 */
	@Test
	public void test06UserDateRatio() throws Exception{
		conf.set("data.splitter.ratio", "userdate");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertorWithDate.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertorWithDate);
		assertTrue(Math.abs(actualRatio - 0.8) <= 0.02);
	}

	/**
	 * Test the methods splitData and getRatioByItemDate
	 *
	 * @throws Exception
	 */
	@Test
	public void test07ItemDateRatio() throws Exception{
		conf.set("data.splitter.ratio", "itemdate");
		conf.set("data.splitter.trainset.ratio", "0.8");

		convertorWithDate.processData();
		RatioDataSplitter splitter = new RatioDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		double actualRatio = calTrainRatio(splitter, convertorWithDate);
		assertTrue(Math.abs(actualRatio-0.8) <= 0.04);
	}

	/**
	 * calculate the ratio of training set of a specified RatioDataSplitter object and its convertor
	 *
	 * @param splitter
	 * @param convertor
     * @return the ratio of training set of a specified RatioDataSplitter object and its convertor
     */
	public double calTrainRatio(RatioDataSplitter splitter, TextDataConvertor convertor){
		double trainSize = splitter.getTrainData().size();
		double totalSize = convertor.getPreferenceMatrix().size();

		return trainSize/totalSize;
	}

	/**
	 * calculate the ratio of validation set of a specified RatioDataSplitter object and its convertor
	 *
	 * @param splitter
	 * @param convertor
	 * @return the ratio of validation set of a specified RatioDataSplitter object and its convertor
	 */
	public double calValidRatio(RatioDataSplitter splitter, TextDataConvertor convertor){
		double validSize = splitter.getValidData().size();
		double totalSize = convertor.getPreferenceMatrix().size();

		return validSize/totalSize;
	}

}
