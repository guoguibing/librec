package research.data.reader;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.netty.util.internal.StringUtil;
import research.core.tool.SparkTool;
import research.core.vo.Action;
import research.core.vo.ActionResult;
import research.core.vo.Item;
import research.core.vo.Items;
import research.core.vo.Label;
import research.core.vo.User;

/**
 * 用户-行为-物品读取器
 * 
 * @author liweigu714@163.com
 *
 */
public class UserActionItemReader implements Serializable {
	private static Logger LOG = LoggerFactory.getLogger(UserActionItemReader.class);
	private static final long serialVersionUID = 7355533030642261649L;
	// 用户属性数据缓存
	private static Map<String, Map<String, User>> UsersData = new HashMap<String, Map<String, User>>();
	// 用户行为数据缓存
	private static Map<String, Map<Integer, Action>> ActionsData = new HashMap<String, Map<Integer, Action>>();
	// 物品数据缓存
	private static Map<String, Map<Integer, Items>> ItemsData = new HashMap<String, Map<Integer, Items>>();
	// 标签数据缓存
	private static Map<String, Map<Integer, Integer>> ClicksData = new HashMap<String, Map<Integer, Integer>>();
	private boolean useSpark = true;
	private String strategy = "score";
	private int outputSize = 1;

	public UserActionItemReader() {
	}

	/**
	 * 构造函数
	 * 
	 * @param strategy 策略。针对不同的算法，需要以不同方式读取数据。<br>
	 *            已支持的策略包括：score,itemid,multiclass。score是评分策略，将每条点击结果匹配多种类型的结果生成多条结果，每个生成结果带有一个评分；itemid是直接预测物品id值；multiclass是预测物品多分类值。<br>
	 *            默认值是score。
	 */
	public UserActionItemReader(String strategy) {
		if (strategy != null) {
			this.strategy = strategy;
		}
	}

	/**
	 * 构造函数
	 * 
	 * @param strategy 策略。针对不同的算法，需要以不同方式读取数据。<br>
	 *            已支持的策略包括：score,itemid,multiclass。score是评分策略，将每条点击结果匹配多种类型的结果生成多条结果，每个生成结果带有一个评分；itemid是直接预测物品id值；multiclass是预测物品多分类值。<br>
	 *            默认值是score。
	 * @param useSpark 是否使用spark读取数据
	 */
	public UserActionItemReader(String strategy, boolean useSpark) {
		if (strategy != null) {
			this.strategy = strategy;
		}
		this.useSpark = useSpark;
	}

	public int getOutputSize() {
		return outputSize;
	}

	public void setOutputSize(int outputSize) {
		this.outputSize = outputSize;
	}

	/**
	 * 读取用户属性数据
	 * 
	 * @param userDataPath 用户数据路径
	 * @return 用户数据
	 */
	public Map<String, User> readUsers(String userDataPath) {
		Map<String, User> userMap;

		if (UsersData.containsKey(userDataPath)) {
			userMap = UsersData.get(userDataPath);
		} else {
			userMap = new HashMap<String, User>();

			if (useSpark) {
				Dataset<Row> dataset = SparkTool.getSparkSession().read().option("header", "true").csv(userDataPath);
				Dataset<User> userDataset = dataset.map(new MapFunction<Row, User>() {
					private static final long serialVersionUID = 4867603486988710260L;

					@Override
					public User call(Row row) {
						// "196356","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","1.0","0.0","0.0","1.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0"
						String uid = row.getString(0);
						List<Double> values = new ArrayList<Double>();
						for (int j = 1; j < row.size(); j++) {
							values.add(Double.parseDouble(row.getString(j)));
						}
						User user = new User(values);
						user.setId(uid);
						return user;
					}
				}, Encoders.bean(User.class));
				List<User> userList = userDataset.collectAsList();
				for (User user : userList) {
					String id = user.getId();
					userMap.put(id, user);
				}
			} else {
				List<String> lines = FileTool.readFile(userDataPath);
				for (int i = 1; i < lines.size(); i++) {
					// "196356","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","1.0","0.0","0.0","1.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","1.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0","0.0"
					String[] arr = lines.get(i).split("\",\"");
					// 去掉第一个引号
					String uid = arr[0].substring(1);
					// 去掉最后一个引号
					arr[arr.length - 1] = arr[arr.length - 1].substring(0, arr[arr.length - 1].length() - 1);
					List<Double> values = new ArrayList<Double>();
					for (int j = 1; j < arr.length; j++) {
						values.add(Double.parseDouble(arr[j]));
					}
					User user = new User(values);
					userMap.put(uid, user);
				}
			}

			UsersData.put(userDataPath, userMap);
		}

		return userMap;
	}

	/**
	 * 读取用户行为数据
	 * 
	 * @param actionDataPath 用户行为数据路径
	 * @return 用户行为数据
	 */
	public Map<Integer, Action> readActions(String actionDataPath) {
		Map<Integer, Action> actionMap;

		if (ActionsData.containsKey(actionDataPath)) {
			actionMap = ActionsData.get(actionDataPath);
		} else {
			actionMap = new HashMap<Integer, Action>();

			if (useSpark) {
				Dataset<Row> dataset = SparkTool.getSparkSession().read().option("header", "true").csv(actionDataPath);
				Dataset<Action> actionDataset = dataset.map(new MapFunction<Row, Action>() {
					private static final long serialVersionUID = 2206590485696083609L;

					@Override
					public Action call(Row row) {
						// "1126541","178395","2018-12-05 14:51:43","116.41,39.92","116.46,39.95"
						Integer sid = Integer.parseInt(row.getString(0));
						// 如果pid为空，则默认为0
						Integer pid = 0;
						String pidString = row.getString(1);
						if (!StringUtil.isNullOrEmpty(pidString)) {
							pid = Integer.parseInt(pidString);
						}
						String req_time = row.getString(2);
						String origin = row.getString(3);
						String destination = row.getString(4);

						String[] from = origin.split(",");
						String[] to = destination.split(",");
						double x1 = Double.parseDouble(from[0]);
						double y1 = Double.parseDouble(from[1]);
						double x2 = Double.parseDouble(to[0]);
						double y2 = Double.parseDouble(to[1]);

						// 正则化
						x1 = (x1 - 100) / 100;
						y1 = y1 / 100;
						x2 = (x2 - 100) / 100;
						y2 = y2 / 100;

						List<Double> values = new ArrayList<Double>();
						values.add(x1);
						values.add(y1);
						values.add(x2);
						values.add(y2);

						Action action = new Action(values);
						action.setSid(sid);
						action.setPid(pid);
						action.setTime(req_time);

						return action;
					}
				}, Encoders.bean(Action.class));
				List<Action> actionList = actionDataset.collectAsList();
				for (Action action : actionList) {
					int sid = action.getSid();
					actionMap.put(sid, action);
				}
			} else {
				List<String> lines = FileTool.readFile(actionDataPath);
				for (int i = 1; i < lines.size(); i++) {
					// "1126541","178395","2018-12-05 14:51:43","116.41,39.92","116.46,39.95"
					String[] arr = lines.get(i).split("\",\"");
					Integer sid = Integer.parseInt(arr[0].substring(1));
					// 如果pid为空，则默认为0
					Integer pid = 0;
					if (!StringUtil.isNullOrEmpty(arr[1])) {
						pid = Integer.parseInt(arr[1]);
					}
					String req_time = arr[2];
					String origin = arr[3];
					String destination = arr[4].substring(0, arr[4].length() - 1);

					String[] from = origin.split(",");
					String[] to = destination.split(",");
					double x1 = Double.parseDouble(from[0]);
					double y1 = Double.parseDouble(from[1]);
					double x2 = Double.parseDouble(to[0]);
					double y2 = Double.parseDouble(to[1]);

					// 正则化
					x1 = (x1 - 100) / 100;
					y1 = y1 / 100;
					x2 = (x2 - 100) / 100;
					y2 = y2 / 100;

					List<Double> values = new ArrayList<Double>();
					values.add(x1);
					values.add(y1);
					values.add(x2);
					values.add(y2);

					Action action = new Action(values);
					action.setSid(sid);
					action.setPid(pid);
					action.setTime(req_time);

					actionMap.put(sid, action);
				}
			}

			ActionsData.put(actionDataPath, actionMap);
		}

		return actionMap;
	}

	/**
	 * 读取物品数据
	 * 
	 * @param itemDataPath 物品数据路径
	 * @return 物品数据
	 */
	public Map<Integer, Items> readItems(String itemDataPath) {
		Map<Integer, Items> itemsMap;

		if (ItemsData.containsKey(itemDataPath)) {
			itemsMap = ItemsData.get(itemDataPath);
		} else {
			itemsMap = new HashMap<Integer, Items>();

			if (useSpark) {
				// 用csv读会破坏数据，取第2个字段不是完整json。因此用textFile读。
				Dataset<String> dataset = SparkTool.getSparkSession().read().option("header", true).textFile(itemDataPath);
				Dataset<Items> itemsDataset = dataset.map(new MapFunction<String, Items>() {
					private static final long serialVersionUID = 5727205531197965202L;

					@Override
					public Items call(String row) {
						// "1112456","2018-12-05 17:39:47","[{""distance"": 465, ""price"": """", ""eta"": 418, ""transport_mode"": 5}, {""distance"": 465,
						// ""price"":
						// """",
						// ""eta"": 140, ""transport_mode"": 6}, {""distance"": 462, ""price"": """", ""eta"": 178, ""transport_mode"": 3}]"
						DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
						String[] arr = row.split("\",\"");
						arr[0] = arr[0].substring(1);
						if (arr[0].equals("sid")) {
							// 不能返回null，否则spark会报错。
							return new Items();
						}
						Integer sid = Integer.parseInt(arr[0]);
						LocalDateTime planTime = LocalDateTime.parse(arr[1], dateTimeFormatter);
						String plans = arr[2].substring(0, arr[2].length() - 1);

						// TODO: 以下代码是针对百度推荐数据的内容，要做成通用方法。
						// 时间特征
						int dayOfWeek = planTime.getDayOfWeek().getValue();
						int hour = planTime.getHour();

						List<Item> items = new ArrayList<Item>();
						// 物品（计划）特征
						JSONArray jsonPlans = JSONArray.parseArray(plans.replace("\"\"", "\""));
						List<Double> transportModes = new ArrayList<Double>();
						for (int j = 0; j < jsonPlans.size(); j++) {
							JSONObject jsonPlan = jsonPlans.getJSONObject(j);
							double distance = jsonPlan.getDoubleValue("distance");
							double price = 0;
							String strPrice = jsonPlan.getString("price");
							if (strPrice != null && strPrice.length() > 0 && !strPrice.equals("\"\"")) {
								try {
									price = Double.parseDouble(strPrice);
								} catch (Exception e) {
								}
							}
							double eta = jsonPlan.getDoubleValue("eta");
							double transportMode = jsonPlan.getDoubleValue("transport_mode");
							transportModes.add(transportMode);

							List<Double> values = new ArrayList<Double>();
							// 正则化
							values.add(dayOfWeek * 1.0);
							values.add(hour * 1.0 / 24);

							values.add(distance / 100000);
							values.add(price / 10000);
							values.add(eta / 20000);
							values.add(transportMode);

							Item item = new Item(values);
							item.setId(sid);
							item.getExt().put("transportMode", Collections.singletonList(transportMode));
							items.add(item);
						}

						for (Item item : items) {
							item.getExt().put("transportModes", transportModes);
						}

						return new Items(items);
					}
				}, Encoders.bean(Items.class));
				List<Items> itemsList = itemsDataset.collectAsList();
				for (Items items : itemsList) {
					if (items.getItems().size() > 0) {
						int sid = items.getItems().get(0).getId();
						itemsMap.put(sid, items);
					}
				}
			} else {
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
				List<String> lines = FileTool.readFile(itemDataPath);
				LOG.info("(item) lines.size() = " + lines.size());
				for (int i = 1; i < lines.size(); i++) {
					if (i % 100000 == 0) {
						LOG.info("(item) i = " + i);
					}
					// "1112456","2018-12-05 17:39:47","[{""distance"": 465, ""price"": """", ""eta"": 418, ""transport_mode"": 5}, {""distance"": 465,
					// ""price"":
					// """",
					// ""eta"": 140, ""transport_mode"": 6}, {""distance"": 462, ""price"": """", ""eta"": 178, ""transport_mode"": 3}]"
					String[] arr = lines.get(i).split("\",\"");
					Integer sid = Integer.parseInt(arr[0].substring(1));
					LocalDateTime planTime = LocalDateTime.parse(arr[1], dateTimeFormatter);
					String plans = arr[2].substring(0, arr[2].length() - 1);

					// TODO: 以下代码是针对百度推荐数据的内容，要做成通用方法。
					// 时间特征
					int dayOfWeek = planTime.getDayOfWeek().getValue();
					int hour = planTime.getHour();

					List<Item> items = new ArrayList<Item>();
					// 物品（计划）特征
					JSONArray jsonPlans = JSONArray.parseArray(plans.replace("\"\"", "\""));
					List<Double> transportModes = new ArrayList<Double>();
					for (int j = 0; j < jsonPlans.size(); j++) {
						JSONObject jsonPlan = jsonPlans.getJSONObject(j);
						double distance = jsonPlan.getDoubleValue("distance");
						double price = 0;
						String strPrice = jsonPlan.getString("price");
						if (strPrice != null && strPrice.length() > 0 && !strPrice.equals("\"\"")) {
							try {
								price = Double.parseDouble(strPrice);
							} catch (Exception e) {
							}
						}
						double eta = jsonPlan.getDoubleValue("eta");
						double transportMode = jsonPlan.getDoubleValue("transport_mode");
						transportModes.add(transportMode);

						List<Double> values = new ArrayList<Double>();
						// 正则化
						values.add(dayOfWeek * 1.0);
						values.add(hour * 1.0 / 24);

						values.add(distance / 100000);
						values.add(price / 10000);
						values.add(eta / 20000);
						values.add(transportMode);

						Item item = new Item(values);
						item.setId(sid);
						item.getExt().put("transportMode", Collections.singletonList(transportMode));
						items.add(item);
					}

					for (Item item : items) {
						item.getExt().put("transportModes", transportModes);
					}

					itemsMap.put(sid, new Items(items));
				}
			}

			LOG.info("itemsMap.size() = " + itemsMap.size());
			ItemsData.put(itemDataPath, itemsMap);
		}

		return itemsMap;
	}

	/**
	 * 读取行为结果数据
	 * 
	 * @param userDataPath 用户数据路径
	 * @param actionDataPath 用户行为数据路径
	 * @param itemDataPath 物品数据路径
	 * @param actionResultDataPath 行为结果数据路径
	 * @param index 起始索引
	 * @param num 期望返回记录数
	 * @return 行为结果列表
	 */
	public List<ActionResult> readActionResults(String userDataPath, String actionDataPath, String itemDataPath, String actionResultDataPath, int index,
			int num) {
		// 先读取用户、行为、物品、点击数据，因为它们会存入缓存，所以在多次调用时不会影响性能。
		Map<String, User> userMap = readUsers(userDataPath);
		Map<Integer, Action> actionMap = readActions(actionDataPath);
		Map<Integer, Items> itemsMap = readItems(itemDataPath);

		// sid -> click_mode
		Map<Integer, Integer> clicks = null;
		if (actionResultDataPath != null && actionResultDataPath.length() > 0) {
			if (ClicksData.containsKey(actionResultDataPath)) {
				clicks = ClicksData.get(actionResultDataPath);
			} else {
				clicks = new HashMap<Integer, Integer>();
				List<String> lines = FileTool.readFile(actionResultDataPath);
				for (int i = 1; i < lines.size(); i++) {
					// "2848914","2018-11-17 18:42:17","1"
					String[] arr = lines.get(i).split("\",\"");
					// 去掉第一个引号
					Integer sid = Integer.parseInt(arr[0].substring(1));
					String click_time = arr[1];
					// 去掉最后一个引号
					arr[2] = arr[2].substring(0, arr[2].length() - 1);
					int click_mode = Integer.parseInt(arr[2]);

					clicks.put(sid, click_mode);
				}

				LOG.info("clicks.size() = " + clicks.size());
				ClicksData.put(actionResultDataPath, clicks);
			}
		}

		// TODO: 需要保证数据顺序一致
		int currentIndex = 0;
		List<ActionResult> actionResults = new ArrayList<ActionResult>();
		for (Entry<Integer, Items> entry : itemsMap.entrySet()) {
			if (currentIndex % 1000 == 0) {
				LOG.debug("currentIndex = " + currentIndex);
			}
			Integer sid = entry.getKey();
			Items items = entry.getValue();

			Action action = actionMap.get(sid);
			User user = null;
			if (action != null) {
				user = userMap.get(action.getPid());
			}
			boolean endLoop = false;
			if ("score".equals(this.strategy)) {
				if (clicks != null) {
					// 训练数据
					int click_mode = 0;
					if (clicks.containsKey(sid)) {
						click_mode = clicks.get(sid);
					}

					double clickedScore = 1.0;
					double recalled = 0.4;
					double unrecalled = 0.;
					// 对于每条点击结果，将结果跟12种结果选择进行比对，生成12条评分结果，其中比对相同的设置评分为1.0，被召回但未点击的评分为0.4，未被召回的为0.0。
					for (int i = 0; i < 12; i++) {
						// 更新索引
						++currentIndex;
						// 取num条记录，返回分批结果。
						if (currentIndex <= index) {
							continue;
						}
						if (currentIndex > index + num) {
							endLoop = true;
							break;
						}

						Item currentItem = null;
						for (Item item : items.getItems()) {
							double itemTransportMode = item.getExt().get("transportMode").get(0);
							if (itemTransportMode == i) {
								currentItem = item;
								break;
							}
						}

						Label label;
						if (i == click_mode) {
							// 被点击的
							label = new Label(Collections.singletonList(clickedScore));
						} else if (currentItem != null) {
							// 召回结果里未选中的
							label = new Label(Collections.singletonList(recalled));
						} else {
							// 未召回的
							label = new Label(Collections.singletonList(unrecalled));
						}
						ActionResult actionResult = new ActionResult(user, action, currentItem, label);
						actionResults.add(actionResult);
					}
				} else {
					// 测试数据不包含clicks
					for (int i = 0; i < 12; i++) {
						// 更新索引
						++currentIndex;
						if (currentIndex <= index) {
							continue;
						}
						if (currentIndex > index + num) {
							endLoop = true;
							break;
						}

						Item currentItem = null;
						for (Item item : items.getItems()) {
							double itemTransportMode = item.getExt().get("transportMode").get(0);
							if (itemTransportMode == i) {
								currentItem = item;
								break;
							}
						}

						Label label = null;
						ActionResult actionResult = new ActionResult(user, action, currentItem, label);
						actionResults.add(actionResult);
					}
				}
			} else if ("itemid".equals(this.strategy)) {
				// 更新索引
				++currentIndex;
				// if (currentIndex % 640 == 0) {
				LOG.debug("currentIndex = " + currentIndex + ", index = " + index + ", num = " + num);
				// }
				// 取num条记录，返回分批结果。
				if (currentIndex <= index) {
					continue;
				}
				if (currentIndex > index + num) {
					endLoop = true;
					break;
				}

				if (clicks != null) {
					// 训练数据
					int click_mode = 0;
					if (clicks.containsKey(sid)) {
						click_mode = clicks.get(sid);
					}

					Label label = new Label(Collections.singletonList((double) click_mode));
					ActionResult actionResult = new ActionResult(user, action, items, label);
					actionResults.add(actionResult);
				} else {
					// 测试数据不包含clicks

					Label label = null;
					ActionResult actionResult = new ActionResult(user, action, items, label);
					actionResults.add(actionResult);
				}
			} else if ("multyclass".equals(this.strategy)) {
				// 更新索引
				++currentIndex;
				// if (currentIndex % 640 == 0) {
				LOG.debug("currentIndex = " + currentIndex + ", index = " + index + ", num = " + num);
				// }
				// 取num条记录，返回分批结果。
				if (currentIndex <= index) {
					continue;
				}
				if (currentIndex > index + num) {
					endLoop = true;
					break;
				}

				if (clicks != null) {
					// 训练数据
					int click_mode = 0;
					if (clicks.containsKey(sid)) {
						click_mode = clicks.get(sid);
					}

					// 将click_mode转换为多分类one-hot值
					List<Double> values = new ArrayList<Double>();
					// 12种结果，加上0，一共是outputSize种分类
					for (int i = 0; i < outputSize; i++) {
						double onehotValue = 0;
						if (click_mode == i) {
							onehotValue = 1;
						}
						values.add(onehotValue);
					}

					Label label = new Label(Collections.singletonList((double) click_mode));
					ActionResult actionResult = new ActionResult(user, action, items, label);
					actionResults.add(actionResult);
				} else {
					// 测试数据不包含clicks

					Label label = null;
					ActionResult actionResult = new ActionResult(user, action, items, label);
					actionResults.add(actionResult);
				}
			} else {
				throw new IllegalStateException("strategy = " + this.strategy);
			}

			if (endLoop) {
				break;
			}
		}

		return actionResults;
	}

}
