package research.data.reader.db;


import java.util.List;
import java.util.Map;

public interface UserBehaviorDao {

    /**
     * The scroll read all document from index
     * @param indexName index name
     * @param type  document type
     * @return List<Map<String, Object>>
     */
    List<Map<String, Object>> readData(String indexName, String type);


//    /**
//     * The scroll read  document by condition　from index　
//     * @param indexName index name
//     * @param type  document type
//     * @return List<Map<String, Object>>
//     */
//    List<Map<String, Object>> readData(String indexName, String type, Map<String, Object> condition);

    void saveData(List<Map<String, Object>> mapList, String indexName);











}
