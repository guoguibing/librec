/**
 * Copyright (C) 2016 LibRec
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
package net.librec.util;

/**
 * This class sets vector or matrix or tensor to zero
 *
 * @author bin wu (wubin@gs.zzu.edu.cn)
 */
public class ZeroSetter {

    /**
     * set all the elements of a double vector to zero
     *
     * @param a  the double vector to be set to zero
     * @param l1 the length of the vector
     */
    public static void zero(double[] a, int l1) {
        for (int i = 0; i < l1; i++)
            a[i] = 0;
    }

    /**
     * set all the elements of a integer vector to zero
     *
     * @param a  the integer vector to be set to zero
     * @param l1 the length of the vector
     */
    public static void zero(int[] a, int l1) {
        for (int i = 0; i < l1; i++)
            a[i] = 0;
    }

    /**
     * set all the elements of a integer matrix to zero
     *
     * @param a  the integer matrix to be set to zero
     * @param l1 the rows of the matrix
     * @param l2 the columns of the matrix
     */
    public static void zero(int[][] a, int l1, int l2) {
        for (int i = 0; i < l1; i++)
            for (int j = 0; j < l2; j++)
                a[i][j] = 0;
    }

    /**
     * set all the elements of a double matrix to zero
     *
     * @param a  the double matrix to be set to zero
     * @param l1 the rows of the matrix
     * @param l2 the columns of the matrix
     */
    public static void zero(double[][] a, int l1, int l2) {
        for (int i = 0; i < l1; i++)
            for (int j = 0; j < l2; j++)
                a[i][j] = 0.0;
    }

    /**
     * set all the elements of a double tensor to zero
     *
     * @param a  the double tensor to be set to zero
     * @param l1 the first dimension of the tensor
     * @param l2 the second dimension of the tensor
     * @param l3 the third dimension of the tensor
     */
    public static void zero(double[][][] a, int l1, int l2, int l3) {
        for (int i = 0; i < l1; i++)
            for (int j = 0; j < l2; j++)
                for (int k = 0; k < l3; k++)
                    a[i][j][k] = 0.0;
    }
}