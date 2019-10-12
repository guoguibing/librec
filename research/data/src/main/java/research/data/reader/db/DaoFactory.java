package research.data.reader.db;

import research.data.reader.db.elasticsearch.ElasticsearchFactory;

/**
 * \* Created with IntelliJ IDEA.
 * \* User: whb
 * \* Description: Connect db factory
 */

public abstract class DaoFactory {


    public static DaoFactory getDaoFactory(String dbName){
        switch (dbName){
            case "elasticsearch":
                return new ElasticsearchFactory();
            case "mongodb":
                return null;

            default:
                return null;
        }
    }


    public abstract UserBehaviorDao getDataModelOperationDto(String urls);



}
