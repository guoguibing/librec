package research.service.recommend.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import research.core.vo.Item;
import research.core.vo.Rating;
import research.core.vo.User;
import research.data.reader.UserItemReader;
import research.service.recommend.IRecommendService;
import research.service.recommend.vo.RecommendParameter;
import research.service.recommend.vo.RecommendResponse;

/**
 * 推荐服务网关
 * 
 * @author liweigu
 *
 */
@RestController
public class RecommendController {
	private IRecommendService service;
	private List<Rating> validRatings;
	private String basePath = "E:/data/ai/librec/movielens/ml-1m/";
	private String userDataPath = basePath + "users.dat";
	private String itemDataPath = basePath + "movies.dat";
	private String validDataPath = basePath + "ratings_valid.dat";

	/**
	 * 构造函数
	 */
	public RecommendController() {
		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "consumer.xml" }); // applicationContext.xml
		context.start();
		System.out.println("context.started");

		if (this.validRatings == null) {
			// 暂时用1000条
			int count = 1000;
			this.validRatings = UserItemReader.readRatings(userDataPath, itemDataPath, validDataPath, 0, count);
			System.out.println("this.validRatings.size() = " + this.validRatings.size());
		}

		this.service = (IRecommendService) context.getBean("service");
		System.out.println("IRecommendService bean got");
	}

	/**
	 * 推荐
	 * 
	 * @param userId 用户ID
	 * @param itemId 物品ID
	 * @return 推荐评分
	 */
	@RequestMapping("/recommend")
	public String recommend(String userId, String itemId) {
		String msg = null;

		// http://localhost:8080/recommend?userId=1001&itemId=2001
		RecommendParameter parameter = new RecommendParameter();
		parameter.setMsg(userId + "," + itemId);
		Integer userIdInteger = Integer.parseInt(userId);
		Integer itemIdInteger = Integer.parseInt(itemId);

		User user = UserItemReader.CachedUsers.get(userDataPath).get(userIdInteger);
		Item item = UserItemReader.CachedItems.get(itemDataPath).get(itemIdInteger);

		if (user != null && item != null) {
			double[][] features = new double[1][];
			List<Double> feature = new ArrayList<Double>();
			feature.addAll(user.getDoubleValue());
			feature.addAll(item.getDoubleValue());
			features[0] = new double[feature.size()];
			for (int i = 0; i < feature.size(); i++) {
				features[0][i] = feature.get(i);
			}
			parameter.setFeatures(features);
			RecommendResponse recommendResponse = this.service.recommend(parameter);
			System.out.println("recommendResponse.getMsg() = " + recommendResponse.getMsg());
			if (recommendResponse != null) {
				double[][] results = recommendResponse.getResults();
				double result = results[0][0];
				// 反归一化
				result *= 5;
				msg = "" + result;
			}
		} else {
			msg = "参数非法";
		}

		return msg;
	}
}
