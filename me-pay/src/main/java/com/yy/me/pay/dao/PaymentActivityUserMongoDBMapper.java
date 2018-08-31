package com.yy.me.pay.dao;

import com.mongodb.*;
import com.yy.me.pay.entity.PaymentActivityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;

import static com.yy.me.mongo.MongoUtil.*;

/**
 * 参与充值活动的用户信息.
 * 
 * 索引：
 * 
 * db.payment_activity_user.createIndex( { "uid": 1, "propId": 1 } )
 * 
 * db.payment_activity_user_lock.createIndex( { "uid": 1, "propId": 1 }, { unique: true } )
 * db.payment_activity_user_lock.createIndex( { "lockTime": 1 }, { expireAfterSeconds: 600 } )
 * 
 * @author cuixiang
 * 
 */
@Repository
public class PaymentActivityUserMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(PaymentActivityUserMongoDBMapper.class);
    
    /**
     * 参加充值活动的用户集合
     */
    public static final String COLLECTION_PAYMENT_ACTIVITY_USER_NAME = "payment_activity_user";
    
    public static final String FIELD_PAYMENT_ACT_USER_ID = "auId";
    public static final String FIELD_PAYMENT_ACT_USER_UID = "uid";
    public static final String FIELD_PAYMENT_ACT_USER_PROPID = "propId";
    public static final String FIELD_PAYMENT_ACT_USER_BUY_TIME = "buyTime";
    public static final String FIELD_PAYMENT_ACT_USER_SEQ = "seq";
    
    /**
     * 参加充值活动的用户锁集合
     */
    public static final String COLLECTION_PAYMENT_ACTIVITY_USER_LOCK_NAME = "payment_activity_user_lock";
    
    public static final String FIELD_PAYMENT_ACT_USER_LOCK_TIME = "lockTime";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public PaymentActivityUser insert(PaymentActivityUser activityUser) throws Exception {
        if (activityUser == null) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACT_USER_UID).is(activityUser.getUid());
            query.and(FIELD_PAYMENT_ACT_USER_PROPID).is(activityUser.getPropId());
            query.and(FIELD_PAYMENT_ACT_USER_SEQ).is(activityUser.getSeq());

            DBObject insertObj = javaObj2Db(mongoTemplate, activityUser, FIELD_PAYMENT_ACT_USER_ID);

            DBObject existsObj = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_USER_NAME)
                    .findAndModify(query.get(), null, null, false, insertObj, false, true, WriteConcern.ACKNOWLEDGED);

            return db2JavaObj(mongoTemplate, PaymentActivityUser.class, existsObj, FIELD_PAYMENT_ACT_USER_ID);
        } finally {
            logger.info("insert - cost time {}ms, PaymentActivityUser: {}", (System.currentTimeMillis() - start),
                    activityUser);
        }
    }

    public long getUserBuyCount(long uid, int propId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACT_USER_UID).is(uid);
            query.and(FIELD_PAYMENT_ACT_USER_PROPID).is(propId);

            return mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_USER_NAME).count(query.get());
        } finally {
            logger.info("getUserBuyCount - cost time {}ms", (System.currentTimeMillis() - start));
        }
    }

    public boolean addUserLock(long uid, int propId) throws Exception {
        if (uid <= 0L || propId <= 0) {
            return false;
        }

        long start = System.currentTimeMillis();

        try {
            BasicDBObject insertObj = new BasicDBObject(FIELD_PAYMENT_ACT_USER_UID, uid);
            insertObj.append(FIELD_PAYMENT_ACT_USER_PROPID, propId);
            insertObj.append(FIELD_PAYMENT_ACT_USER_LOCK_TIME, new Date());

            mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_USER_LOCK_NAME).insert(insertObj);

            return true;
        } catch (DuplicateKeyException e) {
            logger.debug("Ignore exists lock. uid: " + uid + ", propId: " + propId, e);
            return false;
        } finally {
            logger.info("addUserLock - cost time {}ms, uid: {}, propId: {}", (System.currentTimeMillis() - start), uid,
                    propId);
        }
    }

    public boolean checkUserLock(long uid, int propId) throws Exception {
        if (uid <= 0L || propId <= 0) {
            return false;
        }

        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACT_USER_UID).is(uid);
            query.and(FIELD_PAYMENT_ACT_USER_PROPID).is(propId);

            long count = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_USER_LOCK_NAME)
                    .count(query.get());

            return count > 0;
        } finally {
            logger.info("checkUserLock - cost time {}ms, uid: {}, propId: {}", (System.currentTimeMillis() - start),
                    uid, propId);
        }
    }

    public void removeUserLock(long uid, int propId) throws Exception {
        if (uid <= 0L || propId <= 0) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACT_USER_UID).is(uid);
            query.and(FIELD_PAYMENT_ACT_USER_PROPID).is(propId);

            mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_USER_LOCK_NAME).remove(query.get());
        } finally {
            logger.info("removeUserLock - cost time {}ms, uid: {}, propId: {}", (System.currentTimeMillis() - start),
                    uid, propId);
        }
    }

}
