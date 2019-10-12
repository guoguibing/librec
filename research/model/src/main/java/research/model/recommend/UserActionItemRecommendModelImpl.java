package research.model.recommend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.nd4j.linalg.schedule.ISchedule;
import org.nd4j.linalg.schedule.MapSchedule;
import org.nd4j.linalg.schedule.ScheduleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import research.core.data.Dataset;
import research.core.data.iterator.DataIteratorI;
import research.core.vo.Action;
import research.core.vo.ActionResult;
import research.core.vo.Item;
import research.core.vo.Label;
import research.core.vo.User;
import research.data.iterator.UserActionItemIterator;
import research.model.eval.RatingEvaluator;

/**
 * 用户-行为-物品推荐模型
 * 
 * @author liweigu714@163.com
 *
 */
public class UserActionItemRecommendModelImpl extends BaseRecommendModel {
	private static Logger LOG = LoggerFactory.getLogger(UserActionItemRecommendModelImpl.class);

	/**
	 * 初始化模型
	 * 
	 * @param initProps 初始化参数
	 * @param pretrainedModelPath 预训练模型路径
	 */
	public void initModel(Map<String, Object> initProps, String pretrainedModelPath) {
		if (pretrainedModelPath == null || pretrainedModelPath.length() == 0) {
			throw new IllegalArgumentException("pretrainedModelPath为空");
		}
		if (initProps == null || !initProps.containsKey("inputSize")) {
			throw new IllegalArgumentException("initProps缺少属性：inputSize");
		}
		InitProps = initProps;

		this.restore(pretrainedModelPath);
	}

	/**
	 * 初始化模型
	 * 
	 * @param initProps 初始值
	 */
	public void initModel(Map<String, Object> initProps) {
		// 输入参数的维度
		int featureDimenion;
		if (initProps != null && initProps.containsKey("inputSize")) {
			featureDimenion = (int) initProps.get("inputSize");
		} else {
			throw new IllegalArgumentException("initProps缺少属性：inputSize");
		}
		// 输入参数的维度, 默认值是1
		int labelDimenion = 1;
		if (initProps != null && initProps.containsKey("outputSize")) {
			labelDimenion = (int) initProps.get("outputSize");
		}
		InitProps = initProps;

		if (ComputationGraph == null) {
			String strategy = null;
			if (initProps.containsKey("strategy")) {
				strategy = (String) initProps.get("strategy");
			}

			GraphBuilder graphBuilder = getGraphBuilder(featureDimenion, labelDimenion, strategy);

			ComputationGraph = new ComputationGraph(graphBuilder.build());
			ComputationGraph.init();

			int listenerFrequency = 1;
			// 使用UIServer可以在浏览器查看score等监控值
			boolean useUIServer = true;
			LOG.info("useUIServer = " + useUIServer);
			if (useUIServer) {
				UIServer uiServer = UIServer.getInstance();
				StatsStorage memoryStatsStorage = new InMemoryStatsStorage();
				uiServer.attach(memoryStatsStorage);
				ComputationGraph.setListeners(new StatsListener(memoryStatsStorage, listenerFrequency), new ScoreIterationListener(listenerFrequency));
			} else {
				ComputationGraph.setListeners(new ScoreIterationListener(listenerFrequency));
			}

			LOG.info(ComputationGraph.summary());
		}
	}

	private GraphBuilder getGraphBuilder(int featureDimenion, int labelDimenion, String strategy) {
		GraphBuilder graphBuilder = null;

		if (strategy == null || "score".equals(strategy)) {
			double learningRate = 1e-4;
			LOG.info("learningRate = " + learningRate);
			Map<Integer, Double> lrSchedule = new HashMap<Integer, Double>();
			lrSchedule.put(0, learningRate);
			lrSchedule.put(1000, learningRate / 2);
			lrSchedule.put(3000, learningRate / 4);
			LOG.info("lrSchedule = " + lrSchedule);
			ISchedule mapSchedule = new MapSchedule(ScheduleType.ITERATION, lrSchedule);

			double l2 = 0;

			NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
			builder.seed(1234);
			builder.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT);
			builder.weightInit(WeightInit.XAVIER);
			if (l2 > 0) {
				LOG.info("l2 = " + l2);
				builder.l2(l2);
			}
			builder.updater(new Adam(mapSchedule));

			String[] inputs = new String[] { "input" };
			graphBuilder = builder.graphBuilder().backpropType(BackpropType.Standard).addInputs(inputs).setOutputs("output");
			int hidden = 512;
			graphBuilder.addLayer("layer1",
					new DenseLayer.Builder().nIn(featureDimenion).nOut(hidden).activation(Activation.TANH).updater(new Adam(mapSchedule)).build(), "input");
			graphBuilder.addLayer("layer2",
					new DenseLayer.Builder().nIn(hidden).nOut(hidden / 2).activation(Activation.TANH).updater(new Adam(mapSchedule)).build(), "layer1");
			graphBuilder.addLayer("layer3",
					new DenseLayer.Builder().nIn(hidden / 2).nOut(hidden / 4).activation(Activation.TANH).updater(new Adam(mapSchedule)).build(), "layer2");
			graphBuilder.addLayer("layer4",
					new DenseLayer.Builder().nIn(hidden / 4).nOut(hidden / 8).activation(Activation.TANH).updater(new Adam(mapSchedule)).build(), "layer3");
			graphBuilder.addLayer("layer5",
					new DenseLayer.Builder().nIn(hidden / 8).nOut(hidden / 16).activation(Activation.TANH).updater(new Adam(mapSchedule)).build(), "layer4");
			graphBuilder = graphBuilder.addLayer("output",
					new OutputLayer.Builder(LossFunction.MSE).nIn(hidden / 16).nOut(1).updater(new Adam(mapSchedule)).activation(Activation.IDENTITY).build(),
					"layer5");
		} else if ("itemid".equals(strategy) || "multiclass".equals(strategy)) {
			double baseLr = 5e-4;
			LOG.info("baseLr = " + baseLr);
			Map<Integer, Double> lrSchedule = new HashMap<Integer, Double>();
			lrSchedule = new HashMap<Integer, Double>();
			lrSchedule.put(0, 1 * baseLr);
			lrSchedule.put(10000, baseLr / 2);
			ISchedule mapSchedule = new MapSchedule(ScheduleType.ITERATION, lrSchedule);

			NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
			builder.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).weightInit(WeightInit.RELU).seed(1234).updater(new Adam(mapSchedule));

			String[] inputs = new String[] { "input" };
			graphBuilder = builder.graphBuilder().backpropType(BackpropType.Standard).addInputs(inputs).setOutputs("output");

			// 1024 -> 512 -> 256 -> 128, batch=128, epoch=74 --> 0.6263
			int hidden = 1024; // 512, 1024, 2048
			graphBuilder.addLayer("layer1",
					new DenseLayer.Builder().nIn(featureDimenion).nOut(hidden).activation(Activation.RELU).updater(new Adam(mapSchedule)).build(), "input");
			graphBuilder.addLayer("layer2",
					new DenseLayer.Builder().nIn(hidden).nOut(hidden / 2).activation(Activation.RELU).updater(new Adam(mapSchedule)).build(), "layer1");
			graphBuilder.addLayer("layer3",
					new DenseLayer.Builder().nIn(hidden / 2).nOut(hidden / 4).activation(Activation.RELU).updater(new Adam(mapSchedule)).build(), "layer2");
			graphBuilder.addLayer("layer4",
					new DenseLayer.Builder().nIn(hidden / 4).nOut(hidden / 8).activation(Activation.RELU).updater(new Adam(mapSchedule)).build(), "layer3");
			// graphBuilder.addLayer("layer5",
			// new DenseLayer.Builder().nIn(hidden / 8).nOut(hidden / 16).activation(Activation.RELU).updater(new Adam(mapSchedule)).build(), "layer4");
			OutputLayer outputLayer = null;
			if ("itemid".equals(strategy)) {
				outputLayer = new OutputLayer.Builder(LossFunction.MSE).nIn(hidden / 8).nOut(1).updater(new Adam(mapSchedule)).activation(Activation.RELU)
						.build();
			} else if ("multiclass".equals(strategy)) {
				// 多分类输出
				outputLayer = new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).nIn(hidden / 8).nOut(labelDimenion).updater(new Adam(mapSchedule))
						.activation(Activation.SOFTMAX).build();
			}
			graphBuilder = graphBuilder.addLayer("output", outputLayer, "layer4");
		} else {
			throw new IllegalArgumentException("不支持的strategy:" + strategy);
		}

		return graphBuilder;
	}

	/**
	 * 训练
	 * 
	 * @param ratings 评分列表
	 */
	public void fit(List<ActionResult> actionResults) {
		List<List<Double>> features = new ArrayList<List<Double>>();
		List<List<Double>> labels = new ArrayList<List<Double>>();
		for (ActionResult actionResult : actionResults) {
			User user = actionResult.getUser();
			Action action = actionResult.getAction();
			Item item = actionResult.getItem();
			Label label = actionResult.getLabel();
			List<Double> feature = new ArrayList<Double>();
			feature.addAll(this.getUserFeatures(user));
			feature.addAll(action.getDoubleValue());
			feature.addAll(this.getItemFeatures(item));
			features.add(feature);
			labels.add(this.getLabelFeatures(label));
		}
		LOG.debug("features.size() = " + features.size());
		LOG.debug("features.get(0).size() = " + features.get(0).size());
		this.fit(features, labels);
	}

	private List<Double> getLabelFeatures(Label label) {
		List<Double> labelFeatures;
		if (label == null) {
			// 默认用户特征
			labelFeatures = new ArrayList<Double>();
			labelFeatures.add(0.);
		} else {
			labelFeatures = label.getDoubleValue();
		}
		return labelFeatures;
	}

	private List<Double> getUserFeatures(User user) {
		List<Double> userFeatures;
		if (user == null) {
			// 默认用户特征
			userFeatures = new ArrayList<Double>();
			for (int i = 0; i < 66; i++) {
				userFeatures.add(0.);
			}
		} else {
			userFeatures = user.getDoubleValue();
		}
		return userFeatures;
	}

	private List<Double> getItemFeatures(Item item) {
		List<Double> itemFeatures;
		if (item == null) {
			// 默认物品特征
			itemFeatures = new ArrayList<Double>();
			for (int i = 0; i < 6; i++) {
				itemFeatures.add(0.);
			}
		} else {
			itemFeatures = item.getDoubleValue();
		}
		return itemFeatures;
	}

	/**
	 * 训练
	 * 
	 * @param dataIterator 数据迭代器
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void fit(DataIteratorI dataIterator) {
		if (dataIterator instanceof UserActionItemIterator) {
			UserActionItemIterator userActionItemIterator = (UserActionItemIterator) dataIterator;
			List<ActionResult> actionResults = (List<ActionResult>) userActionItemIterator.next(userActionItemIterator.batch());
			LOG.debug("actionResults.size() = " + actionResults.size());
			while (actionResults.size() > 0) {
				this.fit(actionResults);
				actionResults = (List<ActionResult>) userActionItemIterator.next(userActionItemIterator.batch());
				LOG.debug("actionResults.size() = " + actionResults.size());
			}
		} else {
			throw new IllegalArgumentException("dataIterator不是UserActionItemIterator类型");
		}
	}

	/**
	 * 训练
	 * 
	 * @param features 特征值
	 * @param labels 标签值
	 */
	@Override
	public void fit(List<List<Double>> features, List<List<Double>> labels) {
		ComputationGraph computationGraph = (ComputationGraph) this.getModel();

		DataSet trainDataSet = getDataSet(features, labels);
		computationGraph.fit(trainDataSet);
	}

	/**
	 * 将数值转为DataSet
	 * 
	 * @param features 特征值
	 * @param labels 标签值
	 * @return DataSet 数据集
	 */
	private DataSet getDataSet(List<List<Double>> features, List<List<Double>> labels) {
		DataSet dataSet = null;
		Map<String, Object> initProps = this.getInitProps();
		if (initProps != null) {
			int inputSize = (int) initProps.get("inputSize");
			int outputSize = 1;
			int batchSize = features.size();
			double[] featuresData = new double[inputSize * batchSize];
			for (int i = 0; i < batchSize; i++) {
				for (int j = 0; j < inputSize; j++) {
					if (i >= features.size()) {
						LOG.warn("i = " + i + ", features.size() = " + features.size());
					}
					List<Double> feature = features.get(i);
					if (j >= feature.size()) {
						LOG.warn("j = " + j + ", feature.size() = " + feature.size());
					}
					double value = feature.get(j);
					featuresData[i + j * batchSize] = value;
				}
			}
			INDArray featuresINDArray = Nd4j.create(featuresData, new int[] { batchSize, inputSize });

			INDArray labelsINDArray = null;
			if (labels != null) {
				double[] labelsData = new double[batchSize];
				for (int i = 0; i < batchSize; i++) {
					labelsData[i] = labels.get(i).get(0);
				}
				labelsINDArray = Nd4j.create(labelsData, new int[] { batchSize, outputSize });
			}

			dataSet = new DataSet(featuresINDArray, labelsINDArray);
		}
		return dataSet;
	}

	/**
	 * 训练
	 * 
	 * @param dataFrame 数据框架
	 */
	@Override
	public void fit(Dataset dataFrame) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 预测
	 * 
	 * @param features 特征值
	 * @return 预测结果值
	 */
	@Override
	public List<List<Double>> output(List<List<Double>> features) {
		List<List<Double>> result = new ArrayList<List<Double>>();

		ComputationGraph computationGraph = (ComputationGraph) this.getModel();

		DataSet dataSet = this.getDataSet(features, null);
		INDArray featuresINDArray = dataSet.getFeatures();

		for (int i = 0; i < featuresINDArray.rows(); i++) {
			INDArray featureINDArray = featuresINDArray.getRow(i);
			INDArray[] outputs = computationGraph.output(featureINDArray);
			// LOG.info("outputs.length = " + outputs.length);
			double value = outputs[0].getDouble(0);
			result.add(Collections.singletonList(value));
		}

		return result;
	}

	/**
	 * 预测
	 * 
	 * @param actionResult 行为结果
	 * @return 预测结果值
	 */
	public List<List<Double>> output(ActionResult actionResult) {
		List<List<Double>> features = new ArrayList<List<Double>>();
		User user = actionResult.getUser();
		Action action = actionResult.getAction();
		Item item = actionResult.getItem();

		List<Double> feature = new ArrayList<Double>();
		feature.addAll(this.getUserFeatures(user));
		feature.addAll(action.getDoubleValue());
		feature.addAll(this.getItemFeatures(item));
		features.add(feature);

		return this.output(features);
	}

	/**
	 * 结果评估
	 * 
	 * @param actionResults 行为结果
	 */
	public void evaluate(List<ActionResult> actionResults) {
		List<List<Double>> features = new ArrayList<List<Double>>();
		List<List<Double>> labels = new ArrayList<List<Double>>();
		for (ActionResult actionResult : actionResults) {
			User user = actionResult.getUser();
			Action action = actionResult.getAction();
			Item item = actionResult.getItem();
			Label label = actionResult.getLabel();

			List<Double> feature = new ArrayList<Double>();
			feature.addAll(this.getUserFeatures(user));
			feature.addAll(action.getDoubleValue());
			feature.addAll(this.getItemFeatures(item));
			features.add(feature);
			labels.add(this.getLabelFeatures(label));
		}
		this.evaluate(features, labels);
	}

	/**
	 * 结果评估
	 * 
	 * @param features 特征值
	 * @param labels 标签值
	 */
	@Override
	public void evaluate(List<List<Double>> features, List<List<Double>> labels) {
		List<List<Double>> predictedResults = this.output(features);
		LOG.info("predictedResults.size() = " + predictedResults.size());
		Map<String, Double> evalResults = RatingEvaluator.eval(predictedResults, labels, null);
		LOG.info("mae = " + evalResults.get("mae") + ", mse = " + evalResults.get("mse") + ", rmse = " + evalResults.get("rmse"));
	}

}
