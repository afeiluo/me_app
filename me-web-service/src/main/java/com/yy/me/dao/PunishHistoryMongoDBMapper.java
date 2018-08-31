package com.yy.me.dao;

import static com.yy.me.mongo.MongoUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.DBObject;
import com.yy.me.entity.PunishHistory;

/**
 * 处罚历史记录。
 *
 * db.punish_history.createIndex( { "uid": 1, "punishDate": -1 } )
 *
 * Created by Chris on 16/6/15.
 */
@Repository
public class PunishHistoryMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(PunishHistoryMongoDBMapper.class);
    
    /**
     * 处罚记录
     */
    public static final String COLLECTION_PUNISH_HISTORY_NAME = "punish_history";

    @Autowired
    @Qualifier("mongoTemplateUser")
    private MongoTemplate userMongoTemplate;

    public void create(PunishHistory history) throws Exception {
        if (history == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {

            DBObject obj = javaObj2Db(userMongoTemplate, history, "punishId");
            userMongoTemplate.getDb().getCollection(COLLECTION_PUNISH_HISTORY_NAME).insert(obj);
        } finally {
            logger.info("create - cost time {}ms, PunishHistory: {}", (System.currentTimeMillis() - start),
                    history);
        }
    }
}
