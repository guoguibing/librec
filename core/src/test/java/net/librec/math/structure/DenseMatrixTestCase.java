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
package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Test cases about the DenseMatrix class
 * {@link net.librec.math.structure.DenseMatrix}
 *
 * @author Ma Chen
 */
public class DenseMatrixTestCase extends BaseTestCase {
    private double[][] matrixData1 = new double[][] {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12}
    };


    @Test
    public void testTimesWithDenseMatrix() throws LibrecException {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        double[][] matrixData2 = new double[][] {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9},
                {10, 11, 12}
        };

        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        DenseMatrix result = denseMatrix1.times(denseMatrix2);
        assertEquals(70, result.get(0,0), 1e-8);
        assertEquals(184, result.get(1,1), 1e-8);

        System.out.println(new Date(System.currentTimeMillis()));
        DenseMatrix denseMatrix3 = new DenseMatrix(200,20000);
        DenseMatrix denseMatrix4 = new DenseMatrix(20000,200);
        denseMatrix3.init(3);
        denseMatrix4.init(3);
        System.out.println(new Date(System.currentTimeMillis()));
        DenseMatrix result2 = denseMatrix3.times(denseMatrix4);
        System.out.println(new Date(System.currentTimeMillis()));

    }

    @Test
    public void testInverse(){
        double[][] matrixData2 = new double[][] {
                {1, 2, 3},
                {4, 7, 6},
                {7, 8, 9},
        };
        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        DenseMatrix denseMatrix3 = denseMatrix2.clone();
        System.out.println(denseMatrix3);
        DenseMatrix inverseMatrix  = denseMatrix2.inverse();
        System.out.println(inverseMatrix);
        System.out.println(denseMatrix2.times(inverseMatrix));
    }

    @Test
    public void testAssign(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        double[][] matrixData2 = new double[][] {
                {10, 20, 30, 40},
                {50, 60, 70, 80},
                {90, 100, 110, 120},
        };
        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        denseMatrix1.assign(denseMatrix2);
        assertEquals(10, denseMatrix1.get(0, 0), 1e-8);

        denseMatrix1.assign((row, column, value) -> 6);
        assertEquals(6, denseMatrix1.get(0, 0), 1e-8);
    }

    @Test
    public void testInit(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        denseMatrix1.init();
        System.out.println(denseMatrix1.get(0, 0));

        denseMatrix1.init(5);
        System.out.println(denseMatrix1.get(0, 0));

        denseMatrix1.init(10, 0.1);
        System.out.println(denseMatrix1.get(0, 0));
    }

    @Test
    public void testClone(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseMatrix denseMatrix2 = denseMatrix1.clone();
        assertEquals(1, denseMatrix2.get(0, 0), 1e-8);
    }

    @Test
    public void testGetAndSet(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        denseMatrix1.set(0, 0, 10);
        assertEquals(10, denseMatrix1.get(0, 0), 1e-8);

        DenseVector vector = new VectorBasedDenseVector(new double[] {2, 4, 6, 8});
        denseMatrix1.set(0, vector);
        assertEquals(2, denseMatrix1.get(0, 0), 1e-8);
        assertEquals(4, denseMatrix1.get(0, 1), 1e-8);
        assertEquals(6, denseMatrix1.get(0, 2), 1e-8);
        assertEquals(8, denseMatrix1.get(0,3), 1e-8);
    }

    @Test
    public void testRow(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseVector vector = denseMatrix1.row(0);
        assertEquals(1, vector.get(0), 1e-8);
        assertEquals(2, vector.get(1), 1e-8);
        assertEquals(3, vector.get(2), 1e-8);
        assertEquals(4, vector.get(3), 1e-8);
    }

    @Test
    public void testColumn(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseVector vector = denseMatrix1.column(0);
        assertEquals(1, vector.get(0), 1e-8);
        assertEquals(5, vector.get(1), 1e-8);
        assertEquals(9, vector.get(2), 1e-8);
    }

    @Test
    public void testViewRow(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseVector vector = denseMatrix1.viewRow(0);
        assertEquals(1, vector.get(0), 1e-8);
        assertEquals(2, vector.get(1), 1e-8);
        assertEquals(3, vector.get(2), 1e-8);
        assertEquals(4, vector.get(3), 1e-8);
    }

    @Test
    public void testViewColumn(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseVector vector = denseMatrix1.viewColumn(0);
        assertEquals(1, vector.get(0), 1e-8);
        assertEquals(5, vector.get(1), 1e-8);
        assertEquals(9, vector.get(2), 1e-8);
    }

    @Test
    public void testPlus(){
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        denseMatrix1.plus(0, 0, 10);
        assertEquals(11, denseMatrix1.get(0, 0), 1e-8);

        DenseMatrix result1 = denseMatrix1.plus(6);
        assertEquals(8, result1.get(0, 1), 1e-8);

        double[][] matrixData2 = new double[][] {
                {10, 20, 30, 40},
                {50, 60, 70, 80},
                {90, 100, 110, 120},
        };
        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        DenseMatrix result2 = denseMatrix1.plus(denseMatrix2);
        assertEquals(21, result2.get(0, 0), 1e-8);
    }

    @Test
    public void testMinus() {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        double[][] matrixData2 = new double[][] {
                {10, 20, 30, 40},
                {50, 60, 70, 80},
                {90, 100, 110, 120},
        };
        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        DenseMatrix result = denseMatrix1.minus(denseMatrix2);
        assertEquals(-9, result.get(0, 0), 1e-8);
    }

    @Test
    public void testTimes() {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseMatrix result1 = denseMatrix1.times(10);
        assertEquals(10, result1.get(0, 0), 1e-8);

        DenseVector vector = new VectorBasedDenseVector(new double[] {2, 4, 6, 8});
        DenseVector result2 = denseMatrix1.times(vector);
        assertEquals(60, result2.get(0), 1e-8);

        double[][] matrixData2 = new double[][] {
                {1, 2},
                {3, 4},
                {5, 6},
                {7, 8}
        };
        DenseMatrix denseMatrix2 = new DenseMatrix(matrixData2);
        DenseMatrix result3 = denseMatrix1.times(denseMatrix2);
        System.out.println(result3.column(0).cardinality());
        System.out.println(result3.row(0).cardinality());
        assertEquals(50, result3.get(0, 0), 1e-8);
        assertEquals(60, result3.get(0, 1), 1e-8);
    }

    @Test
    public void testTranspose() {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        DenseMatrix result = denseMatrix1.transpose();
        assertEquals(5, result.get(0, 1), 1e-8);
    }

    @Test
    public void testNorm() {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        double result = denseMatrix1.norm();
        assertEquals(25.495097568, result, 1e-8);
    }

    @Test
    public void testIterator() {
        DenseMatrix denseMatrix1 = new DenseMatrix(matrixData1);

        Iterator<MatrixEntry> iterator = denseMatrix1.iterator();
        assertEquals(1, iterator.next().get(), 1e-8);
        assertEquals(2, iterator.next().get(), 1e-8);
        assertEquals(3, iterator.next().get(), 1e-8);
    }

    @Test
    public void testTimesWithDenseMatrixPerformance() {
        int dimension = 2000;
        double[][] data1 = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        double[][] data2 = data1.clone();

        DenseMatrix denseMatrix1 = new DenseMatrix(data1);
        DenseMatrix denseMatrix2 = new DenseMatrix(data2);

        long startTime = System.currentTimeMillis();
        DenseMatrix result = denseMatrix1.times(denseMatrix2);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        System.out.println(elapsedTime);
    }

    @Test
    public void testTimesWithDenseVectorPerformance() {
        int dimension = 5000;
        double[][] data1 = new double[dimension][dimension];
        double[] data2 = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        for (int i = 0; i < dimension; i++) {
            data2[i] = i;
        }

        DenseMatrix denseMatrix = new DenseMatrix(data1);
        DenseVector denseVector = new VectorBasedDenseVector(data2);

        long startTime = System.currentTimeMillis();
        DenseVector result = denseMatrix.times(denseVector);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        System.out.println(elapsedTime);
    }

    @Test
    public void testTimesWithSparseVectorPerformance() {
        int dimension = 5000;
        double[][] data1 = new double[dimension][dimension];
        double[] data2 = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        Random rand = new Random();
        for (int i = 0; i < dimension; i++) {
            if (rand.nextFloat() >= 0.5) { // sparsity
                data2[i] = i;
            }
        }

        DenseMatrix denseMatrix = new DenseMatrix(data1);
        DenseVector denseVector = new VectorBasedDenseVector(data2);
        VectorBasedSequentialSparseVector sparseVector = new VectorBasedSequentialSparseVector(denseVector);

        long startTime = System.currentTimeMillis();
        DenseVector result = denseMatrix.times(sparseVector);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        System.out.println(elapsedTime);
    }

    @Test
    public void testTimesWithSparseMatrixPerformance() {
        int dimension = 3000;
        double[][] data1 = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        Random rand = new Random();
        Table<Integer, Integer, Double> dataTable = HashBasedTable.create();
        // RowRandomAccessSparseMatrix rowRandomAccessSparseMatrix = new RowRandomAccessSparseMatrix(dimenstion, dimenstion);
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (rand.nextFloat() >= 0.5) {  // sparsity
                    // rowRandomAccessSparseMatrix.set(i, j, i + j);
                    dataTable.put(i, j, (double) i + j);
                }
            }
        }

        DenseMatrix denseMatrix = new DenseMatrix(data1);

        // times with RowSequentialAccessSparseMatrix
        SequentialAccessSparseMatrix rowSequentialAccessSparseMatrix = new SequentialAccessSparseMatrix(dimension, dimension, dataTable);
//        RowSequentialAccessSparseMatrix rowSequentialAccessSparseMatrix = new RowSequentialAccessSparseMatrix(dimension, dimension, dataTable);
        long startTime = System.currentTimeMillis();
        DenseMatrix result = denseMatrix.times(rowSequentialAccessSparseMatrix);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("time:" + elapsedTime);
    }

    @Test
    public void testCholeskyPerformance() {
        int dimension = 5000;
        double[][] data1 = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        DenseMatrix denseMatrix = new DenseMatrix(data1);
        try {
            long startTime = System.currentTimeMillis();
            DenseMatrix result = denseMatrix.cholesky();
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.println("time:" + elapsedTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInversePerformance() {
        int dimension = 5000;
        double[][] data1 = new double[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                data1[i][j] = i + j;
            }
        }

        DenseMatrix denseMatrix = new DenseMatrix(data1);
        try {
            long startTime = System.currentTimeMillis();
            DenseMatrix result = denseMatrix.inverse();
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.println("time:" + elapsedTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
