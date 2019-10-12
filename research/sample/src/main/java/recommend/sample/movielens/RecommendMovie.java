package recommend.sample.movielens;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import research.core.data.iterator.DataIteratorI;
import research.core.vo.Rating;
import research.data.iterator.UserItemIterator;
import research.data.reader.UserItemReader;
import research.model.recommend.UserItemRecommendModelImpl;

/**
 * 电影推荐
 * 
 * @author liweigu714@163.com
 *
 */
public class RecommendMovie {
	public static void main(String[] args) throws IOException {
		String basePath = "E:/data/ai/librec/movielens/ml-1m/";
		run(basePath);
	}

	public static void run(String basePath) throws IOException {
		// 数据
		// TODO: 暂时预先划分了训练集/验证集/测试集，实现数据分割器后再支持动态分组。
		String userDataPath = basePath + "users.dat";
		String itemDataPath = basePath + "movies.dat";
		String trainDataPath = basePath + "ratings_train.dat";
		String validDataPath = basePath + "ratings_valid.dat";
		String testDataPath = basePath + "ratings_test.dat";

		// 训练
		UserItemRecommendModelImpl userItemRecommendModel = new UserItemRecommendModelImpl();
		Map<String, Object> modelInitProps = new HashMap<String, Object>();
		modelInitProps.put("inputSize", 23);
		boolean usePretrainedModel = true;
		String modelName = "movie.model";
		if (usePretrainedModel) {
			// 本地路径
			String pretrainedModelPath = basePath + modelName;
			// 远程路径
			pretrainedModelPath = "http://model.liweigu.top/model/movie.model";
			userItemRecommendModel.initModel(modelInitProps, pretrainedModelPath);
		} else {
			userItemRecommendModel.initModel(modelInitProps);

			int epoch = 1; // 10
			int batchSize = 32;
			Map<String, Object> iteratorInitProps = new HashMap<String, Object>();
			iteratorInitProps.put("userDataPath", userDataPath);
			iteratorInitProps.put("itemDataPath", itemDataPath);
			iteratorInitProps.put("trainDataPath", trainDataPath); // TODO
			for (int i = 0; i < epoch; i++) {
				if (i % 10 == 0) {
					System.out.println("i = " + i);
				}
				DataIteratorI dataIterator = new UserItemIterator(batchSize, iteratorInitProps);
				userItemRecommendModel.fit(dataIterator);
			}
			System.out.println("saving...");
			userItemRecommendModel.save(basePath + modelName);
		}

		// 验证与评估
		// 读取全部验证数据
		System.out.println("evaluating...");
		int evalCount = 100;
		List<Rating> validRatings = UserItemReader.readRatings(userDataPath, itemDataPath, validDataPath, 0, evalCount);
		System.out.println("validRatings.size() = " + validRatings.size());
		// 结果评估
		userItemRecommendModel.evaluate(validRatings);
	}

}
