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
package net.librec.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;

/**
 * Model File TestCase
 * 
 * @author WangYuFeng
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelFileTestCase extends BaseTestCase {

	private String filePath;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		filePath = conf.get("dfs.result.dir") + "/model-user";
	}

	@Test
	public void test1ModelFileWriter() throws LibrecException, IOException {
		double[] denseVectorArrs = { 0.1, 0.2, 0.3, 0.4, 0.5 };
		double[][] denseMatrixArrs = { {0.1, 0.2, 0.3, 0.4, 0.5}, {0.6,0.7,0.8,0.9,1.0} };
		DenseVector denseVector = new DenseVector(denseVectorArrs);
		DenseVectorWritable denseVectorWritable = new DenseVectorWritable(denseVector);
		DenseMatrix denseMatrix = new DenseMatrix(denseMatrixArrs);
		DenseMatrixWritable denseMatrixWritable = new DenseMatrixWritable(denseMatrix);
		FileOutputStream fos = new FileOutputStream(filePath);
		DataOutputStream out = new DataOutputStream(fos);
		ModelFile.Writer writer = new ModelFile.Writer(out);
		writer.writeData(out, denseVectorWritable);
		writer.writeData(out, denseMatrixWritable);
	}

	@Test
	public void test2ModelFileReader() throws LibrecException, IOException {
		FileInputStream fis = new FileInputStream(filePath);
		DataInputStream in = new DataInputStream(fis);
		ModelFile.Reader reader = new ModelFile.Reader(in);
		DenseVectorWritable denseVectorWritable = (DenseVectorWritable) reader.readData(in);
		DenseVector denseVector = (DenseVector)denseVectorWritable.getValue();
		if (denseVector != null && denseVector.getData() != null && denseVector.getData().length > 0) {
			for (double x : denseVector.getData()) {
				System.out.println(x);
			}
		}
		DenseMatrixWritable denseMatrixWritable = (DenseMatrixWritable)reader.readData(in);
		if (denseMatrixWritable != null && denseMatrixWritable.getValue() != null && ((DenseMatrix)denseMatrixWritable.getValue()).getData().length > 0) {
			double[][] denseMatrixArrs = ((DenseMatrix)denseMatrixWritable.getValue()).getData();
			for (int i = 0; i < denseMatrixArrs.length; i++) {
				for (int j = 0; j < denseMatrixArrs[i].length; j++) {
					System.out.println(i+"----"+j+"---"+denseMatrixArrs[i][j]);
				}
			}
		}
	}
}
