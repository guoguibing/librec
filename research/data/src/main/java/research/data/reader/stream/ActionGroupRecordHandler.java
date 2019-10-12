package research.data.reader.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import research.core.vo.Action;
import research.core.vo.ActionGroup;
import research.data.elasticsearch.ElasticsearchTool;

/**
 * 行为组数据处理工具。<br>
 * 行为组数据指用户的一组具有时序特征的行为，比如经过查询、浏览等行为，最后以购买行为结束。<br>
 * 行为数据随时间推进而产生，以流数据的方式读入，需要将同一组数据归并在一起，以方便后续查询使用。<br>
 * 具体来说，用kafka读取数据，在ConsumerRecordHandler里处理数据；handle方法会处理新读入的数据，saveActionGroup会根据用户id和session id将同一组数据归并存放，getPreviousActionGroup用于查询之前存放的数据。<br>
 * 完成写入后，数据存放在Elasticsearch里，每个行为组数据是一条记录。
 * 
 * @author liweigu714@163.com
 *
 */
public class ActionGroupRecordHandler implements ConsumerRecordHandler {

	/**
	 * 数据处理
	 * 
	 * @param record 数据
	 * @return 是否还要持续取数据
	 */
	@Override
	public boolean handle(List<ConsumerRecord<String, String>> records) {
		for (ConsumerRecord<String, String> record : records) {
			// user_id,session_id,timestamp,step,action_type,reference,platform,city,device,current_filters,impressions,prices
			String value = record.value();
			String[] arr = value.split(",");
			String userId = arr[0];
			String sessionId = arr[1];
			ActionGroup actionGroup = getPreviousActionGroup(userId, sessionId);
			if (actionGroup == null) {
				actionGroup = new ActionGroup(userId, sessionId);
			}

			Action action = new Action();
			// TODO 解析数据
			actionGroup.getActions().add(action);

			saveActionGroup(userId, sessionId, actionGroup);
		}
		return true;
	}
	
	/**
	 * 数据处理
	 * 
	 * @param record 数据
	 */
	public void handle2(List<ConsumerRecord<String, String>> records) {
		for (ConsumerRecord<String, String> record : records) {
			// user_id,session_id,timestamp,step,action_type,reference,platform,city,device,current_filters,impressions,prices
			String value = record.value();
			String[] arr = value.split(",");
			String userId = arr[0];
			String sessionId = arr[1];
			
			Map<String,Object> actionMap = new HashMap<String,Object>();
			actionMap.put("timestamp", arr[2]);
			actionMap.put("step", arr[3]);
			actionMap.put("actionType", arr[4]);
			actionMap.put("reference", arr[5]);
			actionMap.put("platform", arr[6]);
			actionMap.put("city", arr[7]);
			actionMap.put("device", arr[8]);
			actionMap.put("currentFilters", arr[9]);
			actionMap.put("impressions", arr[10]);
			actionMap.put("prices", arr[11]);

			ElasticsearchTool elasticsearchTool = new ElasticsearchTool("es.liweigu.top:6200");
			// 从ES中取数据，查看是否有已存在相同userId和sessionId的数据
			List<Map<String, Object>> actionGroupList = elasticsearchTool.read("trivago");
			for (Map<String, Object> actionGroup : actionGroupList) {
				if (userId.equals(actionGroup.get("userId")) && sessionId.equals(actionGroup.get("sessionId"))) {
					//TODO 更新ES中的数据
					return;
				}
			}
			
			List<Map<String, Object>> newResult = new ArrayList<Map<String, Object>>();
			Map<String,Object> newResultMap = new HashMap<String,Object>();
			newResultMap.put("userId", userId);
			newResultMap.put("sessionId", sessionId);
			List<Map<String,Object>> actionLists = new ArrayList<Map<String,Object>>();
			actionLists.add(actionMap);
			newResultMap.put("actions", actionLists);
			newResult.add(newResultMap);
			elasticsearchTool.save(newResult, "trivago");
		}
	}

	/**
	 * 获取行为组
	 * 
	 * @param userId 用户ID
	 * @param sessionId Session ID
	 * @return 行为组
	 */
	private ActionGroup getPreviousActionGroup(String userId, String sessionId) {
		// TODO 从ES读取数据
		// ElasticsearchTool
		return null;
	}

	/**
	 * 保存行为组
	 * 
	 * @param userId 用户ID
	 * @param sessionId Session ID
	 * @param actionGroup 行为组
	 */
	private void saveActionGroup(String userId, String sessionId, ActionGroup actionGroup) {
		// TODO 将数据写入ES
	}

}
