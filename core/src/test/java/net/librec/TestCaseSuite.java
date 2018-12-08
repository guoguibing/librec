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
package net.librec;

import net.librec.conf.ConfigurationTestCase;
import net.librec.data.model.ArffDataModelTestCase;
import net.librec.data.model.TextDataModelTestCase;
import net.librec.data.splitter.*;
import net.librec.filter.GenericRecommendedFilterTestCase;
import net.librec.job.RecommenderJobTestCase;
import net.librec.recommender.cf.ranking.*;
import net.librec.recommender.cf.rating.BiasedMFTestCase;
import net.librec.recommender.cf.rating.PMFTestCase;
import net.librec.similarity.BinaryCosineSimilarityTestCase;
import net.librec.tool.driver.DataDriverTestCase;
import net.librec.tool.driver.RecDriverTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test Suite
 * @author WangYuFeng
 */
@RunWith(Suite.class)
@SuiteClasses({
	//conf
	ConfigurationTestCase.class,
	//data.convertor
	//data.model
	ArffDataModelTestCase.class,
	TextDataModelTestCase.class,
	//data.splitter
	GivenNDataSplitterTestCase.class,
	KCVDataSplitterTestCase.class,
	LOOCVDataSplitterTestCase.class,
	RatioDataSplitterTestCase.class,
	//filter
	GenericRecommendedFilterTestCase.class,
	//io
//	ArrayWritableTestCase.class,
	//job
//	JobStatusTestCase.class,
	RecommenderJobTestCase.class,
	//recommender.rec.baseline
	//recommender.cf.rating
	BiasedMFTestCase.class,
	PMFTestCase.class,
	//recommender.cf.ranking
	AOBPRTestCase.class,
	BPRTestCase.class,
	CLIMFTestCase.class,
	CoFiSetTestCase.class,
	EALSTestCase.class,
	GBPRTestCase.class,
	RankSGDTestCase.class,
	WBPRTestCase.class,
	WRMFTestCase.class,
	//recommender.content

	//similarity
	BinaryCosineSimilarityTestCase.class,
	//tool.driver
	DataDriverTestCase.class,
	RecDriverTestCase.class
})
public class TestCaseSuite {

}
