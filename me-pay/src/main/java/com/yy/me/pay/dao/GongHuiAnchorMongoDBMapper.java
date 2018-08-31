package com.yy.me.pay.dao;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.yy.me.mongo.MongoUtil;

/**
 *
 * 索引：
 *
 * db.gonghui_anchor.createIndex( { "ghUid": 1 } )
 * db.gonghui_anchor.createIndex( { "ghUid": 1, "anchorUid": 1 }, { unique: true } )
 *
 * Created by Chris on 16/5/7.
 */
@Repository
public class GongHuiAnchorMongoDBMapper {
    private static final Logger logger = LoggerFactory.getLogger(GongHuiAnchorMongoDBMapper.class);

    public static final String COLLECTION_GONGHUI_ANCHOR_NAME = "gonghui_anchor";

    public static final String FIELD_GONGHUI_UID = "ghUid";
    public static final String FIELD_GONGHUI_ANCHOR_UID = "anchorUid";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public Set<Long> findAnchorByGH(long ghUid) throws Exception {
        long start = System.currentTimeMillis();

        try {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_GONGHUI_ANCHOR_NAME).find(new BasicDBObject(FIELD_GONGHUI_UID, ghUid));

            Set<Long> anchorSet = Sets.newHashSet();

            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                anchorSet.add(MongoUtil.getLong(obj.get(FIELD_GONGHUI_ANCHOR_UID), 0L));
            }

            return anchorSet;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("findAnchorByGH - cost time {}ms", (System.currentTimeMillis() - start));
        }
    }
}
