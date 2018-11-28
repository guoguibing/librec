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
package librec.util;

import librec.util.Measures;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MeasuresTest {

  @Test
  public void APOutputPositive() {

    // Arrange
    final ArrayList rankedList = new ArrayList();
    rankedList.add(1_073_741_825);
    final ArrayList groundTruth = new ArrayList();
    groundTruth.add(1_073_741_825);

    // Act
    final double retval = Measures.AP(rankedList, groundTruth);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }

  @Test
  public void ASYMMLossOutputPositive() {

    // Arrange
    final double rate = -0x1.27fffffcp-996 /* -1.72654e-300 */;
    final double pred = -0x1.28p-996 /* -1.72654e-300 */;
    final double minRate = -0x1p-993 /* -1.19458e-299 */;
    final double maxRate = 0x1.6c000000ffffep-994 /* 8.4927e-300 */;

    // Act
    final double retval = Measures.ASYMMLoss(rate, pred, minRate, maxRate);

    // Assert result
    Assert.assertEquals(0x0.1p-1022 /* 1.39067e-309 */, retval, 0.0);
  }

  @Test
  public void ASYMMOutputPositive2() {

    // Arrange
    final double rate = -0x0.0020800000004p-1022 /* -1.10344e-311 */;
    final double pred = -0x0.0000400000004p-1022 /* -8.48798e-314 */;
    final double minRate = -0x1p-985 /* -3.05812e-297 */;
    final double maxRate = 0x1.fffffffffffffp-986 /* 3.05812e-297 */;

    // Act
    final double retval = Measures.ASYMMLoss(rate, pred, minRate, maxRate);

    // Assert result
    Assert.assertEquals(0x0.00306p-1022 /* 1.64242e-311 */, retval, 0.0);
  }

  @Test
  public void AUCOutputPositive() {

    // Arrange
    final ArrayList rankedList = new ArrayList();
    final ArrayList groundTruth = new ArrayList();
    final int num_dropped_items = -2_147_483_648;

    // Act
    final double retval = Measures.AUC(rankedList, groundTruth, num_dropped_items);

    // Assert result
    Assert.assertEquals(0.5, retval, 0.0);
  }

  @Test
  public void HitsAtOutputPositive() {

    // Arrange
    final ArrayList rankedList = new ArrayList();
    rankedList.add(null);
    final ArrayList groundTruth = new ArrayList();
    groundTruth.add(null);
    final int n = 1;

    // Act
    final int retval = Measures.HitsAt(rankedList, groundTruth, n);

    // Assert result
    Assert.assertEquals(1, retval);
  }

  @Test
  public void RROutputPositive() {

    // Arrange
    final ArrayList rankedList = new ArrayList();
    rankedList.add(-520_095_677);
    final ArrayList groundTruth = new ArrayList();
    groundTruth.add(-520_095_677);

    // Act
    final double retval = Measures.RR(rankedList, groundTruth);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }
}
