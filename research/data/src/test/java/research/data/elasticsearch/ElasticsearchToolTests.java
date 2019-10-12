package research.data.elasticsearch;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ElasticsearchToolTests {
	@Test
	public void saveAndRead() {
		String urls = "es.liweigu.top:6200";
		ElasticsearchTool elasticsearchTool = new ElasticsearchTool(urls);
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("name", "Zhang");
		data.put("age", 30);
		dataList.add(data);
		String indexName = "librec";
		boolean saved = elasticsearchTool.save(dataList, indexName);
		assertTrue("写入ES失败", saved);

		dataList = elasticsearchTool.read(indexName);
		System.out.println(dataList.size());
		assertTrue("查询结果记录数为0", dataList.size() > 0);
		for (Map<String, Object> item : dataList) {
			System.out.println("name = " + item.get("name"));
			System.out.println("age = " + item.get("age"));
		}
	}

	@Test
	public void read() {
		String urls = "es.liweigu.top:6200";
		ElasticsearchTool elasticsearchTool = new ElasticsearchTool(urls);
		String indexName = "librec";
		List<Map<String, Object>> dataList = elasticsearchTool.read(indexName);
		System.out.println(dataList.size());
		assertTrue("查询结果记录数为0", dataList.size() > 0);
		for (Map<String, Object> item : dataList) {
			System.out.println("name = " + item.get("name"));
			System.out.println("age = " + item.get("age"));
		}
	}
}
