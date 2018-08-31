package com.yy.me.dao;

import com.google.common.collect.Lists;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;
import com.yy.me.entity.HeaderReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

import static com.yy.me.mongo.MongoUtil.FIELD_OBJ_ID;

/**
 * Created by wangke on 2016/11/10.
 */
@Repository
public class HeaderReviewMongoDBMapper {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "header_review";

    public static final String FIELD_UID = "uid";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_HEADERURL = "headerUrl";
    public static final String FIELD_CREATETIME = "createTime";
    public static final String FIELD_REVIEWTIME = "reviewTime";
    public static final String FIELD_IS_VALID = "isValid";

    public static final int STATUS_FAIL = -1;
    public static final int STATUS_DEFAULT = 0;
    public static final int STATUS_PASS = 1;
    public static final int STATUS_GOOD = 2;

    public void insertRecord(long uid, String headerUrl) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update().setOnInsert(FIELD_STATUS, STATUS_DEFAULT).setOnInsert(FIELD_CREATETIME, new Date()).setOnInsert(FIELD_HEADERURL,headerUrl);
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }
    public void upsertRecord(long uid, String headerUrl) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update().set(FIELD_STATUS, STATUS_DEFAULT).set(FIELD_CREATETIME, new Date()).set(FIELD_HEADERURL,headerUrl);
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }

    public void remove(long uid){
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).remove(queryBuilder.get());
    }

    public boolean isHeaderValid(long uid) throws Exception {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_NAME).findOne(queryBuilder.get());
        if (dbObject != null && dbObject.containsField(FIELD_STATUS)) {
            return (int) dbObject.get(FIELD_STATUS) != STATUS_FAIL;
        }
        return true;
    }

    public List<Long> getValidUser2Push(int limit) throws Exception {
        //获取活动无效时通过的用户，用于广播得到勋章
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_IS_VALID).is(true);
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_NAME).find(queryBuilder.get()).limit(limit);
        List<Long> ret= Lists.newArrayListWithCapacity(limit);
        while (cursor.hasNext()){
            ret.add((Long) cursor.next().get(FIELD_OBJ_ID));
        }
        return ret;
    }

    public void setUserInvalid(List<Long> uids){
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).in(uids);
        Update update=new Update().set(FIELD_IS_VALID,false);
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).update(queryBuilder.get(),update.getUpdateObject(),false,true);
    }






}
