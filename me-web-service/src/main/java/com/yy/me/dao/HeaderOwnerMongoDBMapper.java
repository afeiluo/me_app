package com.yy.me.dao;

import com.mongodb.QueryBuilder;
import com.mongodb.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;

import static com.yy.me.mongo.MongoUtil.FIELD_OBJ_ID;

/**
 * Created by wangke on 2016/11/10.
 */
@Repository
public class HeaderOwnerMongoDBMapper {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "header_review";

    public static final String FIELD_MID = "mid";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_CREATETIME = "createTime";


    public void insertRecord(long uid, int medalId) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_MID).is(medalId).and(FIELD_UID).is(uid);
        Update update = new Update().setOnInsert(FIELD_CREATETIME, new Date());
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }
    public void removeRecord(long uid, int medalId) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_MID).is(medalId).and(FIELD_UID).is(uid);
        mongoTemplate.getDb().getCollection(COLLECTION_NAME).remove(queryBuilder.get());
    }


    




}
