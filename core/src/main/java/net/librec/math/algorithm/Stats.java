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

import net.librec.util.Lists;

import java.util.*;

/**
 * @author Guo Guibing
 */
public class Stats {

    /**
     * Return mean value of a sample.
     * @param data  the set to sample from
     *
     * @return mean value of a sample
     */
    public static double mean(Collection<? extends Number> data) {
        double sum = 0.0;
        int count = 0;
        for (Number d : data) {
            if (!Double.isNaN(d.doubleValue())) {
                sum += d.doubleValue();
                count++;
            }
        }

        return sum / count;
    }

    /**
     * Return harmonic mean.
     * @param a parameter of the function
     * @param b parameter of the function
     *
     * @return harmonic mean
     */
    public static double hMean(double a, double b) {
        return 2 * a * b / (a + b);
    }

    /**
     * reference:
     * http://www.weibull.com/DOEWeb/unbiased_and_biased_estimators.htm
     * <p>
     * Notes: the sample mean and population mean is estimated in the same way.
     *
     * @param data  the set to sample from
     * @return mean value of a sample
     */
    public static double mean(double[] data) {
        double sum = 0.0;
        int count = 0;
        for (double d : data) {
            if (!Double.isNaN(d)) {
                sum += d;
                count++;
            }
        }

        return sum / count;
    }

    public static double mode(double[] data) {
        Map<Double, Integer> modes = new HashMap<Double, Integer>();

        double mode = Double.NaN;
        int max = 0;
        for (double d : data) {
            int count = 0;
            if (modes.containsKey(d))
                count = modes.get(d);
            count++;

            if (max < count) {
                mode = d;
                max = count;
            }

            modes.put(d, count);
        }

        return mode;
    }

    public static double weightedcMean(double[] a, double[] w) {
        double sum = 0.0, ws = 0.0;
        for (int i = 0; i < a.length; i++) {
            if (!Double.isNaN(a[i]) && !Double.isNaN(w[i])) {
                sum += a[i] * w[i];
                ws += w[i];
            }
        }

        return sum / ws;
    }

    /**
     * Return weighted average value of {@code data} and {@code weights}.
     *
     * @param data     data set
     * @param weights  weight for the data
     * @return weighted average value of {@code data} and {@code weights}
     */
    public static double average(List<Double> data, List<Double> weights) {
        double sum = 0, ws = 0;

        for (int i = 0; i < data.size(); i++) {
            double value = data.get(i);
            double weight = weights.get(i);

            sum += value * weight;
            ws += weight;
        }

        return sum / ws;
    }

    /**
     * Calculate the median value of an array,
     * <em>Note that the values of doulbe.NaN will be ignored silently.</em>
     *
     * @param data  an array
     * @return  median value of an array,
     */
    public static double median(double[] data) {
        double median = 0.0;

        // make a clone: do not change original data
        double[] clones = data.clone();
        Arrays.sort(clones);

        int size = clones.length;
        int index = 0;
        if (size % 2 == 0) {
            index = clones.length / 2 - 1;
            median = (clones[index] + clones[index + 1]) / 2.0;
        } else {
            index = (clones.length + 1) / 2 - 1;
            median = clones[index];
        }

        return median;
    }

    /**
     * Calculate the median value of a data collection,
     * <em>Note that the values of doulbe.NaN will be ignored silently</em>
     *
     * @param data  a data collection
     * @return  the median value of the data collection
     */
    public static double median(Collection<? extends Number> data) {
        return median(Lists.toArray(data));
    }

    /**
     * Calculate a sample's variance.
     *
     * @param data  a data array
     * @return  variance of the sample
     */
    public static double var(double[] data) {
        return var(data, mean(data));
    }

    /**
     * Calculate a sample's variance
     * <p>
     * refers to:
     * http://www.weibull.com/DOEWeb/unbiased_and_biased_estimators.htm, for an
     * explanation why the denominator is (n-1) rather than n.
     *
     * @param data  a data array
     * @param mean  mean of the data array
     * @return the sample's variance
     */
    public static double var(double[] data, double mean) {
        if (data.length == 0)
            return Double.NaN;

        double sum = 0.0;
        for (int i = 0; i < data.length; i++)
            sum += (data[i] - mean) * (data[i] - mean);

        return sum / data.length;
    }

    /**
     * Calculate the standard deviation.
     *
     * @param data  a data collection
     * @return  standard deviation of the collection
     */
    public static double sd(Collection<? extends Number> data) {
        return sd(data, mean(data));
    }

    /**
     * Calculate the standard deviation.
     *
     * @param data  a data collection
     * @param mean  mean of the collection
     * @return  standard deviation of the collection
     */
    public static double sd(Collection<? extends Number> data, double mean) {
        double sum = 0.0;
        for (Number d : data)
            sum += Math.pow(d.doubleValue() - mean, 2);

        return Math.sqrt(sum / data.size());
    }

    /**
     * Calculate a sample's standard deviation.
     *
     * @param data a data array
     * @return  sample's standard deviation
     */
    public static double sd(double[] data) {
        return sd(data, mean(data));
    }

    /**
     * Calculate a sample's standard deviation.
     *
     * @param data a data array
     * @param mean mean of the data
     * @return  sample's standard deviation
     */
    public static double sd(double[] data, double mean) {
        return Math.sqrt(var(data, mean));
    }

    public static double sum(double[] data) {
        double sum = 0.0;
        for (int i = 0; i < data.length; i++)
            sum += data[i];

        return sum;
    }

    public static double sum(Collection<? extends Number> data) {
        double sum = 0.0;
        for (Number d : data)
            sum += d.doubleValue();
        return sum;
    }

    public static int sum(int[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++)
            sum += data[i];

        return sum;
    }

    /**
     * The sum from 1 to n.
     *
     * @param n  max of the sum sequence
     * @return  sum of the sequence 1 to n
     */
    public static int sum(int n) {
        return n * (n - 1) / 2;
    }

    /**
     * The sum from 1^2 to n^2, with the largest value to n^3/3
     *
     * @param n  parameter n of the function
     * @return  sum from 1^2 to n^2, with the largest value to n^3/3
     */
    public static double sumSquare(int n) {
        return n * (n + 0.5) * (n + 1) / 3;
    }

    /**
     * Find out the maximum element and its index of an array
     *
     * @param data  a data array
     * @return the maximum element and its index of an array
     */
    public static double[] max(double[] data) {
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < data.length; i++) {
            if (max < data[i]) {
                max = data[i];
                index = i;
            }

        }
        return new double[]{max, index};
    }

    /**
     * Find out the maximum element and its index of an array.
     *
     * @param data a data array
     * @return   the maximum element and its index of an array
     */
    public static int[] max(int[] data) {
        int max = Integer.MIN_VALUE;
        int index = -1;
        for (int i = 0; i < data.length; i++) {
            if (max < data[i]) {
                max = data[i];
                index = i;
            }

        }
        return new int[]{max, index};
    }

    /**
     * Find out the minimum element and its index of an array.
     *
     * @param data  a data array
     * @return  the minimum element and its index of an array.
     */
    public static int[] min(int[] data) {
        int min = Integer.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < data.length; i++) {
            if (min > data[i]) {
                min = data[i];
                index = i;
            }

        }
        return new int[]{min, index};
    }

    /**
     * Find out the minimum element and its index of an array.
     *
     * @param data a data array
     * @return the minimum element and its index of an array.
     */
    public static double[] min(double[] data) {
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < data.length; i++) {
            if (min > data[i]) {
                min = data[i];
                index = i;
            }

        }
        return new double[]{min, index};
    }

}
