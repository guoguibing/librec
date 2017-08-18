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

public class Gamma {

    private final static double small = 1e-6;
    private final static double large = 9.5;
    private final static double d1 = -0.5772156649015328606065121; // digamma(1)
    private final static double d2 = Math.pow(Math.PI, 2.0) / 6.0;

    private final static double s3 = 1.0 / 12.0;
    private final static double s4 = 1.0 / 120.0;
    private final static double s5 = 1.0 / 252.0;
    private final static double s6 = 1.0 / 240.0;
    private final static double s7 = 1.0 / 132.0;

    /**
     * log Gamma function: log(gamma(alpha)) for alpha bigger than 0, accurate to 10 decimal places<br>
     * <p>
     * Reference: Pike MC &amp; Hill ID (1966) Algorithm 291: Logarithm of the gamma function. Communications of the
     * Association for Computing Machinery, 9:684
     *
     * @param x  parameter of the gamma function
     * @return   the log of the gamma function of the given alpha
     */
    public static double logGamma(double x) {
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1) + 24.01409822 / (x + 2) - 1.231739516
                / (x + 3) + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    /**
     * The Gamma function is defined by: <br>
     * <p>
     * Gamma(x) = integral( t^(x-1) e^(-t), t = 0 .. infinity) <br>
     * <p>
     * Uses Lanczos approximation formula.
     *
     * @param x  parameter of the gamma function
     * @return   gamma function of the given alpha
     */
    public static double gamma(double x) {
        return Math.exp(logGamma(x));
    }

    /**
     * digamma(x) = d log Gamma(x)/ dx
     *
     * @param x  parameter of the gamma function
     * @return   derivative of the gamma function
     */
    public static double digamma(double x) {
        double y = 0.0;
        double r = 0.0;

        if (Double.isInfinite(x) || Double.isNaN(x)) {
            return 0.0 / 0.0;
        }

        if (x == 0.0) {
            return -1.0 / 0.0;
        }

        if (x < 0.0) {
            y = gamma(-x + 1) + Math.PI * (1.0 / Math.tan(-Math.PI * x));
            return y;
        }

        // Use approximation if argument <= small.
        if (x <= small) {
            y = y + d1 - 1.0 / x + d2 * x;
            return y;
        }

        // Reduce to digamma(X + N) where (X + N) >= large.
        while (true) {
            if (x > small && x < large) {
                y = y - 1.0 / x;
                x = x + 1.0;
            } else {
                break;
            }
        }

        // Use de Moivre's expansion if argument >= large.
        // In maple: asympt(Psi(x), x);
        if (x >= large) {
            r = 1.0 / x;
            y = y + Math.log(x) - 0.5 * r;
            r = r * r;
            y = y - r * (s3 - r * (s4 - r * (s5 - r * (s6 - r * s7))));
        }

        return y;
    }

    /**
     * Newton iteration to solve digamma(x)-y = 0.
     *
     * @param y  parameter y in the function above
     * @return the inverse function of digamma, i.e., returns x such that digamma(x) = y adapted from Tony Minka fastfit
     * Matlab code
     */
    public static double invDigamma(double y) {
        // Newton iteration to solve digamma(x)-y = 0
        return y < -2.22 ? (-1.0 / (y - digamma(1))) : (Math.exp(y) + 0.5);
    }

}
