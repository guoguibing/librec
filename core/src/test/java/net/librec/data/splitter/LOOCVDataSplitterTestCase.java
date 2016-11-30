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
 * LOOCVDataSplitter TestCase
 * {@link net.librec.data.splitter.LOOCVDataSplitter}
 *
 * @author Liuxz and Sunyt
 */
public class LOOCVDataSplitterTestCase extends BaseTestCase {

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
		
		conf.set("data.model.splitter", "loobyuser");
		LOOCVDataSplitter loocvDataSplitter = new LOOCVDataSplitter(dataConvertor,conf);
		loocvDataSplitter.splitData();
		
		conf.set("data.model.splitter", "loobyitems");
		loocvDataSplitter = new LOOCVDataSplitter(dataConvertor,  conf);
		loocvDataSplitter.splitData();
	}

	/**
	 * Test the methods splitData and getLOOByUser
	 *
	 * @throws Exception
     */
	@Test
	public void testLOOByUser() throws Exception{
		conf.set("loocv.data.splitter", "loobyuser");
		convertor.processData();

		LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertor, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 9);
		assertEquals(splitter.getTestData().size(), 4);
	}

	/**
	 * Test the methods splitData and getLOOByItem
	 *
	 * @throws Exception
	 */
	@Test
	public void testLOOByItem() throws Exception{
		conf.set("loocv.data.splitter", "loobyitem");
		convertor.processData();

		LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertor, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 9);
		assertEquals(splitter.getTestData().size(), 4);
	}

	/**
	 * Test the methods splitData and getLOOByUserDate
	 *
	 * @throws Exception
	 */
	@Test
	public void testLOOByUserDate() throws Exception{
		conf.set("loocv.data.splitter", "loobyuserdate");
		convertorWithDate.processData();

		LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 9);
		assertEquals(splitter.getTestData().size(), 4);
	}

	/**
	 * Test the methods splitData and getLOOByItemDate
	 *
	 * @throws Exception
	 */
	@Test
	public void testLOOByItemDate() throws Exception{
		conf.set("loocv.data.splitter", "loobyitemdate");
		convertorWithDate.processData();

		LOOCVDataSplitter splitter = new LOOCVDataSplitter(convertorWithDate, conf);
		splitter.splitData();

		assertEquals(splitter.getTrainData().size(), 9);
		assertEquals(splitter.getTestData().size(), 4);
	}
}
