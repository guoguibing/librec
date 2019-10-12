package research.data.reader.db.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import research.data.reader.db.DaoFactory;
import research.data.reader.db.UserBehaviorDao;

import java.util.ArrayList;
import java.util.List;

/**
 * \* Created with IntelliJ IDEA.
 * \* User: Wanghuobin
 * \* Date: 19-4-4
 * \* Time: 下午2:07
 * \* Description: connect es factory
 * \
 */

public class ElasticsearchFactory extends DaoFactory {
    private static final Log LOG = LogFactory.getLog(ElasticsearchFactory.class);
    //  user name
    private static final String userName = "librec";
    //  password
    private static final  String password = "123456";

    //Connect time  para
    private static final int CONNECT_TIME_OUT = 1000;
    private static final int SOCKET_TIME_OUT = 30000;
    private static final int CONNECTION_REQUEST_TIME_OUT = 500;

    //Connect num
    private static final int MAX_CONNECT_NUM = 100;
    private static final int MAX_CONNECT_PER_ROUTE = 100;

    private  static  RestClientBuilder builder;
    private  static RestHighLevelClient  highLevelClient;


    @Override
    public UserBehaviorDao getDataModelOperationDto(String urls){
        List<HttpHost> httpHosts = new ArrayList<>();
        for(String url: urls.split(",")){
            String[] hostPort = url.split(":");
            httpHosts.add(new HttpHost(hostPort[0], Integer.parseInt(hostPort[1])));
        }
        if(highLevelClient == null){
            if (httpHosts != null && httpHosts.size()>0) {
                // Create a connection for each es node.
                builder = RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()]));
                setConnectTimeOutConfig(); // set connect time
                setMutiConnectConfig(); //set connect num
                // Initialize the high-level REST client which sends the actual requests to ElasticSearch.
                highLevelClient = new RestHighLevelClient(builder);
            }else {
                LOG.error(String.format("The  url = %s format error!(eg:localhost:9200,localhost:9222)", urls));
            }
        }
        return new ESUserBehaviorDaoImpl(highLevelClient);
    }

    /**
     * get rest high level client
     */
    public static RestHighLevelClient getHighLevelClient() {
        return highLevelClient;
    }

    /**
     * close rest high level client
     */
    public void closeClient(){
        try{
            highLevelClient.close();
        } catch (Exception e){
            LOG.error(e);
        }
    }


    /**
     *  Set async connect time
     */
    public void setConnectTimeOutConfig() {
        builder.setRequestConfigCallback(
            new RestClientBuilder.RequestConfigCallback() {
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                requestConfigBuilder.setConnectTimeout(CONNECT_TIME_OUT);
                requestConfigBuilder.setSocketTimeout(SOCKET_TIME_OUT);
                requestConfigBuilder.setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT);
                return requestConfigBuilder;
            }});
         }


    /**
     * Set async connect num
     */
    public void setMutiConnectConfig() {
        builder.setHttpClientConfigCallback(
            new RestClientBuilder.HttpClientConfigCallback() {
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder.setMaxConnTotal(MAX_CONNECT_NUM);
                httpClientBuilder.setMaxConnPerRoute(MAX_CONNECT_PER_ROUTE);
                return httpClientBuilder;
            }});
        }


    /**
     * Set async connect user name and password
     */
    public  void  setConnectUserPassword(){
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        builder.setHttpClientConfigCallback(
                new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.disableAuthCaching();
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });
        }






}
