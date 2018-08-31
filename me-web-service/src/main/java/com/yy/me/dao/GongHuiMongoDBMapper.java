package com.yy.me.dao;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.yy.me.entity.GongHui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.yy.me.mongo.MongoUtil.*;

/**
 * Created by Chris on 16/5/7.
 */
@Repository
public class GongHuiMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(GongHuiMongoDBMapper.class);

    public static final String COLLECTION_GONGHUI_INFO_NAME = "gonghui_info";

    public static final String ID_FIELD = "ghUid";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public void save(GongHui gongHui) throws Exception {
        if (gongHui == null) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = javaObj2Db(mongoTemplate, gongHui, ID_FIELD);
            mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME).save(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("save - cost time {}ms, gongHui: {}", (System.currentTimeMillis() - start), gongHui);
        }
    }

    public void remove(long ghUid) throws Exception {
        if (ghUid <= 0L) {
            return;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = new BasicDBObject();
            obj.put(FIELD_OBJ_ID, ghUid);
            mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME).remove(obj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("remove - cost time {}ms, ghUid: {}", (System.currentTimeMillis() - start), ghUid);
        }
    }

    public GongHui findById(long ghUid) throws Exception {
        if (ghUid <= 0L) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME)
                    .findOne(new BasicDBObject(FIELD_OBJ_ID, ghUid));
            return db2JavaObj(mongoTemplate, GongHui.class, obj, ID_FIELD);
        } finally {
            logger.info("findById - cost time {}ms, ghUid: {}", (System.currentTimeMillis() - start), ghUid);
        }
    }

    public long count() throws Exception {
        return mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME).count();
    }

    public List<GongHui> findByPage(int pageOffset, int pageSize) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME).find();
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }

            if (pageSize > 0) {
                cursor.limit(pageSize);
            }

            List<GongHui> gongHuiList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                gongHuiList.add(db2JavaObj(mongoTemplate, GongHui.class, obj, ID_FIELD));
            }

            return gongHuiList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findByPage - cost time {}ms, pageOffset: {}, pageSize: {}",
                    (System.currentTimeMillis() - start), pageOffset, pageSize);
        }
    }

    public List<GongHui> findAll() throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_INFO_NAME).find();

            List<GongHui> gongHuiList = Lists.newArrayList();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                gongHuiList.add(db2JavaObj(mongoTemplate, GongHui.class, obj, ID_FIELD));
            }

            return gongHuiList;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findAll - cost time {}ms", (System.currentTimeMillis() - start));
        }
    }
}
