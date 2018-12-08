/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a clone of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
Copyright 1999 CERN - European Organization for Nuclear Research.
Permission to use, clone, modify, distribute and sell this software and its documentation for any purpose
is hereby granted without fee, provided that the above copyright notice appear in all copies and
that both that copyright notice and this permission notice appear in supporting documentation.
CERN makes no representations about the suitability of this software for any purpose.
It is provided "as is" without expressed or implied warranty.
*/

package net.librec.math.function;

/**
 * Interface that represents a function object: a function that takes two arguments and returns a single value.
 **/
public abstract class DoubleDoubleFunction {

    /**
     * Apply the function to the arguments and return the result
     *
     * @param arg1 a double for the first argument
     * @param arg2 a double for the second argument
     * @return the result of applying the function
     */
    public abstract double apply(double arg1, double arg2);

    /**
     * @return true iff f(x, 0) = x for any x
     */
    public boolean isLikeRightPlus() {
        return false;
    }

    /**
     * @return true iff f(0, y) = 0 for any y
     */
    public boolean isLikeLeftMult() {
        return false;
    }

    /**
     * @return true iff f(x, 0) = 0 for any x
     */
    public boolean isLikeRightMult() {
        return false;
    }

    /**
     * @return true iff f(x, 0) = f(0, y) = 0 for any x, y
     */
    public boolean isLikeMult() {
        return isLikeLeftMult() && isLikeRightMult();
    }

    /**
     * @return true iff f(x, y) = f(y, x) for any x, y
     */
    public boolean isCommutative() {
        return false;
    }

    /**
     * @return true iff f(x, f(y, z)) = f(f(x, y), z) for any x, y, z
     */
    public boolean isAssociative() {
        return false;
    }

    /**
     * @return true iff f(x, y) = f(y, x) for any x, y AND f(x, f(y, z)) = f(f(x, y), z) for any x, y, z
     */
    public boolean isAssociativeAndCommutative() {
        return isAssociative() && isCommutative();
    }

    /**
     * @return true iff f(0, 0) != 0
     */
    public boolean isDensifying() {
        return apply(0.0, 0.0) != 0.0;
    }
}
