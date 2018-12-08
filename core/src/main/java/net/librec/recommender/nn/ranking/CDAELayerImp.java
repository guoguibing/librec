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
package net.librec.recommender.nn.ranking;

import org.deeplearning4j.exception.DL4JInvalidInputException;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.util.Dropout;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.Arrays;

/**
 * Yao et al., <strong>Collaborative Denoising Auto-Encoders for Top-N Recommender Systems, WSDM 2016.
 *
 * @author Ma Chen
 */
public class CDAELayerImp extends BaseLayer<CDAELayer> {

    public CDAELayerImp(NeuralNetConfiguration conf) {
        super(conf);
    }

    @Override
    public INDArray preOutput(INDArray x, boolean training) {
        if (x == null) {
            throw new IllegalArgumentException("No null input allowed");
        } else {
            this.setInput(x);
            return this.preOutput(training);
        }
    }

    @Override
    public INDArray preOutput(boolean training) {
        /*
        The preOut method(s) calculate the activations (forward pass), before the activation function is applied.

        Because we aren't doing anything different to a standard dense layer, we can use the existing implementation
        for this. Other network types (RNNs, CNNs etc) will require you to implement this method.

        For custom layers, you may also have to implement methods such as calcL1, calcL2, numParams, etc.
         */
        applyDropOutIfNecessary(training);
        INDArray b = getParam(CDAEParamInitializer.BIAS_KEY);
        INDArray W = getParam(CDAEParamInitializer.WEIGHT_KEY);
        INDArray U = getParam(CDAEParamInitializer.USER_WEIGHT_KEY);

        //Input validation:
        if (input.rank() != 2 || input.columns() != W.rows()) {
            if (input.rank() != 2) {
                throw new DL4JInvalidInputException("Input that is not a matrix; expected matrix (rank 2), got rank "
                        + input.rank() + " array with shape " + Arrays.toString(input.shape())
                        + ". Missing preprocessor or wrong input type? " + layerId());
            }
            throw new DL4JInvalidInputException(
                    "Input cardinality (" + input.columns() + " columns; shape = " + Arrays.toString(input.shape())
                            + ") is invalid: does not match layer input cardinality (layer # inputs = "
                            + W.size(0) + ") " + layerId());
        }

        if (conf.isUseDropConnect() && training && conf.getLayer().getDropOut() > 0) {
            W = Dropout.applyDropConnect(this, CDAEParamInitializer.WEIGHT_KEY);
        }

        // modified preOut: WX + V + b
        INDArray ret = input.mmul(W).addi(U).addiRowVector(b);

        if (maskArray != null) {
            applyMask(ret);
        }

        return ret;
    }


    @Override
    public INDArray activate(boolean training) {
        return super.activate(training);
    }

    @Override
    public boolean isPretrainLayer() {
        return false;
    }


    @Override
    public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon) {
        //If this layer is layer L, then epsilon is (w^(L+1)*(d^(L+1))^T) (or equivalent)
        INDArray z = this.preOutput(true); //Note: using preOutput(INDArray) can't be used as this does a setInput(input) and resets the 'appliedDropout' flag
        //INDArray activationDerivative = Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(conf().getLayer().getActivationFunction(), z).derivative());
        //        INDArray activationDerivative = conf().getLayer().getActivationFn().getGradient(z);
        //        INDArray delta = epsilon.muli(activationDerivative);
        INDArray delta = layerConf().getActivationFn().backprop(z, epsilon).getFirst(); //TODO handle activation function params

        if (maskArray != null) {
            applyMask(delta);
        }

        Gradient ret = new DefaultGradient();

        INDArray weightGrad = gradientViews.get(CDAEParamInitializer.WEIGHT_KEY); //f order
        Nd4j.gemm(input, delta, weightGrad, true, false, 1.0, 0.0);
        INDArray userWeightGrad = gradientViews.get(CDAEParamInitializer.USER_WEIGHT_KEY); //f order
        userWeightGrad.assign(delta);
        INDArray biasGrad = gradientViews.get(CDAEParamInitializer.BIAS_KEY);
        biasGrad.assign(delta.sum(0)); //biasGrad is initialized/zeroed first

        ret.gradientForVariable().put(CDAEParamInitializer.WEIGHT_KEY, weightGrad);
        ret.gradientForVariable().put(CDAEParamInitializer.BIAS_KEY, biasGrad);
        ret.gradientForVariable().put(CDAEParamInitializer.USER_WEIGHT_KEY, userWeightGrad);

        INDArray epsilonNext = params.get(CDAEParamInitializer.WEIGHT_KEY).mmul(delta.transpose()).transpose();
        //epsilonNext = null;
        return new Pair<>(ret, epsilonNext);
    }

    @Override
    public double calcL2(boolean backpropParamsOnly) {
        if (!this.conf.isUseRegularization()) {
            return 0.0D;
        } else {
            double l2Sum = 0.0D;
            double l2Norm;
            if (this.conf.getL2ByParam("W") > 0.0D) {
                l2Norm = this.getParam("W").norm2Number().doubleValue();
                l2Sum += 0.5D * this.conf.getL2ByParam("W") * l2Norm * l2Norm;
            }

            if (this.conf.getL2ByParam("uw") > 0.0D) {
                l2Norm = this.getParam("uw").norm2Number().doubleValue();
                l2Sum += 0.5D * this.conf.getL2ByParam("uw") * l2Norm * l2Norm;
            }

            if (this.conf.getL2ByParam("b") > 0.0D) {
                l2Norm = this.getParam("b").norm2Number().doubleValue();
                l2Sum += 0.5D * this.conf.getL2ByParam("b") * l2Norm * l2Norm;
            }

            return l2Sum;
        }
    }
}
