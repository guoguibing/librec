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

package librec.util;

/**
 * <h3>Gaussian Distribution</h3>
 * <p>
 * The approximation is accurate to absolute error less than $8 * 10^(-16)$. <br/>
 * Reference paper: George Marsaglia, Evaluating the Normal Distribution.<br/>
 * 
 * http://introcs.cs.princeton.edu/java/21function/ <br>
 * http://www.jstatsoft.org/v11/a04/paper
 * </p>
 * 
 */
public class Gaussian {

	/**
	 * standard Gaussian pdf
	 */
	public static double pdf(double x) {
		return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
	}

	/**
	 * Gaussian pdf with mean mu and stddev sigma
	 */
	public static double pdf(double x, double mu, double sigma) {
		return pdf((x - mu) / sigma) / sigma;
	}

	/**
	 * standard Gaussian cdf using Taylor approximation;
	 * 
	 * @return the probability that a random variable distributed according to the standard normal distribution (mean =
	 *         0 and stdev = 1) produces a value less than z
	 */
	public static double cdf(double z) {
		if (z < -8.0)
			return 0.0;
		if (z > 8.0)
			return 1.0;
		double sum = 0.0, term = z;
		for (int i = 3; sum + term != sum; i += 2) {
			sum = sum + term;
			term = term * z * z / i;
		}
		return 0.5 + sum * pdf(z);
	}

	/**
	 * Gaussian cdf with mean mu and stddev sigma
	 * 
	 * @returnt he probability that a random variable X distributed normally with mean mu and stdev sigma produces a
	 *          value less than z
	 */
	public static double cdf(double z, double mu, double sigma) {
		return cdf((z - mu) / sigma);
	}

	/**
	 * Compute z for standard normal such that cdf(z) = y via bisection search
	 */
	public static double PhiInverse(double y) {
		return PhiInverse(y, .00000001, -8, 8);
	}

	private static double PhiInverse(double y, double delta, double lo, double hi) {
		double mid = lo + (hi - lo) / 2;
		if (hi - lo < delta)
			return mid;
		if (cdf(mid) > y)
			return PhiInverse(y, delta, lo, mid);
		else
			return PhiInverse(y, delta, mid, hi);
	}

	/**
	 * Compute z for standard normal such that cdf(z, mu, sigma) = y via bisection search
	 */
	public static double PhiInverse(double y, double mu, double sigma) {
		return PhiInverse2(y, mu, sigma, .00000001, (mu - 8 * sigma), (mu + 8 * sigma));
	}

	private static double PhiInverse2(double y, double mu, double sigma, double delta, double lo, double hi) {
		double mid = lo + (hi - lo) / 2;
		if (hi - lo < delta)
			return mid;
		if (cdf(mid, mu, sigma) > y)
			return PhiInverse2(y, mu, sigma, delta, lo, mid);
		else
			return PhiInverse2(y, mu, sigma, delta, mid, hi);
	}

	public static void main(String[] args) {

		// This prints out the values of the probability density function for N(2.0.0.6)
		// A graph of this is here: http://www.cs.bu.edu/fac/snyder/cs237/Lecture%20Materials/GaussianExampleJava.png
		double mu = 2.0;
		double sigma = 1.5;
		System.out.println("PDF for N(2.0,0.6) in range [-4..8]:");
		for (double z = -4.0; z <= 8.0; z += 0.2)
			System.out.format("%.1f\t%.4f\n", z, pdf(z, mu, sigma));

		// This prints out the values of the cumulative density function for N(2.0.0.6)
		// A graph of this is here: http://www.cs.bu.edu/fac/snyder/cs237/Lecture%20Materials/GaussianExample2Java.png
		System.out.println("CDF for N(2.0,0.6) in range [-4..8]:");
		for (double z = -4.0; z <= 8.0; z += 0.2)
			System.out.format("%.1f\t%.4f\n", z, cdf(z, mu, sigma));

		// Calculates the probability that for N(2.0, 0.6), the random variable produces a value less than 3.45
		System.out.format("\nIf X ~ N(2.0, 1.5), then P(X <= 3.2) is %.4f\n", cdf(3.2, 2.0, 1.5));

		// Calculates the value x for X ~ N(2.0, 0.6) which is the 78.81% cutoff (i.e., 78.81% of the values lie below x and 21.19% above). 
		System.out.format("\nIf X ~ N(2.0, 1.5), then x such that P(X <= x ) = 0.7881 is %.4f\n",
				PhiInverse(0.7881, 2.0, 1.5));

	}

}
