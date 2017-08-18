// Copyright (C) 2014-2015 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package net.librec.math.algorithm;

import net.librec.common.LibrecException;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.SparseVector;

import java.util.*;

/**
 * @author Guo Guibing
 */
public class Randoms {
    private static Random r = new Random(System.currentTimeMillis());

    private static List<Object> _tempList = new ArrayList<>();

    /**
     * Random generate an integer in [0, range)
     *
     * @param range  range of the interval
     * @return  an integer random generated in [0, range)
     */
    public static int uniform(int range) {
        return uniform(0, range);
    }

    public static void seed(long seed) {
        r = new Random(seed);
    }

    /**
     * Random generate an integer in [min, max)
     *
     * @param min  minimum of the range
     * @param max  maximum of the range
     * @return     an integer random generated in [min, max)
     */
    public static int uniform(int min, int max) {
        return min + r.nextInt(max - min);
    }

    /**
     * Return real number uniformly in [0, 1).
     *
     * @return real number uniformly in [0, 1).
     */
    public static double random() {
        return uniform();
    }

    /**
     * Return a random number from a given list of numbers.
     *
     * @param <T>  type parameter
     * @param data a given list of numbers
     * @return a random number from a given list of numbers
     */
    public static <T> T random(List<T> data) {
        int idx = uniform(data.size());

        return data.get(idx);
    }

    /**
     * A random double array with values in [0, 1)
     *
     * @param size the size of random array
     * @return a random double array with values in [0, 1)
     */
    public static double[] doubles(int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++)
            array[i] = random();

        return array;
    }

    /**
     * A random double array with values in [min, max).
     *
     * @param min  minimum
     * @param max  maximum
     * @param size the size of random array
     * @return a random double array with values in [min, max)
     */
    public static double[] doubles(double min, double max, int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++)
            array[i] = uniform(min, max);

        return array;
    }

    /**
     * Random (uniformly distributed) double in [0, 1)
     *
     * @return Random (uniformly distributed) double in [0, 1)
     */
    public static double uniform() {
        return uniform(0.0, 1.0);
    }

    /**
     * Random (uniformly distributed) double in [min, max)
     *
     * @param max max of the range
     * @param min min of the range
     * @return Random (uniformly distributed) double in [min, max)
     */
    public static double uniform(double min, double max) {
        return min + (max - min) * r.nextDouble();
    }

    /**
     * Return a boolean, which is true with probability p, and false otherwise.
     *
     * @param p probability p
     * @return a boolean, which is true with probability p, and false otherwise.
     */
    public static boolean bernoulli(double p) {
        return uniform() < p;
    }

    /**
     * Return a boolean, which is true with probability .5, and false otherwise.
     *
     * @return a boolean, which is true with probability .5, and false otherwise.
     */
    public static boolean bernoulli() {
        return bernoulli(0.5);
    }

    /**
     * Return a real number from a Gaussian distribution with given mean and stddev.
     *
     * @param mu    mean
     * @param sigma stddev
     * @return  a real number from a Gaussian distribution with given mean and stddev
     */
    public static double gaussian(double mu, double sigma) {
        return mu + sigma * r.nextGaussian();
    }

    /**
     * Randomly sample 1 point from Gamma Distribution with the given parameters. The code is from Mahout
     * (http://mahout.apache.org/), available under Apache 2 license.
     *
     * @param alpha alpha parameter for Gamma Distribution.
     * @param scale scale parameter for Gamma Distribution.
     * @return a sample point randomly drawn from the given distribution.
     */
    public static double gamma(double alpha, double scale) {
        double rate = 1 / scale;

        if (alpha <= 0.0 || rate <= 0.0) {
            throw new IllegalArgumentException();
        }

        double gds;
        double b = 0.0;

        // CASE A: Acceptance rejection algorithm gs
        if (alpha < 1.0) {
            b = 1.0 + 0.36788794412 * alpha; // Step 1
            while (true) {
                double p = b * r.nextDouble();
                // Step 2. Case gds <= 1
                if (p <= 1.0) {
                    gds = Math.exp(Math.log(p) / alpha);
                    if (Math.log(r.nextDouble()) <= -gds) {
                        return gds / rate;
                    }
                }
                // Step 3. Case gds > 1
                else {
                    gds = -Math.log((b - p) / alpha);
                    if (Math.log(r.nextDouble()) <= ((alpha - 1.0) * Math.log(gds))) {
                        return gds / rate;
                    }
                }
            }
        }
        // CASE B: Acceptance complement algorithm gd (gaussian distribution,
        // box muller transformation)
        else {
            double ss = 0.0;
            double s = 0.0;
            double d = 0.0;

            // Step 1. Preparations
            if (alpha != -1.0) {
                ss = alpha - 0.5;
                s = Math.sqrt(ss);
                d = 5.656854249 - 12.0 * s;
            }

            // Step 2. Normal deviate
            double v12;
            double v1;

            do {
                v1 = 2.0 * r.nextDouble() - 1.0;
                double v2 = 2.0 * r.nextDouble() - 1.0;
                v12 = v1 * v1 + v2 * v2;
            } while (v12 > 1.0);

            double t = v1 * Math.sqrt(-2.0 * Math.log(v12) / v12);
            double x = s + 0.5 * t;
            gds = x * x;

            if (t >= 0.0) { // Immediate acceptance
                return gds / rate;
            }

            double u = r.nextDouble();
            if (d * u <= t * t * t) { // Squeeze acceptance
                return gds / rate;
            }

            double q0 = 0.0;
            double si = 0.0;
            double c = 0.0;

            // Step 4. Set-up for hat case
            if (alpha != -1.0) {
                double rr = 1.0 / alpha;
                double q9 = 0.0001710320;
                double q8 = -0.0004701849;
                double q7 = 0.0006053049;
                double q6 = 0.0003340332;
                double q5 = -0.0003349403;
                double q4 = 0.0015746717;
                double q3 = 0.0079849875;
                double q2 = 0.0208333723;
                double q1 = 0.0416666664;

                q0 = ((((((((q9 * rr + q8) * rr + q7) * rr + q6) * rr + q5) * rr + q4) * rr + q3) * rr + q2) * rr + q1)
                        * rr;

                if (alpha > 3.686) {
                    if (alpha > 13.022) {
                        b = 1.77;
                        si = 0.75;
                        c = 0.1515 / s;
                    } else {
                        b = 1.654 + 0.0076 * ss;
                        si = 1.68 / s + 0.275;
                        c = 0.062 / s + 0.024;
                    }
                } else {
                    b = 0.463 + s - 0.178 * ss;
                    si = 1.235;
                    c = 0.195 / s - 0.079 + 0.016 * s;
                }
            }

            double v, q;
            double a9 = 0.104089866;
            double a8 = -0.112750886;
            double a7 = 0.110368310;
            double a6 = -0.124385581;
            double a5 = 0.142873973;
            double a4 = -0.166677482;
            double a3 = 0.199999867;
            double a2 = -0.249999949;
            double a1 = 0.333333333;

            // Step 5. Calculation of q
            if (x > 0.0) {
                // Step 6.
                v = t / (s + s);
                if (Math.abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
                }
                // Step 7. Quotient acceptance
                else {
                    q = q0
                            + 0.5
                            * t
                            * t
                            * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1)
                            * v;
                }
                if (Math.log(1.0 - u) <= q) {
                    return gds / rate;
                }
            }

            double e7 = 0.000247453;
            double e6 = 0.001353826;
            double e5 = 0.008345522;
            double e4 = 0.041664508;
            double e3 = 0.166666848;
            double e2 = 0.499999994;
            double e1 = 1.000000000;

            // Step 8. Double exponential deviate t
            while (true) {
                double sign_u;
                double e;
                do { // Step 9. Rejection of t
                    e = -Math.log(r.nextDouble());
                    u = r.nextDouble();
                    u = u + u - 1.0;
                    sign_u = (u > 0) ? 1.0 : -1.0;
                    t = b + (e * si) * sign_u;
                } while (t <= -0.71874483771719);

                // Step 10. New q(t)
                v = t / (s + s);

                if (Math.abs(v) > 0.25) {
                    q = q0 - s * t + 0.25 * t * t + (ss + ss) * Math.log(1.0 + v);
                } else {
                    q = q0
                            + 0.5
                            * t
                            * t
                            * ((((((((a9 * v + a8) * v + a7) * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1)
                            * v;
                }

                // Step 11.
                if (q <= 0.0) {
                    continue;
                }

                // Step 12. Hat acceptance
                double w;
                if (q > 0.5) {
                    w = Math.exp(q) - 1.0;
                } else {
                    w = ((((((e7 * q + e6) * q + e5) * q + e4) * q + e3) * q + e2) * q + e1) * q;
                }

                if (c * u * sign_u <= w * Math.exp(e - 0.5 * t * t)) {
                    x = s + 0.5 * t;
                    return x * x / rate;
                }
            }
        }
    }

    /**
     * Randomly sample a matrix from Wishart Distribution with the given parameters.
     *
     * @param scale scale parameter for Wishart Distribution.
     * @param df    degree of freedom for Wishart Distribution.
     * @return the sample randomly drawn from the given distribution.
     * @throws LibrecException if error occurs
     */
    public static DenseMatrix wishart(DenseMatrix scale, double df) throws LibrecException {
        DenseMatrix A = scale.cholesky();
        if (A == null)
            return null;

        int p = scale.numRows();
        DenseMatrix z = new DenseMatrix(p, p);

        for (int i = 0; i < p; i++) {
            for (int j = 0; j < p; j++) {
                z.set(i, j, Randoms.gaussian(0, 1));
            }
        }

        SparseVector y = new SparseVector(p);
        for (int i = 0; i < p; i++)
            y.set(i, Randoms.gamma((df - (i + 1)) / 2, 2));

        DenseMatrix B = new DenseMatrix(p, p);
        B.set(0, 0, y.get(0));

        if (p > 1) {
            // rest of diagonal:
            for (int j = 1; j < p; j++) {
                SparseVector zz = new SparseVector(j);
                for (int k = 0; k < j; k++)
                    zz.set(k, z.get(k, j));

                B.set(j, j, y.get(j) + zz.inner(zz));
            }

            // first row and column:
            for (int j = 1; j < p; j++) {
                B.set(0, j, z.get(0, j) * Math.sqrt(y.get(0)));
                B.set(j, 0, B.get(0, j)); // mirror
            }
        }

        if (p > 2) {
            for (int j = 2; j < p; j++) {
                for (int i = 1; i <= j - 1; i++) {
                    SparseVector zki = new SparseVector(i);
                    SparseVector zkj = new SparseVector(i);

                    for (int k = 0; k <= i - 1; k++) {
                        zki.set(k, z.get(k, i));
                        zkj.set(k, z.get(k, j));
                    }
                    B.set(i, j, z.get(i, j) * Math.sqrt(y.get(i)) + zki.inner(zkj));
                    B.set(j, i, B.get(i, j)); // mirror
                }
            }
        }

        return A.transpose().mult(B).mult(A);
    }

    /**
     * Return an integer with a Poisson distribution with mean lambda.
     *
     * @param lambda  mean lambda
     * @return  an integer with a Poisson distribution with mean lambda
     */
    public static int poisson(double lambda) {
        // using algorithm given by Knuth
        // see http://en.wikipedia.org/wiki/Poisson_distribution
        int k = 0;
        double p = 1.0;
        double L = Math.exp(-lambda);
        do {
            k++;
            p *= uniform();
        } while (p >= L);
        return k - 1;
    }

    /**
     * Return a real number with a Pareto distribution with parameter alpha.
     *
     * @param alpha  parameter alpha
     * @return a real number with a Pareto distribution with parameter alpha.
     */
    public static double pareto(double alpha) {
        return Math.pow(1 - uniform(), -1.0 / alpha) - 1.0;
    }

    /**
     * Return a real number with a Cauchy distribution.
     *
     * @return a real number with a Cauchy distribution
     */
    public static double cauchy() {
        return Math.tan(Math.PI * (uniform() - 0.5));
    }

    /**
     * Return a number from a discrete distribution: i with probability a[i]. Precondition: array entries are
     * nonnegative and their sum (very nearly) equals 1.0.
     *
     * @param a probability a[i]
     * @return  a number from the discrete distribution
     */
    public static int discrete(double[] a) {
        double EPSILON = 1E-6;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0.0)
                throw new IllegalArgumentException("array entry " + i + " is negative: " + a[i]);
            sum = sum + a[i];
        }
        if (sum > 1.0 + EPSILON || sum < 1.0 - EPSILON)
            throw new IllegalArgumentException("sum of array entries not equal to one: " + sum);

        // the for loop may not return a value when both r is (nearly) 1.0 and when the cumulative sum is less than 1.0 (as a result of floating-point roundoff error)
        while (true) {
            double r = uniform();
            sum = 0.0;
            for (int i = 0; i < a.length; i++) {
                sum = sum + a[i];
                if (sum > r)
                    return i;
            }
        }
    }

    /**
     * Return a real number from an exponential distribution with rate lambda.
     *
     * @param lambda rate lambda
     * @return a real number from an exponential distribution with rate lambda.
     */
    public static double exp(double lambda) {
        return -Math.log(1 - uniform()) / lambda;
    }

    /**
     * generate next random integer in a range besides exceptions
     *
     * @param range      the range located of next integer
     * @param exceptions the exception values when generating integers, sorted first
     * @return next no-repeated random integer
     */
    public static int nextInt(int range, int... exceptions) {
        return nextInt(0, range, exceptions);
    }

    /**
     * generate next random integer in a range [min, max) besides exceptions
     *
     * @param min        the minimum of range
     * @param max        the maximum of range
     * @param exceptions the exception values when generating integers, sorted first
     * @return next no-repeated random integer
     */
    public static int nextInt(int min, int max, int... exceptions) {
        int next;
        while (true) {
            next = min + r.nextInt(max - min);
            if (exceptions != null && exceptions.length > 0 && Arrays.binarySearch(exceptions, next) >= 0) {
                continue;
            }
            if (_tempList.contains(next))
                continue;
            else {
                _tempList.add(next);
                break;
            }
        }
        return next;
    }

    /**
     * Generate no repeat {@code size} indexes from {@code min} to {@code max}
     *
     * @param min   min of the range
     * @param max   max of the range
     * @param size  size of the index array
     * @return  no repeat {@code size} indexes from {@code min} to {@code max}
     */
    public static int[] indexs(int size, int min, int max) {
        Set<Integer> used = new HashSet();
        int[] index = new int[size];

        for (int i = 0; i < index.length; i++) {
            while (true) {
                int ind = uniform(min, max);

                if (!used.contains(ind)) {
                    index[i] = ind;
                    used.add(ind);
                    break;
                }
            }
        }

        return index;
    }

    public static void clearCache() {
        _tempList.clear();
    }

    /**
     * generate next integers array with no repeated elements
     *
     * @param length     the length of the array
     * @param range      the index range of the array, default [0, range)
     * @return ascending sorted integer array
     * @throws Exception if the range is less than length, an exception will be thrown
     */
    public static int[] nextIntArray(int length, int range) throws Exception {
        return nextIntArray(length, 0, range, null);
    }

    public static int[] nextIntArray(int length, int range, int... exceptions) throws Exception {
        return nextIntArray(length, 0, range, exceptions);
    }

    public static int[] nextIntArray(int length, int min, int max) throws Exception {
        return nextIntArray(length, min, max, null);
    }

    public static int[] nextIntArray(int length, int min, int max, int... exceptions) throws Exception {
        int maxLen = max - min; // because max itself is not counted
        if (maxLen < length)
            throw new Exception("The range is less than legth");

        int[] index = new int[length];
        if (maxLen == length) {
            for (int i = 0; i < length; i++)
                index[i] = min + i;
        } else {
            Randoms.clearCache();
            for (int i = 0; i < index.length; i++)
                index[i] = Randoms.nextInt(min, max, exceptions);
            Arrays.sort(index);
        }
        return index;
    }

    /**
     * Generate a set of random (unique) integers in the range [min, max) with length {@code length}
     *
     * @param max    max of the range
     * @param min    min of the range
     * @param length length of the List
     * @return a set of unique integers
     * @throws Exception if error occurs
     */
    public static List<Integer> randInts(int length, int min, int max) throws Exception {
        int len = max - min;
        if (len < length)
            throw new Exception("The range is less than legth");

        Set<Integer> ints = new HashSet();

        while (true) {
            int rand = min + r.nextInt(max - min);
            ints.add(rand);

            if (ints.size() >= length)
                break;
        }

        List<Integer> res = new ArrayList(ints);
        Collections.sort(res);

        return res;
    }

    /**
     * Get a normalize array of probabilities
     *
     * @param size array size
     * @return a normalize array of probabilities
     */
    public static double[] randProbs(int size) {
        if (size < 1)
            throw new IllegalArgumentException("The size param must be greate than zero");

        double[] pros = new double[size];

        int sum = 0;
        for (int i = 0; i < pros.length; i++) {
            //avoid zero
            pros[i] = r.nextInt(size) + 1;
            sum += pros[i];
        }

        //normalize
        for (int i = 0; i < pros.length; i++) {
            pros[i] = pros[i] / sum;
        }

        return pros;
    }

    public static int[] ints(int range, int size) {
        int[] data = new int[size];

        for (int i = 0; i < size; i++)
            data[i] = uniform(range);

        return data;
    }

    public static int[] ints(int min, int max, int size) {
        int[] data = new int[size];

        for (int i = 0; i < size; i++)
            data[i] = uniform(min, max);

        return data;
    }

    public static List<Double> list(int size) {
        return list(size, 0, 1, false);
    }

    public static List<Double> list(int size, int min, int max) {
        return list(size, min, max, false);
    }

    public static List<Double> list(int size, int min, int max, boolean isInteger) {
        List<Double> list = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            if (isInteger)
                list.add(uniform(min, max) + 0.0);
            else
                list.add(uniform(min + 0.0, max + 0.0));
        }

        return list;
    }

    /**
     * Generate a permutation from min to max
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a permutation
     */
    public static List<Integer> permute(int min, int max) {
        List<Integer> list = new ArrayList();

        int len = max - min + 1;
        for (int i = 0; i < len; i++) {
            while (true) {
                int index = uniform(min, max + 1);

                if (!list.contains(index)) {
                    list.add(index);
                    break;
                }
            }
        }

        return list;
    }
}
