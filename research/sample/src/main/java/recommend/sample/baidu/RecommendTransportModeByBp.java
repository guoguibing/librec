/**
 * 
 */
package recommend.sample.baidu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import research.core.vo.Action;
import research.core.vo.ActionResult;
import research.core.vo.Item;
import research.core.vo.User;
import research.data.reader.UserActionItemReader;
import research.model.recommend.Bp;

/**
 * 
 * 使用BP神经网络预测百度推荐大赛 https://dianshi.baidu.com/competition/29/rule
 * 
 * @author dayang
 *
 */
public class RecommendTransportModeByBp {
	private static final UserActionItemReader UserActionItemReaderInstance = new UserActionItemReader();

	/**
	 * 数据文件路径，结果文件也写在该路径下，测试时需替换为正确的文件路径
	 */
	private static String BASE_PATH = "/Users/dayang/Documents/个人学习/AI/智能推荐/比赛/百度推荐大赛/data_set_phase1/";

	/**
	 * 日志对象
	 */
	private static Logger log = Logger.getLogger(RecommendTransportModeByBp.class);

	public static void main(String[] args) throws IOException {
		// 初始化，输入76个特征，结果1个
		Bp bp = new Bp(76, 8, 1, 0.05);

		// 1、读取训练数据
		log.info("开始读取训练数据");
		long startTime = System.currentTimeMillis();
		List<ActionResult> actionResults = readTrainData();
		long endTime1 = System.currentTimeMillis();
		log.info("训练数据读取完毕，耗时:" + (endTime1 - startTime));

		// 2、训练模型
		log.info("开始训练模型");
		train(bp, actionResults);
		long endTime2 = System.currentTimeMillis();
		log.info("训练模型结束，耗时：" + (endTime2 - endTime1));

		// 3、读取预测数据
		log.info("开始读取预测数据");
		List<ActionResult> predictResults = readPredictData();
		long endTime3 = System.currentTimeMillis();
		log.info("预测数据读取完毕，耗时：" + (endTime3 - endTime2));

		// 4、预测并生成结果
		log.info("开始进行预测");
		Map<Integer, Integer> predictResult = predict(bp, predictResults);
		long endTime4 = System.currentTimeMillis();
		log.info("预测完毕，耗时：" + (endTime4 - endTime3));

		// 5、写预测结果文件
		log.info("开始写预测结果文件");
		writeResult(predictResult);
		long endTime5 = System.currentTimeMillis();
		log.info("预测结果文件生成完毕，耗时：" + (endTime5 - endTime4));
		log.info("全部过程执行完毕，共耗时：" + (endTime5 - startTime));
	}

	/**
	 * 写预测结果文件
	 * 
	 * @param predictResult
	 * @throws IOException
	 */
	private static void writeResult(Map<Integer, Integer> predictResult) throws IOException {
		File csv = new File(BASE_PATH + "/submission.csv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true));
		bw.write("sid,recommend_mode");
		bw.newLine();
		for (Map.Entry<Integer, Integer> entry : predictResult.entrySet()) {
			bw.write(entry.getKey() + "," + entry.getValue());
			bw.newLine();
		}
		bw.close();
	}

	/**
	 * 读训练数据
	 * 
	 * @return
	 */
	private static List<ActionResult> readTrainData() {
		Map<String, String> trainIteratorInitProps = new HashMap<String, String>();
		trainIteratorInitProps.put("userDataPath", BASE_PATH + "profiles.csv");
		trainIteratorInitProps.put("actionDataPath", BASE_PATH + "train_queries.csv");
		trainIteratorInitProps.put("itemDataPath", BASE_PATH + "train_plans.csv");
		trainIteratorInitProps.put("actionResultDataPath", BASE_PATH + "train_clicks.csv");
		List<ActionResult> actionResults = UserActionItemReaderInstance.readActionResults(
				trainIteratorInitProps.get("userDataPath"), trainIteratorInitProps.get("actionDataPath"),
				trainIteratorInitProps.get("itemDataPath"), trainIteratorInitProps.get("actionResultDataPath"), 0,
				Integer.MAX_VALUE);
		return actionResults;
	}

	/**
	 * 读预测数据
	 * 
	 * @return
	 */
	private static List<ActionResult> readPredictData() {
		Map<String, String> testIteratorInitProps = new HashMap<String, String>();
		testIteratorInitProps.put("userDataPath", BASE_PATH + "profiles.csv");
		testIteratorInitProps.put("actionDataPath", BASE_PATH + "test_queries.csv");
		testIteratorInitProps.put("itemDataPath", BASE_PATH + "test_plans.csv");
		List<ActionResult> predictResults = UserActionItemReaderInstance.readActionResults(
				testIteratorInitProps.get("userDataPath"), testIteratorInitProps.get("actionDataPath"),
				testIteratorInitProps.get("itemDataPath"), testIteratorInitProps.get("actionResultDataPath"), 0,
				Integer.MAX_VALUE);
		return predictResults;
	}

	/**
	 * 训练模型 TODO 特征未做筛选和合并，当前使用的全量特征，有待优化
	 * 
	 * @param bp
	 *            bp网络模型
	 * @param trainData
	 *            训练数据
	 */
	private static void train(Bp bp, List<ActionResult> actionResultList) {
		for (ActionResult actionResult : actionResultList) {
			//  训练数据
			double[] trainData = getFeatures(actionResult);
			// 目标数据
			double[] targetData = new double[1];
			if (actionResult.getLabel() != null) {
				targetData[0] = actionResult.getLabel().getDoubleValue().get(0);
			}
			// 训练
			bp.train(trainData, targetData);
		}
	}

	/**
	 * 预测
	 * 
	 * @param bp
	 * @param predictResultList
	 * @return
	 */
	private static Map<Integer, Integer> predict(Bp bp, List<ActionResult> predictResultList) {
		// 预测结果：Map<sid,transportMode>
		Map<Integer, Integer> preditResult = new HashMap<Integer, Integer>();
		// 存储最大预测结果
		Map<Integer, Double> temMap = new HashMap<Integer, Double>();
		for (ActionResult actionResult : predictResultList) {
			double[] predictData = getFeatures(actionResult);
			double[] result = new double[1];
			bp.predict(predictData, result);
			int sid = actionResult.getAction().getSid();
			int transportMode = 0;
			if (actionResult.getItem() != null) {
				transportMode = (int) (actionResult.getItem().getExt().get("transportMode").get(0).doubleValue());
			}
			if (!temMap.containsKey(sid) || temMap.get(sid) < result[0]) {
				preditResult.put(sid, transportMode);
				temMap.put(sid, result[0]);
			}
		}
		return preditResult;
	}

	/**
	 * 获取特征数组
	 * 
	 * @param actionResult
	 * @return
	 */
	private static double[] getFeatures(ActionResult actionResult) {
		User user = actionResult.getUser();
		Action action = actionResult.getAction();
		Item item = actionResult.getItem();
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
		List<Double> feature = new ArrayList<Double>();
		// 用户特征
		feature.addAll(userFeatures);
		// 行为特征
		feature.addAll(action.getDoubleValue());
		// item特征
		feature.addAll(itemFeatures);
		double[] result = new double[feature.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = feature.get(i);
		}
		return result;
	}

}
