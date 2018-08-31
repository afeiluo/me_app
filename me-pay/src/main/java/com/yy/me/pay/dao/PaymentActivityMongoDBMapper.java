package com.yy.me.pay.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteConcern;
import com.yy.me.pay.entity.PaymentActivity;

/**
 * 充值活动。
 * 
 * 
 * @author cuixiang
 * 
 */
@Repository
public class PaymentActivityMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(PaymentActivityMongoDBMapper.class);
    
    /**
     * 充值活动集合
     */
    public static final String COLLECTION_PAYMENT_ACTIVITY_NAME = "payment_activity";
    
    public static final String FIELD_PAYMENT_ACTIVITY_ID = "actId";
    public static final String FIELD_PAYMENT_ACTIVITY_NAME = "name";
    public static final String FIELD_PAYMENT_ACTIVITY_POSITION = "position";
    public static final String FIELD_PAYMENT_ACTIVITY_PROPID = "propId";
    public static final String FIELD_PAYMENT_ACTIVITY_PRODUCT_ID = "productId";
    public static final String FIELD_PAYMENT_ACTIVITY_BUY_COUNT_PER_USER = "buyCountPerUser";
    public static final String FIELD_PAYMENT_ACTIVITY_TOTAL_COUNT = "totalCount";
    public static final String FIELD_PAYMENT_ACTIVITY_SALED_COUNT = "saledCount";
    public static final String FIELD_PAYMENT_ACTIVITY_DISPLAY_SALED_COUNT = "displaySaledCount";
    public static final String FIELD_PAYMENT_ACTIVITY_START_TIME = "startTime";
    public static final String FIELD_PAYMENT_ACTIVITY_END_TIME = "endTime";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(PaymentActivity activity) throws Exception {
        if (activity == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, activity, FIELD_PAYMENT_ACTIVITY_ID);
            mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_NAME).save(obj);
        } finally {
            logger.info("save - cost time {}ms, PaymentActivity: {}", (System.currentTimeMillis() - start), activity);
        }
    }

    public List<PaymentActivity> getValidActivity() throws Exception {
        long start = System.currentTimeMillis();

        try {
            Date currDate = new Date();

            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACTIVITY_START_TIME).lessThanEquals(currDate);
            query.and(FIELD_PAYMENT_ACTIVITY_END_TIME).greaterThanEquals(currDate);

            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_NAME).find(query.get());
            cursor.sort(new BasicDBObject(FIELD_PAYMENT_ACTIVITY_POSITION, 1));

            List<PaymentActivity> actList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                actList.add(db2JavaObj(mongoTemplate, PaymentActivity.class, obj, FIELD_PAYMENT_ACTIVITY_ID));
            }

            return actList;
        } finally {
            logger.info("getValidActivity - cost time {}ms", (System.currentTimeMillis() - start));
        }
    }

    public PaymentActivity findById(String actId) throws Exception {
        if (StringUtils.isBlank(actId)) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_NAME)
                    .findOne(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(actId)));
            return db2JavaObj(mongoTemplate, PaymentActivity.class, obj, FIELD_PAYMENT_ACTIVITY_ID);
        } finally {
            logger.info("findById - cost time {}ms, actId: {}", (System.currentTimeMillis() - start), actId);
        }
    }
    
    public PaymentActivity findByPropId(int propId) throws Exception {
        if (propId <= 0) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            Date currDate = new Date();

            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_PAYMENT_ACTIVITY_START_TIME).lessThanEquals(currDate);
            query.and(FIELD_PAYMENT_ACTIVITY_END_TIME).greaterThanEquals(currDate);
            query.and(FIELD_PAYMENT_ACTIVITY_PROPID).is(propId);

            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_NAME).findOne(query.get());
            return db2JavaObj(mongoTemplate, PaymentActivity.class, obj, FIELD_PAYMENT_ACTIVITY_ID);
        } finally {
            logger.info("findByPropId - cost time {}ms, propId: {}", (System.currentTimeMillis() - start), propId);
        }
    }

    public void increaseSoldCount(String actId, long saledCount, long displaySaledCount) throws Exception {
        if (StringUtils.isBlank(actId) || saledCount <= 0L || displaySaledCount <= 0L) {
            return;
        }

        DBObject query = new BasicDBObject(FIELD_OBJ_ID, new ObjectId(actId));

        BasicDBObject update = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_PAYMENT_ACTIVITY_SALED_COUNT,
                saledCount).append(FIELD_PAYMENT_ACTIVITY_DISPLAY_SALED_COUNT, displaySaledCount));

        mongoTemplate.getDb().getCollection(COLLECTION_PAYMENT_ACTIVITY_NAME)
                .update(query, update, false, false, WriteConcern.ACKNOWLEDGED);
    }

}
