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

import librec.util.Gaussian;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class GaussianTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void cdfInputNegativePositiveZeroOutputPositive() {

    // Arrange
    final double z = -0x1.040838p+1022 /* -4.56502e+307 */;
    final double mu = 0x1.bcddfcp+1023 /* 1.56198e+308 */;
    final double sigma = -0.0;

    // Act
    final double retval = Gaussian.cdf(z, mu, sigma);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }

  @Test
  public void cdfInputPositiveOutputPositive() {

    // Arrange
    final double z = 0x1.8093383076ffap+1022 /* 6.75145e+307 */;

    // Act
    final double retval = Gaussian.cdf(z);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }
}
