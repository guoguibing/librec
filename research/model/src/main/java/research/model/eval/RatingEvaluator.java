package research.model.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rating评估器
 * 
 * @author liweigu714@163.com
 *
 */
public class RatingEvaluator {
	public static Map<String, Double> eval(List<List<Double>> predictedResults, List<List<Double>> labels, List<String> evalTypes) {
		List<Double> predictedValues = new ArrayList<Double>();
		for (List<Double> predictedResult : predictedResults) {
			predictedValues.add(predictedResult.get(0));
		}
		List<Double> labelValues = new ArrayList<Double>();
		for (List<Double> label : labels) {
			labelValues.add(label.get(0));
		}

		// TODO: 支持evalTypes参数
		double ae = 0;
		double se = 0;

		for (int i = 0; i < predictedValues.size(); i++) {
			double predictedValue = predictedValues.get(i);
			double labelValue = labelValues.get(i);

			// TODO: 支持传入 反归一化 对象
			predictedValue *= 5;
			labelValue *= 5;

			if (i < 20) {
				System.out.println("predictedValue = " + predictedValue + ", labelValue = " + labelValue);
			}

			ae += Math.abs(predictedValue - labelValue);
			se += Math.pow(predictedValue - labelValue, 2);
		}

		double mae = ae / predictedValues.size();
		double mse = se / predictedValues.size();
		// TODO
		double rmse = Math.sqrt(mse);

		Map<String, Double> evalResults = new HashMap<String, Double>();
		evalResults.put("mae", mae);
		evalResults.put("mse", mse);
		evalResults.put("rmse", rmse);
		return evalResults;
	}
}
