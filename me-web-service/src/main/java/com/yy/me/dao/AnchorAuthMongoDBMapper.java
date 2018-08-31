package com.yy.me.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import com.yy.me.entity.AnchorAuth;

@Repository
public class AnchorAuthMongoDBMapper {

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    public static final String FIELD_OBJ_ID = "_id";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_RESULT1 = "result1";
    public static final String FIELD_RESULT2 = "result2";

    public static final String FIELD_PHONE = "phone";

    public static final String FIELD_ID_CARD = "idCard";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_IMG1 = "image1";
    public static final String FIELD_IMG2 = "image2";

    public static final String FIELD_AUTH_SEND = "authSend";

    private static final String COLLECTION_ANCHOR_AUTH_NAME = "anchor_auth";

    public static final int AUTH_SEND_NONE = 0x0;
    public static final int AUTH_SEND_IDCARD = 0x1;
    public static final int AUTH_SEND_IMAGE = 0x2;
    public static final int AUTH_SEND_COMPLETE = 0x3;

    public void insert(AnchorAuth anchorAuth) throws Exception {
        DBObject obj = javaObj2Db(mongoTemplate, anchorAuth, FIELD_UID);
        mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME).insert(obj);
    }

    public AnchorAuth find(long uid) throws Exception {
        DBObject retObje = mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME).findOne(uid);
        return db2JavaObj(mongoTemplate, AnchorAuth.class, retObje, FIELD_UID);
    }

    public long count() {
        return mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME).count();
    }

    public long countPhone(String phone) {
        return mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .count(QueryBuilder.start(FIELD_PHONE).is(phone).get());
    }

    public void bindPhone(long uid, String phone) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update().set(FIELD_STATUS, 0).set(FIELD_PHONE, phone);
        mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }

    public long countIdCard(String idCard) {
        return mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .count(QueryBuilder.start(FIELD_ID_CARD).is(idCard).and(FIELD_STATUS).in(Lists.newArrayList(1, 2))
                        .get());
    }

    public boolean saveAuthMsg(long uid, String name, String idCard, String image1, String image2) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid).and(FIELD_STATUS).lessThanEquals(0);
        Update update = new Update().set(FIELD_NAME, name).set(FIELD_ID_CARD, idCard).set(FIELD_IMG1, image1)
                .set(FIELD_IMG2, image2).set(FIELD_STATUS, 1).set(FIELD_RESULT1, 0).set(FIELD_RESULT2, 0)
                .set(FIELD_AUTH_SEND, 0).set(FIELD_REASON, "");
        WriteResult writeResult = mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .update(queryBuilder.get(), update.getUpdateObject());
        return writeResult.getN() > 0;
    }

    public void remove(long uid) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME).remove(queryBuilder.get());
    }

    public void updateStatus(long uid, int status, String reason) {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update().set(FIELD_STATUS, status);
        if (StringUtils.isNotEmpty(reason)) {
            update.set(FIELD_REASON, reason);
        }
        mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .update(queryBuilder.get(), update.getUpdateObject(), true, false);
    }

    public List<Long> findAnchorAuthApply() throws Exception {
        QueryBuilder query = QueryBuilder.start();
        query.and(FIELD_STATUS).is(1);
        query.and(FIELD_AUTH_SEND).notEquals(AUTH_SEND_COMPLETE);

        DBObject fields = new BasicDBObject(FIELD_OBJ_ID, 1);

        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME).find(query.get(), fields)
                .limit(100);

        List<Long> uids = Lists.newArrayList();

        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            uids.add(((Long) obj.get(FIELD_OBJ_ID)));
        }

        return uids;
    }

    public AnchorAuth findAndModifySendStatusById(Long uid, int status) throws Exception {
        DBObject queryObj = new BasicDBObject(FIELD_OBJ_ID, uid);
        BasicDBObject updateObj = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_AUTH_SEND, status));

        DBObject dbObj = mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .findAndModify(queryObj, updateObj);

        return db2JavaObj(mongoTemplate, AnchorAuth.class, dbObj, "uid");
    }

    public void decrStatusById(Long uid, int number) throws Exception {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid);
        Update update = new Update().bitwise(FIELD_AUTH_SEND).and(Long.MAX_VALUE - number);

        mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .update(queryBuilder.get(), update.getUpdateObject());
    }

    public AnchorAuth setReviewResult(long uid, int type, boolean pass, String reason) throws Exception {
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(uid).and(FIELD_STATUS).notEquals(2);
        Update update = new Update().set(type == 1 ? FIELD_RESULT1 : FIELD_RESULT2, pass ? 1 : -1);
        if (!pass) {
            update.set(FIELD_STATUS, -1);
            update.set(FIELD_REASON, reason);
        }
        DBObject dbObj = mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_AUTH_NAME)
                .findAndModify(queryBuilder.get(), null, null, false, update.getUpdateObject(), false, false);
        return db2JavaObj(mongoTemplate, AnchorAuth.class, dbObj, "uid");
    }
}
