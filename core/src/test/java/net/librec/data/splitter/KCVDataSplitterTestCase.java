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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * KCVDataSplitter TestCase {@link net.librec.data.splitter.KCVDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class KCVDataSplitterTestCase extends BaseTestCase {

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4A.txt");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		convertor = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT), conf.get("inputDataPath"));

		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIRT");
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4A-date.txt");
		convertorWithDate = new TextDataConvertor(conf.get(Configured.CONF_DATA_COLUMN_FORMAT),
				conf.get("inputDataPath"));
		conf.set("data.splitter.cv.number", "6");
	}

	/**
	 * Test method splitData with dateMatrix
	 *
	 * @throws Exception
	 */
	@Test
	public void testKCVWithoutDate() throws Exception {
		convertor.processData();
		KCVDataSplitter splitter = new KCVDataSplitter(convertor, conf);

		for (int i = 1; i <= 6; i++) {
			splitter.splitFolds();
			splitter.splitData(i);
			assertEquals(splitter.getTrainData().size(), 10);
			assertEquals(splitter.getTestData().size(), 2);
		}
	}

	/**
	 * Test method splitData without dateMatrix
	 *
	 * @throws Exception
	 */
	@Test
	public void testKCVWithDate() throws Exception {
		convertorWithDate.processData();
		KCVDataSplitter splitter = new KCVDataSplitter(convertorWithDate, conf);

		for (int i = 1; i <= 6; i++) {
			splitter.splitFolds();
			splitter.splitData(i);
			assertEquals(splitter.getTrainData().size(), 10);
			assertEquals(splitter.getTestData().size(), 2);
		}
	}
}
