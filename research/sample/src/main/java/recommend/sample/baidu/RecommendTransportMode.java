package recommend.sample.baidu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import research.core.data.iterator.DataIteratorI;
import research.core.vo.Action;
import research.core.vo.ActionResult;
import research.data.iterator.UserActionItemIterator;
import research.data.reader.FileTool;
import research.model.recommend.UserActionItemRecommendModelImpl;

/**
 * https://dianshi.baidu.com/competition/29/rule
 * 
 * @author liweigu714@163.com
 *
 */
public class RecommendTransportMode {
	private static Logger LOG = LoggerFactory.getLogger(RecommendTransportMode.class);
	static String ModelName = "transport.model";
	static String Strategy = "multiclass"; // score, itemid, multiclass
	static String UseSpark = null;
	static int BatchSize = 128;
	static int InputSize;
	static int OutputSize;

	public static void main(String[] args) throws IOException {
		String basePath = "E:/data/ai/baidu/data_set_phase1/";

		if (args.length > 0) {
			if (args[0].equals("linux")) {
				basePath = "/root/data/data_set_phase1/";
			}
			if (args.length > 1) {
				if (args[1].equals("score") || args[1].equals("itemid") || args[1].equals("multiclass")) {
					Strategy = args[1];
				}
				if (args.length > 2) {
					if (args[2].equals("true") || args[2].equals("false")) {
						UseSpark = args[2];
					}
					if (args.length > 3) {
						BatchSize = Integer.parseInt(args[3]);
					}
				}
			}
		}
		if (Strategy == null || "score".equals(Strategy)) {
			InputSize = 72;
			OutputSize = 1;
		} else if ("itemid".equals(Strategy)) {
			InputSize = 76;
			OutputSize = 1;
		} else if ("multiclass".equals(Strategy)) {
			InputSize = 76;
			OutputSize = 13;
		}
		LOG.info("Strategy = " + Strategy + ", UseSpark = " + UseSpark + ", InputSize = " + InputSize);
		if (new File(basePath + ModelName).exists()) {
			test(basePath);
		} else {
			train(basePath);
		}
	}

	public static void train(String basePath) throws IOException {
		LOG.info("train.");
		// 训练
		UserActionItemRecommendModelImpl userActionItemRecommendModel = new UserActionItemRecommendModelImpl();
		Map<String, Object> modelInitProps = new HashMap<String, Object>();
		modelInitProps.put("inputSize", InputSize);
		modelInitProps.put("outputSize", OutputSize);
		modelInitProps.put("strategy", Strategy);
		boolean usePretrainedModel = false;
		if (usePretrainedModel) {
			// 本地路径
			String pretrainedModelPath = basePath + ModelName;
			// 远程路径
			pretrainedModelPath = "http://model.liweigu.top/model/xxx.model";
			userActionItemRecommendModel.initModel(modelInitProps, pretrainedModelPath);
		} else {
			userActionItemRecommendModel.initModel(modelInitProps);

			int epoch = 100; // 1
			Map<String, Object> trainIteratorInitProps = new HashMap<String, Object>();
			trainIteratorInitProps.put("userDataPath", basePath + "profiles.csv");
			trainIteratorInitProps.put("actionDataPath", basePath + "train_queries.csv");
			trainIteratorInitProps.put("itemDataPath", basePath + "train_plans.csv");
			trainIteratorInitProps.put("actionResultDataPath", basePath + "train_clicks.csv");
			trainIteratorInitProps.put("strategy", Strategy);
			trainIteratorInitProps.put("outputSize", OutputSize);
			if (UseSpark != null) {
				trainIteratorInitProps.put("useSpark", UseSpark);
			}
			for (int epochIndex = 0; epochIndex < epoch; epochIndex++) {
				if (epochIndex % 10 == 0) {
					LOG.info("epochIndex = " + epochIndex);
				}
				DataIteratorI trainDataIterator = new UserActionItemIterator(BatchSize, trainIteratorInitProps);
				LOG.debug("before fit");
				userActionItemRecommendModel.fit(trainDataIterator);
				LOG.debug("after fit");

				if (epochIndex < epoch - 1) {
					String filePath = basePath + ModelName + "." + epochIndex;
					LOG.info("saving model to " + filePath);
					userActionItemRecommendModel.save(filePath);
				}
			}
			String filePath = basePath + ModelName;
			LOG.info("saving model to " + filePath);
			userActionItemRecommendModel.save(filePath);
		}

		// // 验证与评估
		// // 读取全部验证数据
		// LOG.info("evaluating...");
		// int evalCount = 100;
		// List<ActionResult> actionResults =
		// UserActionItemReader.readActionResults(basePath + "profiles.csv",
		// basePath + "test_queries.csv", basePath + "test_plans.csv", null, 0,
		// evalCount);
		// LOG.info("actionResults.size() = " + actionResults.size());
	}

	/**
	 * 输入测试结果
	 * 
	 * @param basePath
	 */
	private static void test(String basePath) {
		LOG.info("test");
		UserActionItemRecommendModelImpl userActionItemRecommendModel = new UserActionItemRecommendModelImpl();
		userActionItemRecommendModel.restore(basePath + ModelName);
		Map<String, Object> modelInitProps = new HashMap<String, Object>();
		modelInitProps.put("inputSize", InputSize);
		modelInitProps.put("strategy", Strategy);
		userActionItemRecommendModel.initModel(modelInitProps);

		// 测试
		// TODO 根据不同策略（strategy）进行测试；当前代码是score策略的。
		Map<String, Object> testIteratorInitProps = new HashMap<String, Object>();
		testIteratorInitProps.put("userDataPath", basePath + "profiles.csv");
		testIteratorInitProps.put("actionDataPath", basePath + "test_queries.csv");
		testIteratorInitProps.put("itemDataPath", basePath + "test_plans.csv");
		testIteratorInitProps.put("strategy", Strategy);
		if (UseSpark != null) {
			testIteratorInitProps.put("useSpark", UseSpark);
		}
		int batchSize = 12;
		DataIteratorI testDataIterator = new UserActionItemIterator(batchSize, testIteratorInitProps);
		List<ActionResult> actionResults = (List<ActionResult>) testDataIterator.next(batchSize);
		Map<Integer, Double> predictedResults = new HashMap<Integer, Double>();
		int index = 0;
		while (actionResults.size() > 0) {
			++index;
			if (index % 1000 == 0) {
				LOG.info("(test) index = " + index);
			}
			double maxScore = Double.MIN_VALUE;
			double bestTransportMode = 0;
			int sid = 0;
			// 每一批数据的sid应该是相同的
			for (ActionResult actionResult : actionResults) {
				Action action = actionResult.getAction();
				sid = actionResult.getAction().getSid();
				List<List<Double>> outputs = userActionItemRecommendModel.output(actionResult);
				double predictedScore = outputs.get(0).get(0);
				if (predictedScore > maxScore) {
					maxScore = predictedScore;
					double itemTransportMode = 0;
					if (actionResult.getItem() != null) {
						actionResult.getItem().getExt().get("transportMode").get(0);
					}
					bestTransportMode = itemTransportMode;
				}
			}
			if (sid > 0) {
				predictedResults.put(sid, bestTransportMode);
			}

			actionResults = (List<ActionResult>) testDataIterator.next(batchSize);
		}

		List<String> outputLines = new ArrayList<String>();
		outputLines.add("\"sid\",\"recommend_mode\"");
		for (Entry<Integer, Double> entry : predictedResults.entrySet()) {
			int sid = entry.getKey();
			int recommend_mode = entry.getValue().intValue();
			outputLines.add("\"" + sid + "\",\"" + recommend_mode + "\"");
		}
		// 输出到文件
		FileTool.writeFile(basePath + "submission.csv", outputLines, false);
	}

}
