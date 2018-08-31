package com.yy.me.dao;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * 用户黑白名单统一mapper
 * Created by wangke on 2016/7/5.
 */
@Repository
public class UserListMongoDBMapper {

    public static final String COLLECTION_USER_LIST_NAME = "user_list";
    public static final String FIELD_APPID = "appid";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_UPDATE_TIME = "updateTime";
    public static final String FIELD_OBJ_ID = "_id";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public List<Long> getAll(long id) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id);
        List<Long> ret = Lists.newArrayList();
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).find(queryBuilder.get());
        while (cursor.hasNext()) {
            ret.add((Long) cursor.next().get(FIELD_UID));
        }
        return ret;
    }
    public List<Long> getList(long id, Long searchUid, int offset, int pageSize) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id);
        if (searchUid != null) {
            queryBuilder.and(FIELD_UID).is(searchUid);
        }
        List<Long> ret = Lists.newArrayList();
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).find(queryBuilder.get()).skip(offset).limit(pageSize);
        while (cursor.hasNext()) {
            ret.add((Long) cursor.next().get(FIELD_UID));
        }
        return ret;
    }

    public long countList(long id) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id);
        return mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).count(queryBuilder.get());
    }

    public void add(long id, List<Long> uidList) {
        Update update = new Update().set(FIELD_UPDATE_TIME, new Date());
        DBObject updateObject = update.getUpdateObject();
        for (Long uid : uidList) {
            QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id).and(FIELD_UID).is(uid);
            mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).update(queryBuilder.get(), updateObject, true, false);
        }
    }

    public boolean exist(long id, long uid) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id).and(FIELD_UID).is(uid);
        return mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).count(queryBuilder.get()) > 0;
    }

    public void del(long id, long uid) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_APPID).is(id).and(FIELD_UID).is(uid);
        mongoTemplate.getDb().getCollection(COLLECTION_USER_LIST_NAME).remove(queryBuilder.get());
    }
}
