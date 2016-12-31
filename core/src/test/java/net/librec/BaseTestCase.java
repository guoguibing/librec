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

import net.librec.conf.Configuration;
import org.junit.After;
import org.junit.Before;

/**
 * Base TestCase
 * @author YuFeng Wang
 */
public class BaseTestCase {

	protected Configuration conf;

	@Before
	public void setUp() throws Exception {
		conf = new Configuration();
	}

	@After
	public void tearDown() throws Exception {
		conf = new Configuration();
	}

	/**
	 * @return the conf
	 */
	public Configuration getConf() {
		return conf;
	}

}
