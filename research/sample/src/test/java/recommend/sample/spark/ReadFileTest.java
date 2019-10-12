package recommend.sample.spark;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;

import junit.framework.TestCase;
import research.core.tool.SparkTool;
import research.core.vo.User;

public class ReadFileTest extends TestCase implements Serializable {
	private static final long serialVersionUID = 7272358542001232268L;

	public void testRead() {
		// String basePath = "E:/data/";
		String basePath = "E:/data/ai/librec/movielens/ml-1m/";
		String userDataPath = basePath + "test.csv";
		SparkTool.getSparkSession().read().csv(userDataPath);
	}

	public void testConvertToVo() {
		String basePath = "E:/data/ai/librec/movielens/ml-1m/";
		String userDataPath = basePath + "users.dat";
		Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", "false").textFile(userDataPath);
		Dataset<User> users = dataset.map(new MapFunction<String, User>() {
			private static final long serialVersionUID = -2345402111497784328L;

			@Override
			public User call(String row) {
				String[] arr = row.split("::");
				List<Double> values = new ArrayList<Double>();
				for (int i = 0; i < 4; i++) {
					if (i == 1) {
						double value = "M".equals(arr[i]) ? 1 : 0;
						values.add(value);
					} else {
						values.add(Double.parseDouble(arr[i]));
					}
				}
				User user = new User(values);
				user.setId(arr[0]);
				return user;
			}
		}, Encoders.bean(User.class));
		List<User> userList = users.takeAsList(3);
		for (User user : userList) {
			System.out.println(user.getId());
			System.out.println(user.getDoubleValue().size());
		}
	}
}
