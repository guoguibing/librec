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
package net.librec.data.splitter;

import net.librec.BaseTestCase;
import net.librec.conf.Configured;
import net.librec.data.DataConvertor;
import net.librec.data.convertor.ArffDataConvertor;
import net.librec.data.convertor.TextDataConvertor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * GivenTestSetDataSplitter TestCase
 * {@link net.librec.data.splitter.GivenNDataSplitter}
 *
 * @author LiuXiaoze
 */
public class GivenTestSetDataSplitterTestCase extends BaseTestCase {

	private DataConvertor convertor;

	@Before
	public void setUp() throws Exception {
		super.setUp();

	}

	/**
	 * Test for text data format.
	 *
	 * @throws Exception if error occurs.
     */
	@Test
	public void testText() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/given-testset");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		conf.set("data.testset.path", "/test/given-testset/test/ratings_0.txt");
		convertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"));
		convertor.processData();
		GivenTestSetDataSplitter splitter = new GivenTestSetDataSplitter(convertor,conf);
		splitter.splitData();
		assertEquals(splitter.getTrainData().size(), 35491-10435);
		assertEquals(splitter.getTestData().size(), 10435);
	}

	/**
	 * Test for arff data format.
	 *
	 * @throws Exception if error occurs
     */
	@Test
	public void testArff() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/arfftest");
		conf.set("data.testset.path", "/test/arfftest/testset/test.arff");
		conf.set("data.model.format", "arff");
		convertor = new ArffDataConvertor(conf.get("inputDataPath"));
		convertor.processData();
		GivenTestSetDataSplitter splitter = new GivenTestSetDataSplitter(convertor,conf);
		splitter.splitData();
		// assertEquals(splitter.getTrainData().size(), 12);
		assertEquals(splitter.getTestData().size(), 6);
	}
}
