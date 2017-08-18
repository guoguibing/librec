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

import net.librec.BaseTestCase;
import net.librec.conf.Configured;
import net.librec.math.structure.SparseMatrix;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Text Data Convertor TestCase
 *
 * @author WangYuFeng
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TextDataConvertorTestCase extends BaseTestCase {

	private TextDataConvertor textDataConvertor;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		conf.set(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
	}

	/**
	 * Test the method processData with format UIR
	 *
	 * @throws Exception
	 */
	@Test
	public void test01ColumnFormatUIR() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4.txt");

		textDataConvertor = new TextDataConvertor(conf.get("inputDataPath"));
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
	public void test02ColumnFormatUIRT() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/matrix4by4-date.txt");
		conf.set("data.column.format", "UIRT");

		textDataConvertor = new TextDataConvertor(conf.get("data.column.format"), conf.get("inputDataPath"), -1.0);
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
	public void test03SubDir() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/test-convert-dir");

		textDataConvertor = new TextDataConvertor(conf.get("inputDataPath"));
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
	public void test04CSV() throws Exception {
		conf.set("inputDataPath", conf.get("dfs.data.dir") + "/test/datamodeltest/testCSV.txt");

		textDataConvertor = new TextDataConvertor(conf.get("inputDataPath"));
		textDataConvertor.processData();

		SparseMatrix preference = textDataConvertor.getPreferenceMatrix();
		SparseMatrix datetimeMatrix = textDataConvertor.getDatetimeMatrix();

		assertEquals(preference.size(), 13);
		assertNull(datetimeMatrix);
	}

}
