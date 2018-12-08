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
package net.librec.job;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RecommenderJob test case
 * {@link net.librec.job.RecommenderJob}
 *
 * @author SunYatong
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RecommenderJobTestCase extends BaseTestCase {
	/**
	 * Recommender Job
	 */
	private RecommenderJob recommenderJob;
	/**
	 * recommended Item List
	 */
	private List<RecommendedItem> recommendedItemList;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		recommenderJob = new RecommenderJob(conf);
		recommendedItemList = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			recommendedItemList.add(new GenericRecommendedItem(i + "", i + "", i));
		}
	}

	/**
	 * user specified dir to save. user specified the recommender's simple name.
	 * 
	 * @throws LibrecException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void testSaveResult1() throws LibrecException, IOException, ClassNotFoundException {
		conf.set("rec.recommender.class", "aobpr");
		conf.set("dfs.result.dir", "../result");
		recommenderJob.saveResult(recommendedItemList);
	}

	/**
	 * user didn't specified dir to save. user specified the recommender's full
	 * name.
	 * 
	 * @throws LibrecException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void testSaveResult2() throws LibrecException, IOException, ClassNotFoundException {
		conf.set("rec.recommender.class", "net.librec.recommender.cf.AOBPRRecommender");
		// saveResult(recommendedItemList);
	}
}
