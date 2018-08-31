package com.yy.me.web.dao;

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
import com.yy.me.time.DateTimeUtil;
import com.yy.me.web.entity.PopupAd;

/**
 * 弹窗广告信息。
 * 
 * 索引:
 * 
 * db.popup_ad.createIndex( { "startTime": 1, "endTime": -1 } )
 * 
 * @author cuixiang
 * 
 */
@Repository
public class PopupAdMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(PopupAdMongoDBMapper.class);
    
    /**
     * 广告弹窗
     */
    public static final String COLLECTION_POPUP_AD_NAME = "popup_ad";
    
    public static final String FIELD_POPUP_AD_TITLE = "title";
    public static final String FIELD_POPUP_AD_IMG_URL = "imgUrl";
    public static final String FIELD_POPUP_AD_JUMP_URL = "jumpUrl";
    public static final String FIELD_POPUP_AD_START_TIME = "startTime";
    public static final String FIELD_POPUP_AD_END_TIME = "endTime";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(PopupAd popupAd) throws Exception {
        if (popupAd == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, popupAd, "aid");
            mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).save(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("save - cost time {}ms, popupAd: {}", (System.currentTimeMillis() - start), popupAd);
        }
    }

    public void remove(String aid) throws Exception {
        if (StringUtils.isBlank(aid)) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = new BasicDBObject();
            obj.put(FIELD_OBJ_ID, new ObjectId(aid));
            mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).remove(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("remove - cost time {}ms, aid: {}", (System.currentTimeMillis() - start), aid);
        }
    }

    public PopupAd findById(String aid) throws Exception {
        if (StringUtils.isBlank(aid)) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME)
                    .findOne(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(aid)));
            return db2JavaObj(mongoTemplate, PopupAd.class, obj, "aid");
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findById - cost time {}ms, aid: {}", (System.currentTimeMillis() - start), aid);
        }
    }

    public List<PopupAd> findByTime(Date curTime) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_POPUP_AD_END_TIME).greaterThanEquals(curTime);

            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).find(query.get());
            cursor.sort(new BasicDBObject(FIELD_POPUP_AD_START_TIME, 1));

            List<PopupAd> adList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                adList.add(db2JavaObj(mongoTemplate, PopupAd.class, obj, "aid"));
            }

            return adList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByTime - cost time {}ms, curTime: {}", (System.currentTimeMillis() - start),
                    DateTimeUtil.formatDateTime(curTime));
        }
    }

    public long count() throws Exception {
        return mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).count();
    }

    public List<PopupAd> findByPage(int pageOffset, int pageSize) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).find();
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }

            if (pageSize > 0) {
                cursor.limit(pageSize);
            }

            DBObject sortObj = new BasicDBObject(FIELD_POPUP_AD_END_TIME, -1);
            cursor.sort(sortObj);

            List<PopupAd> adList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                adList.add(db2JavaObj(mongoTemplate, PopupAd.class, obj, "aid"));
            }

            return adList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByPage - cost time {}ms, pageOffset: {}, pageSize: {}",
                    (System.currentTimeMillis() - start), pageOffset, pageSize);
        }
    }

    public boolean checkDurationExists(String aid, Date startTime, Date endTime) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder startTimeQuery = QueryBuilder.start();
            startTimeQuery.and(FIELD_POPUP_AD_START_TIME).lessThanEquals(startTime);
            startTimeQuery.and(FIELD_POPUP_AD_END_TIME).greaterThanEquals(startTime);

            QueryBuilder endTimeQuery = QueryBuilder.start();
            endTimeQuery.and(FIELD_POPUP_AD_START_TIME).lessThanEquals(endTime);
            endTimeQuery.and(FIELD_POPUP_AD_END_TIME).greaterThanEquals(endTime);

            QueryBuilder query = QueryBuilder.start();
            if (StringUtils.isNotBlank(aid)) {
                query.and(FIELD_OBJ_ID).notEquals(new ObjectId(aid));
            }
            
            query.or(startTimeQuery.get());
            query.or(endTimeQuery.get());

            long count = mongoTemplate.getDb().getCollection(COLLECTION_POPUP_AD_NAME).count(query.get());

            return count > 0;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("checkDurationExists - cost time {}ms, startTime: {}, endTime: {}",
                    (System.currentTimeMillis() - start), DateTimeUtil.formatDateTime(startTime),
                    DateTimeUtil.formatDateTime(endTime));
        }
    }

}
