package research.core.tool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.function.ForeachFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;

import junit.framework.TestCase;
import research.core.vo.User;

public class SparkToolTest extends TestCase implements Serializable {
	// 需要SparkToolTest实现Serializable接口才能运行
	private static final long serialVersionUID = -9041616189304355810L;

	public void testReadSimpleCSV() {
		String basePath = "E:/data/ai/baidu/data_set_phase1/";
		String userDataPath = basePath + "train_plans.csv";
		Dataset<Row> dataset = SparkTool.getSparkSession().read().option("header", "true")
				// .option("inferSchema", "true")
				// .option("sep", ",")
				.csv(userDataPath);
//		System.out.println("dataset.count() = " + dataset.count());
//		System.out.println("dataset.columns().length = " + dataset.columns().length);
//		dataset.show(10);
		Dataset<User> userDataset = dataset.map(new MapFunction<Row, User>() {
			private static final long serialVersionUID = -1594069586313463661L;

			@Override
			public User call(Row row) {
				// "196356","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","1.0","0.0","0.0","1.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0"
				System.out.println(row.get(0));
//				Integer uid = row.getInt(0);
				String uid = row.getString(0);
				int id = Integer.parseInt(uid);
				System.out.println(id);
				return null;
			}
		}, Encoders.bean(User.class));
		List<User> userList = userDataset.collectAsList();
	}

	public void testReadTxt() {
		String basePath = "E:/data/ai/librec/movielens/ml-1m/";
		String userDataPath = basePath + "users.dat";
		Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", "false").textFile(userDataPath);
		dataset.foreach(new ForeachFunction<String>() {
			private static final long serialVersionUID = -4086842744865453397L;

			@Override
			public void call(String row) throws Exception {
				System.out.println(row);
			}
		});
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
		}
	}
}
