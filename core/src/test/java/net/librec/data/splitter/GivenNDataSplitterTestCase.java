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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.data.convertor.TextDataConvertor;

/**
 * GivenNDataSplitter TestCase
 * {@link net.librec.data.splitter.GivenNDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class GivenNDataSplitterTestCase extends BaseTestCase{

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		convertor = new TextDataConvertor("UIR","../data/Test/sytTest4by4.txt");
		convertorWithDate = new TextDataConvertor("UIRT","../data/Test/sytTestDate.txt");
	}

	@Test
	public void test() throws IOException, LibrecException {
		String inputDataPath = "/home/liuxz/librec/librecBase/data/MovieLens";
		TextDataConvertor dataConvertor = new TextDataConvertor("UIRT",inputDataPath);
		dataConvertor.processData();
		conf.set("data.splitter.given.n", "2");

		conf.set("data.model.splitter", "getgivennbyuser");
		GivenNDataSplitter givenNDataSplitter = new GivenNDataSplitter(dataConvertor, conf);
		givenNDataSplitter.splitData();

		conf.set("data.model.splitter","getgivennbyitem");
		givenNDataSplitter = new GivenNDataSplitter(dataConvertor, conf);
		givenNDataSplitter.splitData();
	}

	/**
	 * Test the methods splitData and getGivenNByUser
	 * givennbyuser: Each user splits out N ratings for test set,the rest for training set.
	 *
	 * @throws Exception
     */
	@Test
	public void testGivenNByUser() throws Exception{
		conf.set("givenn.data.splitter", "getgivennbyuser");
		conf.set("data.splitter.given.n", "1");
		convertor.processData();

		GivenNDataSplitter splitter = new GivenNDataSplitter(convertor, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 4);
		assertEquals(splitter.getTestData().size(), 9);
	}

	/**
	 * Test the methods splitData and getGivenNByItem
	 * givennbyitem: Each user splits out N ratings for test set,the rest for training set.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGivenNByItem() throws Exception{
		conf.set("givenn.data.splitter", "getgivennbyitem");
		conf.set("data.splitter.given.n", "1");
		convertor.processData();

		GivenNDataSplitter splitter = new GivenNDataSplitter(convertor, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 4);
		assertEquals(splitter.getTestData().size(), 9);
	}

	/**
	 * Test the methods splitData and getGivenNByUserDate
	 * givennbyuserdate: Each user splits out N ratings for test set,the rest for training set.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGivenNByUserDate() throws Exception{
		conf.set("givenn.data.splitter", "getgivennbyuserdate");
		conf.set("data.splitter.given.n", "1");
		convertorWithDate.processData();

		GivenNDataSplitter splitter = new GivenNDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 4);
		assertEquals(splitter.getTestData().size(), 9);
	}

	/**
	 * Test the methods splitData and getGivenNByItemDate
	 * givennbyitemdate: Each item splits out N ratings for test set,the rest for training set.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGivenNByItemDate() throws Exception{
		conf.set("givenn.data.splitter", "getgivennbyitemdate");
		conf.set("data.splitter.given.n", "1");
		convertorWithDate.processData();

		GivenNDataSplitter splitter = new GivenNDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 4);
		assertEquals(splitter.getTestData().size(), 9);
	}
}
