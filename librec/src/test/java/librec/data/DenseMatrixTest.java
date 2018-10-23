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

import librec.data.DenseMatrix;
import librec.data.DenseVector;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class DenseMatrixTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void choleskyOutputRuntimeException() {

    // Arrange
    final DenseMatrix objectUnderTest = new DenseMatrix(null, -2_147_483_648, 0);

    // Act
    thrown.expect(RuntimeException.class);
    objectUnderTest.cholesky();

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void constructorInputNullOutputNullPointerException() {

    // Arrange
    final DenseMatrix mat = null;

    // Act, creating object to test constructor
    thrown.expect(NullPointerException.class);
    DenseMatrix objectUnderTest = new DenseMatrix(mat);

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void hadamardProductInputNotNullNotNullOutputException() throws Exception {

    // Arrange
    final DenseMatrix M = new DenseMatrix(0, 1);
    final DenseMatrix N = new DenseMatrix(null, 0, 134_217_729);

    // Act
    thrown.expect(Exception.class);
    DenseMatrix.hadamardProduct(M, N);

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void inverseOutputRuntimeException() {

    // Arrange
    final DenseMatrix objectUnderTest = new DenseMatrix(null, 8, 0);

    // Act
    thrown.expect(RuntimeException.class);
    objectUnderTest.inverse();

    // Method is not expected to return due to exception thrown
  }

  @Test
  public void invOutputRuntimeException() {

    // Arrange
    final DenseMatrix objectUnderTest = new DenseMatrix(null, 2_097_152, 2_097_153);

    // Act
    thrown.expect(RuntimeException.class);
    objectUnderTest.inv();

    // Method is not expected to return due to exception thrown
  }
}
