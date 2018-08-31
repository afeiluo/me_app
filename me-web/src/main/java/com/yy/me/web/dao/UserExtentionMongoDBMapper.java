package com.yy.me.web.dao;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.yy.me.entity.AnchorAuth;
import com.yy.me.web.entity.UserExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import static com.yy.me.mongo.MongoUtil.*;

@Repository
public class UserExtentionMongoDBMapper {

    @Autowired
    @Qualifier("mongoTemplateUser")
    protected MongoTemplate userInfomongoTemplate;

    public static final String FIELD_OBJ_ID = "_id";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_LIVE_COUNT = "liveCount";

    private static final String COLLECTION_USER_EXTENTION_NAME = "user_extention";


    public void insert(UserExtension userExtention) throws Exception {
        DBObject obj = javaObj2Db(userInfomongoTemplate, userExtention, FIELD_UID);
        userInfomongoTemplate.getDb().getCollection(COLLECTION_USER_EXTENTION_NAME).insert(obj);
    }

    public void incrLiveCount(long uid) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update();
        update.inc(FIELD_LIVE_COUNT, 1);
        userInfomongoTemplate.getDb().getCollection(COLLECTION_USER_EXTENTION_NAME).update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }

    public int getLiveCount(long uid) throws Exception {
        DBObject dbObject = userInfomongoTemplate.getDb().getCollection(COLLECTION_USER_EXTENTION_NAME).findOne(uid);
        if (dbObject != null && dbObject.containsField(FIELD_LIVE_COUNT)) {
            return (Integer) dbObject.get(FIELD_LIVE_COUNT);
        }
        return 0;
    }


    public AnchorAuth find(long uid) throws Exception {
        DBObject retObje = userInfomongoTemplate.getDb().getCollection(COLLECTION_USER_EXTENTION_NAME).findOne(uid);
        return db2JavaObj(userInfomongoTemplate, AnchorAuth.class, retObje, FIELD_UID);
    }


}
