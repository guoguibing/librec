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

import librec.util.Gamma;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class GammaTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void digammaInputNegativeInfinityOutputNaN() {

    // Arrange
    final double x = Double.NEGATIVE_INFINITY;

    // Act
    final double retval = Gamma.digamma(x);

    // Assert result
    Assert.assertEquals(Double.NaN, retval, 0.0);
  }

  @Test
  public void digammaInputZeroOutputNegativeInfinity() {

    // Arrange
    final double x = 0.0;

    // Act
    final double retval = Gamma.digamma(x);

    // Assert result
    Assert.assertEquals(Double.NEGATIVE_INFINITY, retval, 0.0);
  }
}
