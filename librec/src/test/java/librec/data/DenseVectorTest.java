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
package librec.data;

import librec.data.DenseVector;
import librec.util.Randoms;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Random;

public class DenseVectorTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void addInputNotNullOutputNotNull2() {

    // Arrange
    final double[] doubleArray = {0x1p-786 /* 2.4571e-237 */};
    final DenseVector objectUnderTest = new DenseVector(doubleArray);
    final double[] doubleArray1 = {0x1.ffffffff00003p-754 /* 2.11064e-227 */};
    final DenseVector vec = new DenseVector(doubleArray1, true);

    // Act
    final DenseVector retval = objectUnderTest.add(vec);

    // Assert result
    Assert.assertNotNull(retval);
    Assert.assertArrayEquals(new double[] {0x1.0000000000002p-753 /* 2.11064e-227 */}, retval.data,
                             0.0);
    Assert.assertEquals(1, retval.size);
  }

  @Test
  public void addInputZeroOutputNotNull() {

    // Arrange
    final double[] doubleArray = {0x0.0000000000001p-1022 /* 4.94066e-324 */};
    final DenseVector objectUnderTest = new DenseVector(doubleArray, true);
    final double val = -0.0;

    // Act
    final DenseVector retval = objectUnderTest.add(val);

    // Assert result
    Assert.assertNotNull(retval);
    Assert.assertArrayEquals(new double[] {0x0.0000000000001p-1022 /* 4.94066e-324 */}, retval.data,
                             0.0);
    Assert.assertEquals(1, retval.size);
  }

  @Test
  public void kroneckerProductInputNullNullOutputNullPointerException() {

    // Arrange
    final DenseVector M = null;
    final DenseVector N = null;

    // Act
    thrown.expect(NullPointerException.class);
    DenseVector.kroneckerProduct(M, N);

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void minusInputNotNullOutputNotNull2() {

    // Arrange
    final double[] doubleArray = {0.0};
    final DenseVector objectUnderTest = new DenseVector(doubleArray, true);
    final double[] doubleArray1 = {0x1.0000000000001p+1 /* 2.0 */};
    final DenseVector vec = new DenseVector(doubleArray1);

    // Act
    final DenseVector retval = objectUnderTest.minus(vec);

    // Assert result
    Assert.assertNotNull(retval);
    Assert.assertArrayEquals(new double[] {-0x1.0000000000001p+1 /* -2.0 */}, retval.data, 0.0);
    Assert.assertEquals(1, retval.size);
  }

  @Test
  public void minusInputZeroOutputNotNull() {

    // Arrange
    final double[] doubleArray = {0x0.0000000000001p-1022 /* 4.94066e-324 */};
    final DenseVector objectUnderTest = new DenseVector(doubleArray, true);
    final double val = 0.0;

    // Act
    final DenseVector retval = objectUnderTest.minus(val);

    // Assert result
    Assert.assertNotNull(retval);
    Assert.assertArrayEquals(new double[] {0x0.0000000000001p-1022 /* 4.94066e-324 */}, retval.data,
                             0.0);
    Assert.assertEquals(1, retval.size);
  }

  @Test
  public void scaleInputPositiveOutputNotNull2() {

    // Arrange
    final double[] doubleArray = {-0x1.80013f8048d43p+515 /* -1.60896e+155 */};
    final DenseVector objectUnderTest = new DenseVector(doubleArray);
    final double val = 0x1.d049f0c080008p+1022 /* 8.15087e+307 */;

    // Act
    final DenseVector retval = objectUnderTest.scale(val);

    // Assert result
    Assert.assertNotNull(retval);
    Assert.assertArrayEquals(new double[] {Double.NEGATIVE_INFINITY}, retval.data, 0.0);
    Assert.assertEquals(1, retval.size);
  }
}
