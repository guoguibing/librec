package librec.util;

import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import librec.util.FileIO.Converter;
import librec.util.FileIO.MapWriter;
import librec.util.Strings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringsTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void isOnInputNotNullOutputFalse6() {

    // Arrange
    final String option = "OFF";

    // Act
    final boolean retval = Strings.isOn(option);

    // Assert result
    Assert.assertEquals(false, retval);
  }

  @Test
  public void isOnInputNotNullOutputFalse7() {

    // Arrange
    final String option = "FALSE";

    // Act
    final boolean retval = Strings.isOn(option);

    // Assert result
    Assert.assertEquals(false, retval);
  }

  @Test
  public void isOnInputNotNullOutputTrue() {

    // Arrange
    final String option = "ON";

    // Act
    final boolean retval = Strings.isOn(option);

    // Assert result
    Assert.assertEquals(true, retval);
  }

  @Test
  public void isOnInputNotNullOutputTrue2() {

    // Arrange
    final String option = "TRUE";

    // Act
    final boolean retval = Strings.isOn(option);

    // Assert result
    Assert.assertEquals(true, retval);
  }

  @Test
  public void lastInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String str = "";
    final int maxLength = 4;

    // Act
    final String retval = Strings.last(str, maxLength);

    // Assert result
    Assert.assertEquals("", retval);
  }

  @Test
  public void lastInputNotNullPositiveOutputNotNull2() {

    // Arrange
    final String str = "!!!!";
    final int maxLength = 4;

    // Act
    final String retval = Strings.last(str, maxLength);

    // Assert result
    Assert.assertEquals("...!", retval);
  }

  @Test
  public void repeatInputNotNullNegativeOutputNotNull() {

    // Arrange
    final String str = "!";
    final int repeat = -2_147_483_647;

    // Act
    final String retval = Strings.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("", retval);
  }

  @Test
  public void repeatInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String str = "010000000";
    final int repeat = 1;

    // Act
    final String retval = Strings.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("010000000", retval);
  }

  @Test
  public void repeatInputNotNullPositiveOutputNotNull2() {

    // Arrange
    final String str = "";
    final int repeat = 1_073_741_825;

    // Act
    final String retval = Strings.repeat(str, repeat);

    // Assert result
    Assert.assertEquals("", retval);
  }

  @Test
  public void repeatInputNullPositiveOutputNull() {

    // Arrange
    final String str = null;
    final int repeat = 8192;

    // Act
    final String retval = Strings.repeat(str, repeat);

    // Assert result
    Assert.assertNull(retval);
  }

  @Test
  public void shortStrInputNotNullNegativeOutputStringIndexOutOfBoundsException() {

    // Arrange
    final String input = "";
    final int len = -2_147_483_648;

    // Act
    thrown.expect(StringIndexOutOfBoundsException.class);
    Strings.shortStr(input, len);

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void shortStrInputNotNullPositiveOutputNotNull() {

    // Arrange
    final String input = "";
    final int len = 2_147_475_456;

    // Act
    final String retval = Strings.shortStr(input, len);

    // Assert result
    Assert.assertEquals("", retval);
  }

  @Test
  public void toIntInputNotNullOutputPositive() {

    // Arrange
    final String str = "5";

    // Act
    final int retval = Strings.toInt(str);

    // Assert result
    Assert.assertEquals(5, retval);
  }

  @Test
  public void toIntInputNotNullZeroOutputPositive() {

    // Arrange
    final String str = "6";
    final int val = 0;

    // Act
    final int retval = Strings.toInt(str, val);

    // Assert result
    Assert.assertEquals(6, retval);
  }

  @Test
  public void toLongInputNotNullOutputPositive() {

    // Arrange
    final String str = "8";

    // Act
    final long retval = Strings.toLong(str);

    // Assert result
    Assert.assertEquals(8L, retval);
  }

  @Test
  public void toLongInputNotNullZeroOutputZero() {

    // Arrange
    final String str = "0";
    final long val = 0L;

    // Act
    final long retval = Strings.toLong(str, val);

    // Assert result
    Assert.assertEquals(0L, retval);
  }

  @Test
  public void toStringInput0OutputNotNull() {

    // Arrange
    final double[] data = {};

    // Act
    final String retval = Strings.toString(data);

    // Assert result
    Assert.assertEquals("[]", retval);
  }

  @Test
  public void toStringInput0OutputNotNull2() {

    // Arrange
    final int[] data = {};

    // Act
    final String retval = Strings.toString(data);

    // Assert result
    Assert.assertEquals("[]", retval);
  }

  @Test
  public void toStringInput1NullOutputNotNull() throws Exception {

    // Arrange
    final ArrayList ts = new ArrayList();
    ts.add(14);
    final Converter lw = null;

    // Act
    final String retval = Strings.toString(ts, lw);

    // Assert result
    Assert.assertEquals("14", retval);
  }

  /**
 * Copyright (C) 2018 Diffblue
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
  @Test
  public void toStringInput1NullOutputNotNull2() {

    // Arrange
    final HashMap map = new HashMap();
    map.put(null, null);
    final MapWriter mw = null;

    // Act
    final String retval = Strings.toString(map, mw);

    // Assert result
    Assert.assertEquals("null -> null", retval);
  }

  @Test
  public void toStringInput1OutputNotNull() {

    // Arrange
    final int[] intArray = {};
    final int[][] data = {intArray};

    // Act
    final String retval = Strings.toString(data);

    // Assert result
    Assert.assertEquals("Dimension: 1 x 0\n[]\n", retval);
  }

  @Test
  public void toStringInput1OutputNotNull2() {

    // Arrange
    final int[] intArray = {0};
    final int[][] data = {intArray};

    // Act
    final String retval = Strings.toString(data);

    // Assert result
    Assert.assertEquals("Dimension: 1 x 1\n[0]\n", retval);
  }

  @Test
  public void toStringInput1OutputNotNull3() {

    // Arrange
    final int[] data = {-1_215_752_192};

    // Act
    final String retval = Strings.toString(data);

    // Assert result
    Assert.assertEquals("[-1215752192]", retval);
  } 
}
