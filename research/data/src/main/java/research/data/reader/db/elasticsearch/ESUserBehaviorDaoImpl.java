package research.data.reader.db.elasticsearch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import research.data.reader.db.UserBehaviorDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * \* Created with IntelliJ IDEA.
 * \* User: whb
 * \* Date: 19-4-4
 * \* Time: 下午1:52
 * \* Description: Read and write data from elasticsearch
 * \
 */

public class ESUserBehaviorDaoImpl implements UserBehaviorDao {

    private static final Log LOG = LogFactory.getLog(ESUserBehaviorDaoImpl.class);

    private  RestHighLevelClient client;

    public ESUserBehaviorDaoImpl(RestHighLevelClient client){
        this.client = client;
    }


    /**
     * The scroll read all document from index
     * @param indexName index name
     * @param type  document type
     * @return List<Map<String, Object>>
     */
    @Override
    public List<Map<String, Object>> readData(String indexName, String type){
        List<Map<String,Object>> rlt = new ArrayList<>();
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        SearchRequest searchRequest = new SearchRequest(indexName);

//		searchRequest.types(this.type);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(matchAllQuery());
        searchRequest.source(searchSourceBuilder);

        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();

            while (searchHits != null && searchHits.length > 0) {
                for (SearchHit hit : searchHits) {
                    rlt.add(hit.getSourceAsMap());
                }
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();

            }
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
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
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rlt;
    }


    /**
     * Save data to es
     * @param mapList
     * @return
     */
    @Override
    public void saveData(List<Map<String,Object>> mapList, String indexName) {
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
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                listener).build();
        // set bulkProcessor handle requests execution.
        BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) ->
                        client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
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
            }
        } catch (Exception e) {
            LOG.error(e);
        }
    }


    }



