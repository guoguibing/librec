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
package net.librec.recommender.baseline;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration.Resource;
import net.librec.job.RecommenderJob;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Constant Guess Test Case correspond to ConstantGuessRecommender
 * {@link net.librec.recommender.baseline.ConstantGuessRecommender}
 * 
 * @author liuxz
 */
public class ConstantGuessTestCase extends BaseTestCase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * test the whole process of Constant Guess Recommender
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 */
	@Test
	public void testRecommender() throws FileNotFoundException, IOException, ClassNotFoundException, LibrecException {
		Resource recourse = new Resource("rec/baseline/constantguess-test.properties");
		conf.addResource(recourse);
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}
}
