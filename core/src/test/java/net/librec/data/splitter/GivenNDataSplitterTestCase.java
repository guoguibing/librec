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

import static org.junit.Assert.assertEquals;

/**
 * GivenNDataSplitter TestCase
 * {@link net.librec.data.splitter.GivenNDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GivenNDataSplitterTestCase extends BaseTestCase{

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4.txt");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		convertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"), -1.0);

		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4-date.txt");
		convertorWithDate = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"), -1.0);
	}

	/**
	 * Test the methods splitData and getGivenNByUser
	 * givennbyuser: Each user splits out N ratings for test set,the rest for training set.
	 *
	 * @throws Exception
     */
	@Test
	public void test01GivenNByUser() throws Exception {
		conf.set("data.splitter.givenn", "user");
		conf.set("data.splitter.givenn.n", "1");
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
	public void test02GivenNByItem() throws Exception {
		conf.set("data.splitter.givenn", "item");
		conf.set("data.splitter.givenn.n", "1");
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
	public void test03GivenNByUserDate() throws Exception {
		conf.set("data.splitter.givenn", "userdate");
		conf.set("data.splitter.givenn.n", "1");
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
	public void test04GivenNByItemDate() throws Exception{
		conf.set("data.splitter.givenn", "itemdate");
		conf.set("data.splitter.givenn.n", "1");
		convertorWithDate.processData();

		GivenNDataSplitter splitter = new GivenNDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 4);
		assertEquals(splitter.getTestData().size(), 9);
	}
}
