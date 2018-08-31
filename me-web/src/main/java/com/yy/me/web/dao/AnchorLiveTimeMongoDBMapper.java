package com.yy.me.web.dao;

import java.util.Date;
import java.util.List;

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

@Repository
public class AnchorLiveTimeMongoDBMapper {

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_REAL_LIVE_SHOW_DURATION = "real_live_show_duration";
    
    public static final String FIELD_UID = "uid";
    public static final String FIELD_LIVE_SHOW_DATE = "liveShowDate";
    public static final String FIELD_DURATION = "duration";
    public static final String FIELD_DURATION_VIEW="durationView";
    
    private static Logger logger = LoggerFactory.getLogger(AnchorLiveTimeMongoDBMapper.class);
    
    
    
    public List<DBObject> getLiveTimeDBObjectByUidAndTime(Long uid, Date startDate, Date endDate) {
        long start  = System.currentTimeMillis();
        List<DBObject> list = Lists.newArrayList();
        QueryBuilder builder = QueryBuilder.start();
        builder.and(FIELD_UID).is(uid);
        if(startDate != null) {
            builder.and(FIELD_LIVE_SHOW_DATE).greaterThanEquals(startDate);
        }
        if(endDate != null) {
            builder.and(FIELD_LIVE_SHOW_DATE).lessThanEquals(endDate);
        }
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_REAL_LIVE_SHOW_DURATION).find(builder.get()).sort(new BasicDBObject(FIELD_LIVE_SHOW_DATE, 1));
        while(cursor.hasNext()) {
            DBObject object  = cursor.next();
            if(object != null) {
                list.add(object);
            }
        }
        logger.info("getLiveTimeDBObjectByUidAndTime cost : {}", (System.currentTimeMillis() - start));
        return list;
    }
    
}
