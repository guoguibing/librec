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

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.Map;

/**
 * Yao et al., <strong>Collaborative Denoising Auto-Encoders for Top-N Recommender Systems, WSDM 2016.
 *
 * @author Ma Chen
 */
public class CDAELayer extends FeedForwardLayer {
    private int numUsers = 0;

    public CDAELayer() {
        //We need a no-arg constructor so we can deserialize the configuration from JSON or YAML format
        // Without this, you will likely get an exception like the following:
        //com.fasterxml.jackson.databind.JsonMappingException: No suitable constructor found for type [simple type, class org.deeplearning4j.examples.misc.customlayers.layer.CustomLayer]: can not instantiate from JSON object (missing default constructor or creator, or perhaps need to plus/enable type information?)
    }

    private CDAELayer(Builder builder) {
        super(builder);
        this.numUsers = builder.numUsers;
    }

    @Override
    public Layer instantiate(NeuralNetConfiguration conf, Collection<IterationListener> iterationListeners,
                             int layerIndex, INDArray layerParamsView, boolean initializeParams) {
        //The instantiate method is how we go from the configuration class (i.e., this class) to the implementation class
        // (i.e., a CustomLayerImpl instance)
        //For the most part, it's the same for each type of layer

        CDAELayerImp myCustomLayer = new CDAELayerImp(conf);
        myCustomLayer.setListeners(iterationListeners);             //Set the iteration listeners, if any
        myCustomLayer.setIndex(layerIndex);                         //Integer index of the layer

        //Parameter view array: In Deeplearning4j, the network parameters for the entire network (all layers) are
        // allocated in one big array. The relevant section of this parameter vector is extracted out for each layer,
        // (i.e., it's a "view" array in that it's a subset of a larger array)
        // This is a row vector, with length equal to the number of parameters in the layer
        myCustomLayer.setParamsViewArray(layerParamsView);

        //Initialize the layer parameters. For example,
        // Note that the entries in paramTable (2 entries here: a weight array of shape [nIn,nOut] and biases of shape [1,nOut]
        // are in turn a view of the 'layerParamsView' array.
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        myCustomLayer.setParamTable(paramTable);
        myCustomLayer.setConf(conf);
        return myCustomLayer;
    }

    @Override
    public ParamInitializer initializer() {
        //This method returns the parameter initializer for this type of layer
        //In this case, we can use the DefaultParamInitializer, which is the same one used for DenseLayer
        //For more complex layers, you may need to implement a custom parameter initializer
        //See the various parameter initializers here:
        //https://github.com/deeplearning4j/deeplearning4j/tree/master/deeplearning4j-core/src/main/java/org/deeplearning4j/nn/params
        CDAEParamInitializer.numUsers = this.numUsers;
        return CDAEParamInitializer.getInstance();
    }

    @Override
    public double getL1ByParam(String paramName) {
        switch (paramName) {
            case CDAEParamInitializer.WEIGHT_KEY:
                return l1;
            case CDAEParamInitializer.BIAS_KEY:
                return l1Bias;
            case CDAEParamInitializer.USER_WEIGHT_KEY:
                return l1;
            default:
                throw new IllegalArgumentException("Unknown parameter name: \"" + paramName + "\"");
        }
    }

    @Override
    public double getL2ByParam(String paramName) {
        switch (paramName) {
            case CDAEParamInitializer.WEIGHT_KEY:
                return l2;
            case CDAEParamInitializer.BIAS_KEY:
                return l2Bias;
            case CDAEParamInitializer.USER_WEIGHT_KEY:
                return l2;
            default:
                throw new IllegalArgumentException("Unknown parameter name: \"" + paramName + "\"");
        }
    }

    @Override
    public double getLearningRateByParam(String paramName) {
        switch (paramName) {
            case CDAEParamInitializer.WEIGHT_KEY:
                return learningRate;
            case CDAEParamInitializer.BIAS_KEY:
                if (!Double.isNaN(biasLearningRate)) {
                    //Bias learning rate has been explicitly set
                    return biasLearningRate;
                } else {
                    return learningRate;
                }
            case CDAEParamInitializer.USER_WEIGHT_KEY:
                return learningRate;
            default:
                throw new IllegalArgumentException("Unknown parameter name: \"" + paramName + "\"");
        }
    }


    //Here's an implementation of a builder pattern, to allow us to easily configure the layer
    //Note that we are inheriting all of the FeedForwardLayer.Builder options: things like n
    public static class Builder extends FeedForwardLayer.Builder<Builder> {
        private int numUsers;

        @Override
        @SuppressWarnings("unchecked")  //To stop warnings about unchecked cast. Not required.
        public CDAELayer build() {
            return new CDAELayer(this);
        }

        public Builder setNumUsers(int numUsers) {
            this.numUsers = numUsers;
            return this;
        }
    }

    public LayerMemoryReport getMemoryReport(InputType inputType) {
        return null;
    }
}
