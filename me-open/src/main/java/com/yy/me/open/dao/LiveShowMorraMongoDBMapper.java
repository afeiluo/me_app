package com.yy.me.open.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;
import com.yy.me.open.entity.LiveShowMorra;
import com.yy.me.open.entity.LiveShowMorraOuter;

/**
 * Created by wangke on 2016/8/16.
 */
@Repository
public class LiveShowMorraMongoDBMapper {

    public static final String COLLETION_LIVE_SHOW_MORRA = "live_show_morra";

    public static final String FIELD_GID = "gid";
    public static final String FIELD_OBJECT_ID = "_id";
    public static final String FIELD_LID = "lid";
    public static final String FIELD_CREATE_TIME = "createTime";
    public static final String FIELD_ANCHOR_UID = "anchorUid";
    public static final String FIELD_GUEST_UID = "guestUid";
    public static final String FIELD_ANCHOR_MORRA = "anchorMorra";
    public static final String FIELD_GUEST_MORRA = "guestMorra";
    public static final String FIELD_ANCHOR_TIME = "anchorTime";
    public static final String FIELD_GUEST_TIME = "guestTime";
    public static final String FIELD_MORRA_COUNT = "morraCount";
    public static final String FIELD_CANCEL = "cancel";
    public static final String FIELD_RESULT = "result";

    public static final int NORMAL=0;
    public static final int CHOOSING=1;
    public static final int FINISH=2;


    @Autowired
    @Qualifier("mongoTemplateLiveshow")
    private MongoTemplate mongoTemplate;

    public List<LiveShowMorraOuter> findUserLiveShow(long uid, long startTime, long endTime) throws Exception{
        QueryBuilder queryBuilder=QueryBuilder.start(FIELD_ANCHOR_UID).is(uid).and(FIELD_CREATE_TIME).greaterThanEquals(new Date(startTime))
                .and(FIELD_CREATE_TIME).lessThanEquals(new Date(endTime)).and(FIELD_RESULT).exists(true);
        BasicDBObject projection =new BasicDBObject(FIELD_CREATE_TIME,true).append(FIELD_ANCHOR_UID,true).append(FIELD_GUEST_UID,true).append(FIELD_RESULT,true);
        DBCursor cursor=mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).find(queryBuilder.get(),projection);
        List<LiveShowMorraOuter> ret= Lists.newArrayList();
        while (cursor.hasNext()){
            ret.add(db2JavaObj(mongoTemplate,LiveShowMorraOuter.class,cursor.next(),FIELD_GID));
        }
        return ret;
    }

    public LiveShowMorra createMorra(long uid, String lid) throws Exception {
        LiveShowMorra liveShowMorra = new LiveShowMorra();
        liveShowMorra.setLid(lid);
        liveShowMorra.setAnchorUid(uid);
        liveShowMorra.setCreateTime(new Date());
        DBObject obj = javaObj2Db(mongoTemplate, liveShowMorra, FIELD_GID);
        mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).save(obj);
        liveShowMorra.setGid(((ObjectId) (obj.get(FIELD_OBJECT_ID))).toHexString());
        return liveShowMorra;
    }


    public boolean cancelMorra(String gid, String lid, Long uid) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJECT_ID).is(new ObjectId(gid)).and(FIELD_LID).is(lid)
                .and(FIELD_MORRA_COUNT).notEquals(FINISH).and(FIELD_CANCEL).is(false);
        Update update=new Update().set(FIELD_CANCEL,true);
        WriteResult result = mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).update(queryBuilder.get(), update.getUpdateObject());
        return result.getN()>0;
    }

    public void setResult(String gid, String lid, int result) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJECT_ID).is(new ObjectId(gid)).and(FIELD_LID).is(lid);
        Update update=new Update().set(FIELD_RESULT,result);
        mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).update(queryBuilder.get(), update.getUpdateObject());
    }

    public LiveShowMorra findChannelLastGame(String lid) throws Exception {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_LID).is(lid);
        DBCursor cursor=mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).find(queryBuilder.get()).sort(new BasicDBObject(FIELD_CREATE_TIME,-1)).limit(1);
        if(cursor.hasNext()){
            return db2JavaObj(mongoTemplate,LiveShowMorra.class,cursor.next(),FIELD_GID);
        }
        return null;
    }

    public LiveShowMorra findMorra(String gid) throws Exception {
        return db2JavaObj(mongoTemplate,LiveShowMorra.class,mongoTemplate.getDb().getCollection(COLLETION_LIVE_SHOW_MORRA).findOne(new ObjectId(gid)),FIELD_GID);
    }

}
