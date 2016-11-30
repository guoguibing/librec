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
package net.librec.data.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import net.librec.BaseTestCase;
import net.librec.conf.Configured;
import net.librec.math.structure.SparseMatrix;

/**
 * Text Data Convertor TestCase
 *
 * @author WangYuFeng
 */
public class TextDataConvertorTestCase extends BaseTestCase {

	private TextDataConvertor textDataConvertor;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		conf.set("xx", "1");
		System.out.println(conf.get("xx"));

		conf.set(Configured.CONF_DFS_DATA_DIR, "../data/Test");
		conf.set(Configured.CONF_DATA_INPUT_PATH, "ratings.txt");
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
	}

	@Test
	public void testReadData() throws Exception {
		textDataConvertor = new TextDataConvertor("E:/workspace/hadoopworkspace/librec/data/filmtrust");
		Thread x = new Thread(textDataConvertor);
		x.start();
		// textDataConvertor.readData("UIR",
		// "E:/workspace/hadoopworkspace/librec/data/filmtrust");
		SparseMatrix preferenceMatrix = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

	}

	/**
	 * Test the method processData with format UIR
	 *
	 * @throws Exception
	 */
	@Test
	public void testColumnFormatUIR() throws Exception {
		textDataConvertor = new TextDataConvertor("../data/Test/sytTest4by4.txt");
		textDataConvertor.processData();

		SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

		assertEquals(preference.size(), 13);
		assertNull(datetimeMatrix);
	}

	/**
	 * Test the method processData with format UIRT
	 *
	 * @throws Exception
	 */
	@Test
	public void testColumnFormatUIRT() throws Exception {
		textDataConvertor = new TextDataConvertor("UIRT", "../data/Test/sytTestDate.txt");
		textDataConvertor.processData();

		SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

		assertEquals(preference.size(), 13);
		assertEquals(datetimeMatrix.size(), 13);
	}

	/**
	 * Test the method processData with reading files from directory and its
	 * sub-directories
	 *
	 * @throws Exception
	 */
	@Test
	public void testSubDir() throws Exception {
		textDataConvertor = new TextDataConvertor("../data/test/testSubDir");
		textDataConvertor.processData();

		SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

		assertEquals(preference.size(), 26);
		assertNull(datetimeMatrix);
	}

	/**
	 * Test the method processData with different types of CSV
	 *
	 * @throws Exception
	 */
	@Test
	public void testCSV() throws Exception {
		textDataConvertor = new TextDataConvertor("../data/test/sytTestCSV.txt");
		textDataConvertor.processData();

		SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

		assertEquals(preference.size(), 13);
		assertNull(datetimeMatrix);
	}
}
