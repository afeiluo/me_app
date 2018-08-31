package com.yy.me.web.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.yy.me.enums.CountDownOperType;

import static com.yy.me.mongo.MongoUtil.*;

/**
 * Created by ben on 16/7/20.
 */
@Repository
public class CountdownMongoDBMapper {
    
    public static String COLLECTION_COUNTDOWN = "countdown_info";
    
    public static String FIELD_OBJ_ID = "_id";
    public static String FIELD_LID = "lid";
    public static String FIELD_DURATION = "duration";
    public static String FIELD_TYPE = "type";
    public static String FIELD_LEFTTIME = "leftTime";
    public static String FIELD_UPDATETIME = "updateTime";
    public static String FIELD_UID = "uid";
    
    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;
    

    public void storeCountdownInfo(Long uid, String lid, Long duration, Integer type) {

        DBObject query = new BasicDBObject(FIELD_LID, lid);
        query.put(FIELD_UID, uid);
        mongoTemplate.getDb().getCollection(COLLECTION_COUNTDOWN).remove(query);//删掉旧的。
        DBObject update = new BasicDBObject(FIELD_DURATION, duration);
        update.put(FIELD_TYPE, type);
        update.put(FIELD_UPDATETIME, System.currentTimeMillis());
        update.put(FIELD_LID, lid);
        update.put(FIELD_UID, uid);
        mongoTemplate.getDb().getCollection(COLLECTION_COUNTDOWN)
                .findAndModify(query, null, null, false, new BasicDBObject(OP_SET, update), false, true);

    }

    public void updateCountdownInfo(Long uid, String lid, Integer type, Long leftTime) {
        DBObject query = new BasicDBObject(FIELD_LID, lid);
        query.put(FIELD_UID, uid);
        DBObject update = new BasicDBObject(FIELD_TYPE, type);
        update.put(FIELD_UPDATETIME, System.currentTimeMillis());
        update.put(FIELD_LID, lid);
        update.put(FIELD_UID, uid);
        if (type == CountDownOperType.PAUSE.getValue() && leftTime != null) {
            update.put(FIELD_LEFTTIME, leftTime);
        }
        mongoTemplate.getDb().getCollection(COLLECTION_COUNTDOWN).update(query, new BasicDBObject(OP_SET, update), false, false);
    }

    public Long getDuration(Long uid, String lid) {
        DBObject query = new BasicDBObject(FIELD_LID, lid);
        query.put(FIELD_UID, uid);
        DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_COUNTDOWN).findOne(query);
        if (obj != null) {
            return (Long) obj.get(FIELD_DURATION);
        }
        return 0L;
    }

    public DBObject getDBRecord(Long uid, String lid) {
        DBObject query = new BasicDBObject(FIELD_LID, lid);
        query.put(FIELD_UID, uid);
        return mongoTemplate.getDb().getCollection(COLLECTION_COUNTDOWN).findOne(query);
    }
}
