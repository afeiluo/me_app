package com.yy.me.open.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteConcern;
import com.yy.me.open.entity.LiveShowWaterMark;

/**
 * Created by wangke on 2016/4/21.
 */
@Repository
public class LiveShowWaterMarkMongoDBMapper {

    public static final String FIELD_WM_ID = "wmId";
    public static final String FIELD_WM_START_TIME = "startTime";
    public static final String FIELD_WM_END_TIME = "endTime";
    public static final String FIELD_WM_START_DATE = "startDate";
    public static final String FIELD_WM_END_DATE = "endDate";
    public static final String FIELD_WM_WHITE_LIST = "whiteList";
    public static final String COLLECTION_LIVE_SHOW_WATER_MARK_NAME = "live_show_water_mark";
    
    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public List<LiveShowWaterMark> getCurrentWaterMark() throws Exception {
        List<LiveShowWaterMark> retList = Lists.newArrayList();
        DateTime now = new DateTime();
        long time = now.getMillisOfDay();
        long date = now.getMillis() - time;
        QueryBuilder query = QueryBuilder.start(FIELD_WM_START_TIME).lessThanEquals(time).and(FIELD_WM_END_TIME).greaterThanEquals(time).and(FIELD_WM_START_DATE)
                .lessThanEquals(date).and(FIELD_WM_END_DATE).greaterThanEquals(date);
        Cursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).find(query.get());
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            retList.add(db2JavaObj(mongoTemplate, LiveShowWaterMark.class, obj, FIELD_WM_ID));
        }
        return retList;
    }

    public List<LiveShowWaterMark> findByPage(int offset, int pageSize) throws Exception {
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).find();
        if (offset > 0) {
            cursor.skip(offset);
        }

        if (pageSize > 0) {
            cursor.limit(pageSize);
        }

        List<LiveShowWaterMark> messageList = Lists.newArrayList();
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            messageList.add(db2JavaObj(mongoTemplate, LiveShowWaterMark.class, obj, FIELD_WM_ID));
        }
        return messageList;
    }

    public long count() {
        return mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).count();
    }


    public String save(LiveShowWaterMark bean) throws Exception {
        if (bean == null) {
            return null;
        }
        if(StringUtils.isEmpty(bean.getWmId())){
            return insert(bean);
        }
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(bean.getWmId()));
        DBObject obj = javaObj2Db(mongoTemplate, bean, FIELD_WM_ID);
        obj.removeField(FIELD_OBJ_ID);
        BasicDBObject update=new BasicDBObject(OP_SET,obj);
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).update(query.get(),update,true,false);
        return bean.getWmId();
    }

    public String insert(LiveShowWaterMark bean) throws Exception {
        if (bean == null) {
            return null;
        }
        DBObject obj = javaObj2Db(mongoTemplate, bean, FIELD_WM_ID);
        obj.removeField(FIELD_OBJ_ID);
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).save(obj,WriteConcern.ACKNOWLEDGED).getUpsertedId();
        return ((ObjectId) obj.get(FIELD_OBJ_ID)).toHexString();
    }

    public void setWhitelist(String id, List<Long> uids) throws Exception {
        List<Long> existUids = fetchWhiteListUser(id);
        for (Long uid : uids) {
            if (!existUids.contains(uid)) {
                existUids.add(uid);
            }
        }
        insertWhiteList(id, existUids);
    }


    public void addWhiteList(String id, List<Long> uids) {
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(id));
        Update update=new Update().addToSet(FIELD_WM_WHITE_LIST,new BasicDBObject("$each",uids));
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME)
                .update(query.get(), update.getUpdateObject(), false, false, WriteConcern.ACKNOWLEDGED);
    }

    public void delWhiteList(String id, List<Long> uids) {
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(id));
        Update update=new Update().pullAll(FIELD_WM_WHITE_LIST,uids.toArray());
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME)
                .update(query.get(), update.getUpdateObject(), false, false, WriteConcern.ACKNOWLEDGED);
    }

    public void insertWhiteList(String id, List<Long> uids) {
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(id));
        BasicDBObject setCommand = new BasicDBObject();
        setCommand.put(FIELD_WM_WHITE_LIST, uids);
        BasicDBObject update = new BasicDBObject(OP_SET, setCommand);
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME)
                .update(query.get(), update, false, false, WriteConcern.ACKNOWLEDGED);
    }

    public List<Long> fetchWhiteListUser(String id) throws Exception {
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(id));
        DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).findOne(query.get());
        if (obj != null) {
            LiveShowWaterMark bean = db2JavaObj(mongoTemplate, LiveShowWaterMark.class, obj, FIELD_WM_ID);
            if (bean != null && bean.getWhiteList() != null) {
                return bean.getWhiteList();
            }
        }
        return Lists.newArrayList();
    }

    public void removeUser(String id,long uid){
        QueryBuilder query = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(id));
        BasicDBObject update = new BasicDBObject(OP_PULL, new BasicDBObject(FIELD_WM_WHITE_LIST,uid));
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).update(query.get(),update,false,false);
    }

    public LiveShowWaterMark findById(String msgId) throws Exception {
        if (StringUtils.isBlank(msgId)) {
            return null;
        }
        DBObject obj = mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME)
                .findOne(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(msgId)));
        return db2JavaObj(mongoTemplate, LiveShowWaterMark.class, obj, FIELD_WM_ID);
    }

    public void remove(String msgId) {
        if (StringUtils.isBlank(msgId)) {
            return;
        }
        DBObject obj = new BasicDBObject();
        obj.put(FIELD_OBJ_ID, new ObjectId(msgId));
        mongoTemplate.getDb().getCollection(COLLECTION_LIVE_SHOW_WATER_MARK_NAME).remove(obj);
    }
}
