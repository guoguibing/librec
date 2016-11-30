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
 * KCVDataSplitter TestCase
 * {@link net.librec.data.splitter.KCVDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class KCVDataSplitterTestCase extends BaseTestCase{

	private TextDataConvertor convertor;
	private TextDataConvertor convertorWithDate;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		convertor = new TextDataConvertor("UIR","../data/Test/sytTest4by4A.txt");
		convertorWithDate = new TextDataConvertor("UIRT","../data/Test/sytTestDateA.txt");
		conf.set("data.splitter.cv.number", "6");
	}

	@Test
	public void test() throws IOException, LibrecException {
		String inputDataPath = "/home/liuxz/librec/librecBase/data/MoviesLens";
		TextDataConvertor dataConvertor = new TextDataConvertor("UIRT",inputDataPath);
		dataConvertor.processData();
		
		conf.set("data.splitter.cv.numer","5");
		conf.set("data.splitter.cv.index", "1");
		
		KCVDataSplitter kcvDataSplitter = new KCVDataSplitter(dataConvertor,conf);
		kcvDataSplitter.splitData();
		
		for (int i = 1; i <= 5 ; i ++){
			kcvDataSplitter.splitData(i);
		}
	}

	/**
	 * Test method splitData with dateMatrix
	 *
	 * @throws Exception
     */
	@Test
	public void testKCVWithoutDate() throws Exception{
		convertor.processData();
		KCVDataSplitter splitter = new KCVDataSplitter(convertor, conf);

		for (int i=1; i<=6; i++){
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
	public void testKCVWithDate() throws Exception{
		convertorWithDate.processData();
		KCVDataSplitter splitter = new KCVDataSplitter(convertorWithDate, conf);

		for (int i=1; i<=6; i++){
			splitter.splitData(i);
			assertEquals(splitter.getTrainData().size(), 10);
			assertEquals(splitter.getTestData().size(), 2);
		}
	}

}
