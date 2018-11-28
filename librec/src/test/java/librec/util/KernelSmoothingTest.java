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

import librec.util.KernelSmoothing;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;


public class KernelSmoothingTest {

  @Test
  public void kernelizeOutputPositiveOutput1() {

    // Arrange
    final double sim = 0x1.295a0b5086p+2 /* 4.64612 */;
    final double width = -0x1.ff296dp+1002 /* -8.55804e+301 */;
    final int kernelType = 201;

    // Act
    final double retval = KernelSmoothing.kernelize(sim, width, kernelType);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }

  @Test
  public void kernelizeOutputZeroOutput0() {

    // Arrange
    final double sim = 0x1.295a0b5086p+2 /* 4.64612 */;
    final double width = -0x1.ff296dp+1002 /* -8.55804e+301 */;
    final int kernelType = 202;

    // Act
    final double retval = KernelSmoothing.kernelize(sim, width, kernelType);

    // Assert result
    Assert.assertEquals(0.0, retval, 0.0);
  }

  @Test
  public void kernelizeeOutputPositiveOutput1() {

    // Arrange
    final double sim = 0x1.000001030d122p+0 /* 1.0 */;
    final double width = 0x1.be10048511d11p-1021 /* 7.75408e-308 */;
    final int kernelType = 202;

    // Act
    final double retval = KernelSmoothing.kernelize(sim, width, kernelType);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }
}
