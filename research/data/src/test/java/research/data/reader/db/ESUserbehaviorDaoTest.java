package research.data.reader.db;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * \* Created with IntelliJ IDEA.
 * \* User: whb
 * \* Date: 19-4-9
 * \* Time: 下午3:15
 * \* Description:
 * \
 */

public class ESUserbehaviorDaoTest {

    private UserBehaviorDao userBehaviorDao;

    @Before
    public void setUserBehaviorDaoTest(){
        String urls = "localhost:9200";
        userBehaviorDao = DaoFactory.getDaoFactory("elasticsearch").getDataModelOperationDto(urls);
    }

    @Test
    public void ESReadDataTest(){
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> userBehaviorList = userBehaviorDao.readData("ratings","ratings");
        long endTime = System.currentTimeMillis();
        System.out.println(String.format("Read %s documents take %s millis",userBehaviorList.size(),endTime-startTime));
    }

    @Test
    public void ESWriteDataTest(){
        List<Map<String,Object>> rlt = new ArrayList<>();
        Map<String, Object> userBehavior = new HashMap<>();
        userBehavior.put("userId",136700);
        userBehavior.put("moviesId",1344500);
        userBehavior.put("rating",5);
        userBehavior.put("timestamp", System.currentTimeMillis());
        rlt.add(userBehavior);
        userBehaviorDao.saveData(rlt,"ratings");
    }



}
