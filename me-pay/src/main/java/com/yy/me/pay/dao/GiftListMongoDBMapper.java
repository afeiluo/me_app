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
import com.yy.me.pay.entity.Gift;

@Repository
public class GiftListMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(GiftListMongoDBMapper.class);
    
    public static final String COLLECTION_GIFT_LIST_NAME = "gift_list";
    
    public static final String FIELD_GIFT_LIST_ID = "gid";
    public static final String FIELD_GIFT_LIST_NAME = "name";
    public static final String FIELD_GIFT_LIST_PROP_ID = "propId";
    public static final String FIELD_GIFT_LIST_COMBO = "combo";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(Gift gift) throws Exception {
        if (gift == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, gift, FIELD_GIFT_LIST_ID);
            mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME).save(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("save - cost time {}ms, gift: {}", (System.currentTimeMillis() - start), gift);
        }
    }

    public void remove(String gid) throws Exception {
        if (StringUtils.isBlank(gid)) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = new BasicDBObject();
            obj.put(FIELD_OBJ_ID, new ObjectId(gid));
            mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME).remove(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("remove - cost time {}ms, gid: {}", (System.currentTimeMillis() - start), gid);
        }
    }

    public long count() throws Exception {
        return mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME).count();
    }

    public List<Gift> findByPage(int pageOffset, int pageSize) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME).find();
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }

            if (pageSize > 0) {
                cursor.limit(pageSize);
            }

            List<Gift> giftList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                giftList.add(db2JavaObj(mongoTemplate, Gift.class, obj, "gid"));
            }

            return giftList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByPage - cost time {}ms, pageOffset: {}, pageSize: {}",
                    (System.currentTimeMillis() - start), pageOffset, pageSize);
        }
    }
    
    public Gift findByPropId(int propId) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME)
                    .findOne(new BasicDBObject(FIELD_GIFT_LIST_PROP_ID, propId));

            return db2JavaObj(mongoTemplate, Gift.class, obj, FIELD_GIFT_LIST_ID);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByPropId - cost time {}ms, propId: {}", (System.currentTimeMillis() - start), propId);
        }
    }

    public List<Gift> findAll() throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GIFT_LIST_NAME).find();

            List<Gift> giftList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                giftList.add(db2JavaObj(mongoTemplate, Gift.class, obj, FIELD_GIFT_LIST_ID));
            }

            return giftList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findAll - cost time {}ms", (System.currentTimeMillis() - start));
        }
    }
}
