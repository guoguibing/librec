package research.data.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;

import research.core.tool.SparkTool;
import research.core.vo.Item;
import research.core.vo.Label;
import research.core.vo.Rating;
import research.core.vo.User;

/**
 * 用户物品读取器
 * 
 * @author liweigu714@163.com
 *
 */
public class UserItemReader {
	public static Map<String, Map<String, User>> CachedUsers = new HashMap<String, Map<String, User>>();
	public static Map<String, Map<Integer, Item>> CachedItems = new HashMap<String, Map<Integer, Item>>();
	private static boolean UseSpark = true;

	/**
	 * 读取用户数据
	 * 
	 * @param userDataPath 用户数据路径
	 * @return 用户数据
	 */
	public static Map<String, User> readUsers(String userDataPath) {
		Map<String, User> users;
		if (CachedUsers.containsKey(userDataPath)) {
			users = CachedUsers.get(userDataPath);
		} else {
			synchronized (CachedUsers) {
				users = new HashMap<String, User>();

				if (UseSpark) {
					Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", "false").textFile(userDataPath);
					Dataset<User> userDataset = dataset.map(new MapFunction<String, User>() {
						private static final long serialVersionUID = -6223488677398733900L;

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
					List<User> userList = userDataset.collectAsList();
					for (User user : userList) {
						String id = user.getId();
						users.put(id, user);
					}
				} else {
					List<String> lines = FileTool.readFile(userDataPath);
					for (String line : lines) {
						String[] arr = line.split("::");
						String userId = arr[0];
						double gender = "M".equals(arr[1]) ? 0 : 1;
						double age = Double.parseDouble(arr[2]);
						double occupation = Double.parseDouble(arr[3]);
						// 只取前5位
						double zipCode = Double.parseDouble(arr[4].substring(0, 5));

						// 归一化
						age /= 100.0;
						occupation /= 20.0;
						zipCode /= 100000;

						List<Double> values = new ArrayList<Double>();
						values.add(gender);
						values.add(age);
						values.add(occupation);
						values.add(zipCode);
						User user = new User(values);
						user.setId(userId);
						users.put(userId, user);
					}
				}
				CachedUsers.put(userDataPath, users);
			}
		}

		return users;
	}

	/**
	 * 读取物品数据
	 * 
	 * @param itemDataPath 物品数据路径
	 * @return 物品数据
	 */
	public static Map<Integer, Item> readItems(String itemDataPath) {
		Map<Integer, Item> items;

		if (CachedItems.containsKey(itemDataPath)) {
			items = CachedItems.get(itemDataPath);
		} else {
			synchronized (CachedItems) {
				items = new HashMap<Integer, Item>();

				if (UseSpark) {
					Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", "false").textFile(itemDataPath);
					Dataset<Item> itemDataset = dataset.map(new MapFunction<String, Item>() {
						private static final long serialVersionUID = 1661358900251571176L;

						@Override
						public Item call(String row) {
							String[] arr = row.split("::");
							int movieId = Integer.parseInt(arr[0]);
							String movieTypes = arr[2];

							List<Double> values = new ArrayList<Double>();
							// 归一化
							double normedMovieId = movieId / 10000.0;
							values.add(normedMovieId);
							// types.length == 18
							String[] types = "Action,Adventure,Animation,Children's,Comedy,Crime,Documentary,Drama,Fantasy,Film-Noir,Horror,Musical,Mystery,Romance,Sci-Fi,Thriller,War,Western"
									.split(",");
							for (String type : types) {
								// movieTypes包含type则设置该列为1，否则设置该列为0
								double typeValue = movieTypes.contains(type) ? 1.0 : 0;
								values.add(typeValue);
							}
							Item item = new Item(values);
							item.setId(movieId);
							return item;
						}
					}, Encoders.bean(Item.class));
					List<Item> itemList = itemDataset.collectAsList();
					for (Item item : itemList) {
						int id = item.getId();
						items.put(id, item);
					}
				} else {
					List<String> lines = FileTool.readFile(itemDataPath);
					for (String line : lines) {
						String[] arr = line.split("::");
						int movieId = Integer.parseInt(arr[0]);
						String movieTypes = arr[2];

						List<Double> values = new ArrayList<Double>();
						// 归一化
						double normedMovieId = movieId / 10000.0;
						values.add(normedMovieId);
						// types.length == 18
						String[] types = "Action,Adventure,Animation,Children's,Comedy,Crime,Documentary,Drama,Fantasy,Film-Noir,Horror,Musical,Mystery,Romance,Sci-Fi,Thriller,War,Western"
								.split(",");
						for (String type : types) {
							// movieTypes包含type则设置该列为1，否则设置该列为0
							double typeValue = movieTypes.contains(type) ? 1.0 : 0;
							values.add(typeValue);
						}
						Item item = new Item(values);
						items.put(movieId, item);
					}
				}
				CachedItems.put(itemDataPath, items);
			}
		}

		return items;
	}

	/**
	 * 读取评分数据
	 * 
	 * @param userDataPath 用户数据路径
	 * @param itemDataPath 物品数据路径
	 * @param ratingDataPath 评分数据路径
	 * @return 评分数据
	 */
	public static List<Rating> readRatings(String userDataPath, String itemDataPath, String ratingDataPath) {
		return readRatings(userDataPath, itemDataPath, ratingDataPath, 0, -1);
	}

	/**
	 * 读取评分数据
	 * 
	 * @param userDataPath 用户数据路径
	 * @param itemDataPath 物品数据路径
	 * @param ratingDataPath 评分数据路径
	 * @param start 起始索引
	 * @param size 期望返回数量
	 * @return 评分数据
	 */
	public static List<Rating> readRatings(String userDataPath, String itemDataPath, String ratingDataPath, int start, int size) {
		List<Rating> ratings = new ArrayList<Rating>();

		Map<String, User> users = readUsers(userDataPath);
		Map<Integer, Item> items = readItems(itemDataPath);

		if (UseSpark) {
			Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", "false").textFile(ratingDataPath);
			Dataset<Rating> ratingDataset = dataset.map(new MapFunction<String, Rating>() {
				private static final long serialVersionUID = 4576097374975744214L;

				@Override
				public Rating call(String row) {
					String[] arr = row.split("::");
					String userId = arr[0];
					double movieId = Double.parseDouble(arr[1]);
					double ratingValue = Double.parseDouble(arr[2]);
					long time = Long.parseLong(arr[3]); // TODO

					// 归一化。TODO: 由预处理器进行数据归一化。
					ratingValue /= 5.0;

					User user = users.get(userId);
					Item item = items.get((int) movieId);
					Label label = new Label(Collections.singletonList(ratingValue));

					Rating rating = new Rating(user, item, label);
					rating.setUser(user);
					rating.setItem(item);
					rating.setLabel(label);
					return rating;
				}
			}, Encoders.bean(Rating.class));
			ratings = ratingDataset.collectAsList();
		} else {
			List<String> lines = FileTool.readFile(ratingDataPath, start, size);
			for (String line : lines) {
				String[] arr = line.split("::");
				String userId = arr[0];
				double movieId = Double.parseDouble(arr[1]);
				double ratingValue = Double.parseDouble(arr[2]);
				long time = Long.parseLong(arr[3]); // TODO

				// 归一化。TODO: 由预处理器进行数据归一化。
				ratingValue /= 5.0;

				User user = users.get(userId);
				// System.out.println(user.doubleValue().size());
				Item item = items.get(movieId);
				// System.out.println(item.doubleValue().size());
				Label label = new Label(Collections.singletonList(ratingValue));

				Rating rating = new Rating(user, item, label);
				rating.setUser(user);
				rating.setItem(item);
				rating.setLabel(label);
				ratings.add(rating);
			}
		}

		return ratings;
	}
}
