package research.data.elasticsearch;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * Elasticsearch读写工具
 * 
 * @author wanghuobin, liweigu714@163.com
 *
 */
public class ElasticsearchTool {
	private static final Log LOG = LogFactory.getLog(ElasticsearchTool.class);

	// Connect time para
	private static final int CONNECT_TIME_OUT = 1000;
	private static final int SOCKET_TIME_OUT = 30000;
	private static final int CONNECTION_REQUEST_TIME_OUT = 500;

	// Connect num
	private static final int MAX_CONNECT_NUM = 100;
	private static final int MAX_CONNECT_PER_ROUTE = 100;

	private RestHighLevelClient restHighLevelClient;

	private String userName;

	private String password;

	/**
	 * 构造函数
	 * 
	 * @param urls ES连接
	 */
	public ElasticsearchTool(String urls) {
		this(urls, null, null);
	}

	/**
	 * 构造函数
	 * 
	 * @param urls ES连接
	 * @param userName 用户名
	 * @param password 密码
	 */
	public ElasticsearchTool(String urls, String userName, String password) {
		this.restHighLevelClient = this.getRestHighLevelClient(urls);
		this.userName = userName;
		this.password = password;
	}

	private RestHighLevelClient getRestHighLevelClient(String urls) {
		RestHighLevelClient restHighLevelClient = null;

		List<HttpHost> httpHosts = new ArrayList<>();
		for (String url : urls.split(",")) {
			String[] hostPort = url.split(":");
			httpHosts.add(new HttpHost(hostPort[0], Integer.parseInt(hostPort[1])));
		}
		if (httpHosts != null && httpHosts.size() > 0) {
			// Create a connection for each es node.
			RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()]));
			// set connect time
			builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
				public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
					requestConfigBuilder.setConnectTimeout(CONNECT_TIME_OUT);
					requestConfigBuilder.setSocketTimeout(SOCKET_TIME_OUT);
					requestConfigBuilder.setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT);
					return requestConfigBuilder;
				}
			});
			// set connect num
			builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
				public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
					httpClientBuilder.setMaxConnTotal(MAX_CONNECT_NUM);
					httpClientBuilder.setMaxConnPerRoute(MAX_CONNECT_PER_ROUTE);
					return httpClientBuilder;
				}
			});
			// set userName and password
			if (this.userName != null && this.password != null) {
				final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
				builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
					@Override
					public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
						httpClientBuilder.disableAuthCaching();
						return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
					}
				});
			}
			// Initialize the high-level REST client which sends the actual requests to ElasticSearch.
			restHighLevelClient = new RestHighLevelClient(builder);
		} else {
			LOG.error(String.format("The  url = %s format error!(eg:localhost:9200,localhost:9222)", urls));
		}

		return restHighLevelClient;
	}

	/**
	 * 读取数据
	 * 
	 * @param indexName 索引名
	 * @return 数据
	 */
	public List<Map<String, Object>> read(String indexName) {
		List<Map<String, Object>> rlt = new ArrayList<>();
		final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
		SearchRequest searchRequest = new SearchRequest(indexName);

		searchRequest.scroll(scroll);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(matchAllQuery());
		searchRequest.source(searchSourceBuilder);

		try {
			SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
			String scrollId = searchResponse.getScrollId();
			SearchHit[] searchHits = searchResponse.getHits().getHits();

			while (searchHits != null && searchHits.length > 0) {
				for (SearchHit hit : searchHits) {
					rlt.add(hit.getSourceAsMap());
				}
				SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
				scrollRequest.scroll(scroll);
				searchResponse = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
				scrollId = searchResponse.getScrollId();
				searchHits = searchResponse.getHits().getHits();
			}
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollId);
			ClearScrollResponse clearScrollResponse = restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
			boolean succeeded = clearScrollResponse.isSucceeded();
			if (succeeded) {
				LOG.info(String.format("Read  %s documents  from  %s index successfully", rlt.size(), indexName));
			} else {
				LOG.info(String.format("Read %s index failure", indexName));
			}
		} catch (Exception e) {
			LOG.error(e);
		} finally {
			try {
				restHighLevelClient.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rlt;
	}

	/**
	 * 保存数据
	 * 
	 * @param mapList 数据
	 * @param indexName 索引名
	 * @return 是否成功
	 */
	public boolean save(List<Map<String, Object>> mapList, String indexName) {
		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
				int numberOfActions = request.numberOfActions();
				LOG.info(String.format("Executing bulk [%s] with %s requests", executionId, numberOfActions));
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				if (response.hasFailures()) {
					LOG.warn(String.format("Bulk [%s] executed with failures", executionId));
				} else {
					LOG.info(String.format(String.format("Bulk [%s] completed in %s milliseconds", executionId, response.getTook().getMillis())));
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				LOG.error("Failed to execute bulk", failure);
			}
		};
		BulkProcessor bulkProcessor = BulkProcessor.builder((request, bulkListener) ->
						restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
				listener).build();
		// set bulkProcessor handle requests execution.
		BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) ->
						restHighLevelClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
				listener);
		builder.setBulkActions(500);
		builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
		builder.setConcurrentRequests(0);
		builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
		builder.setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(1L), 3));
		// add indexRequest to bulk processor
		if (CollectionUtils.isEmpty(mapList) || StringUtils.isBlank(indexName)) {
			LOG.error("The index name or data is null");
		} else {
			mapList.stream().forEach(map -> bulkProcessor.add(new IndexRequest(indexName).source(map)));
		}
		try {
			boolean terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS); // save data to es
			if (terminated) {
				LOG.info("Insert data successful");
				return true;
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}
}
