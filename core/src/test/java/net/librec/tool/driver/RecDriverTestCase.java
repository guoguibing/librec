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
package net.librec.tool.driver;

import net.librec.BaseTestCase;
import org.junit.Test;

/**
 * Test Case corresponds to
 * {@link net.librec.tool.driver.RecDriver}
 *
 * @author SunYatong & WangYuFeng
 */
public class RecDriverTestCase extends BaseTestCase{

    /**
     * Test the option "-exec".
     *
     * @throws Exception
     */
    @Test
    public void testExec() throws Exception{
        // D:/temp/pmf-test.properties
        //../data/test/pmf-test.properties
        String[] args = {"-exec", "-conf", "G:/LibRec/librec/core/src/main/resources/rec/baseline/constantguess-test.properties"};
        RecDriver.main(args);
    }

}
