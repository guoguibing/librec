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

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.data.convertor.TextDataConvertor;

/**
 * RatioDataSplitter TestCase
 * {@link net.librec.data.splitter.RatioDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class RatioDataSplitterTestCase extends BaseTestCase{

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		conf.set("data.splitter.ratio", "0.8");
		convertor = new TextDataConvertor("UIR","../data/Test/ratings.txt");
		convertorWithDate = new TextDataConvertor("UIRT","../data/Test/ratingsDate.txt");
	}

	@Test
	public void test() throws IOException, LibrecException {
		String inputDataPath = "/home/liuxz/librec/librecTemp/librec/data/filmtrust/";
		TextDataConvertor dataConvertor = new TextDataConvertor("UIRT",inputDataPath);
		dataConvertor.processData();
		
		conf.set("data.model.splitter", "ratingratio");
		conf.set("data.splitter.ratio", "0.8");
		RatioDataSplitter ratioDataSplitter = new RatioDataSplitter(dataConvertor, conf);
		ratioDataSplitter.splitData();
		
		conf.set("ratio.data.splitter", "userratio");
		ratioDataSplitter = new RatioDataSplitter(dataConvertor, conf);
		ratioDataSplitter.splitData();
		
		conf.set("ratio.data.splitter", "itemratio");
		ratioDataSplitter = new RatioDataSplitter(dataConvertor, conf);
		ratioDataSplitter.splitData();
		
		conf.set("ratio.data.splitter", "validratio");
		conf.set("data.splitter.train", "0.7");
		conf.set("data.splitter.train", "0.1");
		ratioDataSplitter = new RatioDataSplitter(dataConvertor, conf);
		ratioDataSplitter.splitData();
		
	}

	/**
	 * Test the methods splitData and getRatioByRating
	 *
	 * @throws Exception
     */
	@Test
	public void testRatingRatio() throws Exception{
		conf.set("ratio.data.splitter", "ratingratio");
		conf.set("data.splitter.ratio", "0.8");

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
	public void testUserRatio() throws Exception{
		conf.set("ratio.data.splitter", "userratio");
		conf.set("data.splitter.ratio", "0.8");

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
	public void testItemRatio() throws Exception{
		conf.set("ratio.data.splitter", "itemratio");
		conf.set("data.splitter.ratio", "0.8");

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
	public void testValidRatio() throws Exception{
		conf.set("ratio.data.splitter", "validratio");
		conf.set("data.splitter.train", "0.5");
		conf.set("data.splitter.valid", "0.3");

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
	public void testRatingDateRatio() throws Exception{
		conf.set("ratio.data.splitter", "ratingdateratio");
		conf.set("data.splitter.ratio", "0.8");

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
	public void testUserDateRatio() throws Exception{
		conf.set("ratio.data.splitter", "userdateratio");
		conf.set("data.splitter.ratio", "0.8");

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
	public void testItemDateRatio() throws Exception{
		conf.set("ratio.data.splitter", "itemdateratio");
		conf.set("data.splitter.ratio", "0.8");

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
