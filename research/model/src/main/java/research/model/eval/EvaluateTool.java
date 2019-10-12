package research.model.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 结果评估工具
 * 
 * @author liweigu714@163.com
 *
 */
public class EvaluateTool {
	public static void evalF1(List<Integer> predictedResults, List<Integer> labels) {



		// TODO: 实现算法
	}

	// TODO: 1）修改算法；2）返回对象型结果
	public static void evalF1(List<List<Double>> predictedResults, List<List<Double>> labels, double maxAbs) {
		List<Double> predictedValues = new ArrayList<Double>();
		for (List<Double> predictedResult : predictedResults) {
			predictedValues.add(predictedResult.get(0));
		}
		List<Double> labelValues = new ArrayList<Double>();
		for (List<Double> label : labels) {
			labelValues.add(label.get(0));
		}

		double truePositive = 0;
		double falsePositive = 0;
		double trueNegative = 0;
		double falseNegative = 0;

		for (int i = 0; i < predictedValues.size(); i++) {
			double predictedValue = predictedValues.get(i);
			double labelValue = labelValues.get(i);

			// TODO: 反归一化

			boolean positive = Math.abs(predictedValue - labelValue) < maxAbs;
			boolean isTrue = labelValue == 1;

			if (positive && isTrue) {
				++truePositive;
			} else if (positive && !isTrue) {
				++falsePositive;
			} else if (!positive && isTrue) {
				++trueNegative;
			} else if (!positive && !isTrue) {
				++falseNegative;
			}
		}

		double accuracy = (truePositive + falseNegative) / (truePositive + falsePositive + trueNegative + falseNegative);
		double recall = truePositive / (truePositive + falsePositive);
		double precision = truePositive / (truePositive + trueNegative);
		double f = 2 * recall * precision / (recall + precision);

		System.out.println("accuracy = " + accuracy);
		System.out.println("recall = " + recall);
		System.out.println("precision = " + precision);
		System.out.println("f = " + f);
	}

}
