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
import com.yy.me.web.entity.SplashInfo;

/**
 * 闪屏页信息。
 * 
 * 索引:
 * 
 * db.splash_info.createIndex( { "startTime": 1, "endTime": -1 } )
 * 
 * @author cuixiang
 * 
 */
@Repository
public class SplashInfoMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(SplashInfoMongoDBMapper.class);
    
    /**
     * 闪屏页信息
     */
    public static final String COLLECTION_SPLASH_INFO_NAME = "splash_info";
    
    public static final String FIELD_SPLASH_TITLE = "title";
    public static final String FIELD_SPLASH_IMG_URL = "imgUrl";
    public static final String FIELD_SPLASH_START_TIME = "startTime";
    public static final String FIELD_SPLASH_END_TIME = "endTime";
    public static final String FIELD_SPLASH_CHANNELID = "channelId";

    @Autowired
    @Qualifier("mongoTemplateConfig")
    private MongoTemplate mongoTemplate;

    private final String SPLASH_OFFICE_CHANNEL = "official";

    public void save(SplashInfo splashInfo) throws Exception {
        if (splashInfo == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, splashInfo, "sid");
            mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).save(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("save - cost time {}ms, splashInfo: {}", (System.currentTimeMillis() - start), splashInfo);
        }
    }

    public void remove(String sid) throws Exception {
        if (StringUtils.isBlank(sid)) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = new BasicDBObject();
            obj.put(FIELD_OBJ_ID, new ObjectId(sid));
            mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).remove(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("remove - cost time {}ms, sid: {}", (System.currentTimeMillis() - start), sid);
        }
    }

    public SplashInfo findById(String sid) throws Exception {
        if (StringUtils.isBlank(sid)) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME)
                    .findOne(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(sid)));
            return db2JavaObj(mongoTemplate, SplashInfo.class, obj, "sid");
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findById - cost time {}ms, sid: {}", (System.currentTimeMillis() - start), sid);
        }
    }

    public List<SplashInfo> findByTime(String channelId, Date curTime) throws Exception {
        long start = System.currentTimeMillis();

        try {
            QueryBuilder query = QueryBuilder.start();
            query.and(FIELD_SPLASH_END_TIME).greaterThanEquals(curTime);
            // ios会传空，但android会传official，这里统一处理
            if (StringUtils.isNotBlank(channelId) && (!SPLASH_OFFICE_CHANNEL.equals(channelId))) {// 有渠道
                query.and(FIELD_SPLASH_CHANNELID).is(channelId);
            } else {// 没有渠道 TODO
                query.or(new BasicDBObject(FIELD_SPLASH_CHANNELID, new BasicDBObject(OP_EXISTS, false)),
                        new BasicDBObject(FIELD_SPLASH_CHANNELID, ""));
            }
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).find(query.get());
            if (StringUtils.isNotBlank(channelId) && !cursor.hasNext()) {// 没有查到渠道闪屏
                query.and(FIELD_SPLASH_CHANNELID).is("");
                cursor = mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).find(query.get());
            }
            cursor.sort(new BasicDBObject(FIELD_SPLASH_START_TIME, 1));
            List<SplashInfo> splashList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                splashList.add(db2JavaObj(mongoTemplate, SplashInfo.class, obj, "sid"));
            }

            return splashList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByTime - cost time {}ms, curTime: {}", (System.currentTimeMillis() - start), DateTimeUtil.formatDateTime(curTime));
        }
    }

    public long count() throws Exception {
        return mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).count();
    }

    public List<SplashInfo> findByPage(int pageOffset, int pageSize) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).find();
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }

            if (pageSize > 0) {
                cursor.limit(pageSize);
            }

            DBObject sortObj = new BasicDBObject(FIELD_SPLASH_END_TIME, -1);
            cursor.sort(sortObj);

            List<SplashInfo> splashList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                splashList.add(db2JavaObj(mongoTemplate, SplashInfo.class, obj, "sid"));
            }

            return splashList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByPage - cost time {}ms, pageOffset: {}, pageSize: {}", (System.currentTimeMillis() - start), pageOffset, pageSize);
        }
    }

    public boolean checkDurationExists(String sid, Date startTime, Date endTime, String channelId) throws Exception {

        long start = System.currentTimeMillis();

        try {
            QueryBuilder startTimeQuery = QueryBuilder.start();
            startTimeQuery.and(FIELD_SPLASH_START_TIME).lessThanEquals(startTime);
            startTimeQuery.and(FIELD_SPLASH_END_TIME).greaterThanEquals(startTime);

            QueryBuilder endTimeQuery = QueryBuilder.start();
            endTimeQuery.and(FIELD_SPLASH_START_TIME).lessThanEquals(endTime);
            endTimeQuery.and(FIELD_SPLASH_END_TIME).greaterThanEquals(endTime);

            QueryBuilder query = QueryBuilder.start();
            if (StringUtils.isNotBlank(sid)) {
                query.and(FIELD_OBJ_ID).notEquals(new ObjectId(sid));
            }
            if (StringUtils.isNotBlank(channelId)) {
                query.and(FIELD_SPLASH_CHANNELID).is(channelId);
            } else {
                QueryBuilder channelIdBuilder = QueryBuilder.start();
                channelIdBuilder.or(new BasicDBObject(FIELD_SPLASH_CHANNELID, new BasicDBObject(OP_EXISTS, false)), new BasicDBObject(
                        FIELD_SPLASH_CHANNELID, ""));
                query.and(channelIdBuilder.get());
            }

            query.or(startTimeQuery.get());
            query.or(endTimeQuery.get());

            long count = mongoTemplate.getDb().getCollection(COLLECTION_SPLASH_INFO_NAME).count(query.get());

            return count > 0;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("checkDurationExists - cost time {}ms, startTime: {}, endTime: {}", (System.currentTimeMillis() - start),
                    DateTimeUtil.formatDateTime(startTime), DateTimeUtil.formatDateTime(endTime));
        }
    }
}
