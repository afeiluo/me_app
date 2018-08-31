package com.yy.me.pay.dao;

import static com.yy.me.mongo.MongoUtil.*;

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
import com.yy.me.pay.entity.GiftBagRecord;

/**
 * 送出的礼包信息.
 *
 * 索引:
 *
 * db.gift_bag_record.createIndex( { "seq": 1 }, { unique: true } )
 * db.gift_bag_record.createIndex( { "bagId": 1, "lid": 1 } )
 * db.gift_bag_record.createIndex( { "lid": 1 } )
 * db.gift_bag_record.createIndex( { "usedTime": 1 }, { expireAfterSeconds: 604800 } )
 * 
 * 数据保存7天之后删除.
 * 
 * Created by Chris on 16/1/20.
 */
@Repository
public class GiftBagRecordMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(GiftBagRecordMongoDBMapper.class);
    
    /**
     * 送出的礼包记录
     */
    public static final String COLLECTION_GIFT_BAG_RECORD_NAME = "gift_bag_record";
    
    public static final String FIELD_GIFT_BAG_RECORD_BAGID = "bagId";
    public static final String FIELD_GIFT_BAG_RECORD_SEQ = "seq";
    public static final String FIELD_GIFT_BAG_RECORD_UID = "uid";
    public static final String FIELD_GIFT_BAG_RECORD_RECVUID = "recvUid";
    public static final String FIELD_GIFT_BAG_RECORD_LID = "lid";
    public static final String FIELD_GIFT_BAG_RECORD_USEDTIME = "usedTime";
    public static final String FIELD_GIFT_BAG_RECORD_PROPID = "propId";
    public static final String FIELD_GIFT_BAG_RECORD_PROPCOUNT = "propCount";
    public static final String FIELD_GIFT_BAG_RECORD_AMOUNT = "amount";
    public static final String FIELD_GIFT_BAG_RECORD_PLATFORM = "platform";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(GiftBagRecord giftBag) throws Exception {
        if (giftBag == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, giftBag, "bagId");
            mongoTemplate.getDb().getCollection(COLLECTION_GIFT_BAG_RECORD_NAME).save(obj);
        } finally {
            logger.info("save - cost time {}ms, giftBag: {}", (System.currentTimeMillis() - start), giftBag);
        }
    }

    public List<GiftBagRecord> findGiftByLive(String lid, String lastBagId, int limit) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_GIFT_BAG_RECORD_LID).is(lid);

            if (StringUtils.isNotBlank(lastBagId)) {
                query.and(FIELD_GIFT_BAG_RECORD_BAGID).lessThan(new ObjectId(lastBagId));
            }

            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GIFT_BAG_RECORD_NAME).find(query.get());
            if (limit > 0) {
                cursor.limit(limit);
            }

            DBObject sortObj = new BasicDBObject(FIELD_GIFT_BAG_RECORD_BAGID, -1);
            cursor.sort(sortObj);

            List<GiftBagRecord> giftBagList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                giftBagList.add(db2JavaObj(mongoTemplate, GiftBagRecord.class, obj, "bagId"));
            }

            return giftBagList;
        } finally {
            logger.info("findGiftByLive - cost time {}ms, lid: {}, lastBagId: {}", (System.currentTimeMillis() - start), lid, lastBagId);
        }
    }
    
    public long count(String lid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBObject obj = new BasicDBObject(FIELD_GIFT_BAG_RECORD_LID, lid);
            return mongoTemplate.getDb().getCollection(COLLECTION_GIFT_BAG_RECORD_NAME).count(obj);
        } finally {
            logger.info("count - cost time {}ms, lid: {}", (System.currentTimeMillis() - start), lid);
        }
    }
    
    public GiftBagRecord findLastByLive(String lid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_GIFT_BAG_RECORD_LID).is(lid);

            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GIFT_BAG_RECORD_NAME).find(query.get());
            cursor.sort(new BasicDBObject(FIELD_GIFT_BAG_RECORD_USEDTIME, -1)).limit(1);

            GiftBagRecord giftBag = null;

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                giftBag = db2JavaObj(mongoTemplate, GiftBagRecord.class, obj, FIELD_GIFT_BAG_RECORD_BAGID);
            }

            return giftBag;
        } finally {
            logger.info("findLastByLive - cost time {}ms, lid: {}", (System.currentTimeMillis() - start), lid);
        }
    }
}
