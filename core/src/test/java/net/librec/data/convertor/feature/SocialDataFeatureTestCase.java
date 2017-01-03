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
package net.librec.data.convertor.feature;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.util.DriverClassUtil;
import net.librec.util.ReflectionUtil;

/**
 * Social Data Feature Test Case corresponds to SocialDataFeature
 * {@link net.librec.data.convertor.feature.SocialDataFeature}
 *
 * @author SunYatong
 */
public class SocialDataFeatureTestCase extends BaseTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        conf.set("data.input.path", "test/ratings.txt");
        conf.set("data.feature.format", "social");
    }

    /**
     * Test the function of read file.
     *
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    @Test
    public void testReadFile() throws IOException, LibrecException, ClassNotFoundException {
        String inputPath = conf.get("dfs.data.dir") + "/" + conf.get("data.input.path");
        TextDataConvertor textDataConvertor = new TextDataConvertor(inputPath);
        textDataConvertor.processData();
        conf.set("data.feature.path", "test/trust.txt");
        SocialDataFeature dataFeature = (SocialDataFeature) ReflectionUtil.newInstance(DriverClassUtil.getClass(conf.get("data.feature.format")), conf);
        dataFeature.setUserMappingData(textDataConvertor.getUserIds());
        dataFeature.processData();

        assertTrue(dataFeature.getUserFeature().numRows() == dataFeature.getUserFeature().numColumns());
        assertTrue(dataFeature.getUserFeature().numRows() <= textDataConvertor.getUserIds().size());
    }

    /**
     * Test the function of read directory.
     *
     * @throws IOException
     */
    @Test
    public void testReadDir() throws IOException, LibrecException {
        String inputPath = conf.get("dfs.data.dir") + "/" + conf.get("data.input.path");
        TextDataConvertor textDataConvertor = new TextDataConvertor(inputPath);
        textDataConvertor.processData();
        conf.set("data.feature.path", "test/test-trust-dir");
        SocialDataFeature dataFeature = new SocialDataFeature(conf);
        dataFeature.setUserMappingData(textDataConvertor.getUserIds());
        dataFeature.processData();

        assertTrue(dataFeature.getUserFeature().numRows() == dataFeature.getUserFeature().numColumns());
        assertTrue(dataFeature.getUserFeature().numRows() <= textDataConvertor.getUserIds().size());
    }
}
