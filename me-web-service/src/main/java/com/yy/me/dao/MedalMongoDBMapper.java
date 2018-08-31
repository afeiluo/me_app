package com.yy.me.dao;

import com.mongodb.Cursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.yy.me.user.entity.Medal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.yy.me.mongo.MongoUtil.FIELD_OBJ_ID;
import static com.yy.me.mongo.MongoUtil.db2JavaObj;

/**
 * Created by wangke on 2016/11/8.
 */
@Repository
public class MedalMongoDBMapper {


    private static Logger logger = LoggerFactory.getLogger(MedalMongoDBMapper.class);

    private static final String COLLECTION_NAME = "medal";

    private static final String FIELD_MID = "mid";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_IMGURL = "imgUrl";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESC = "desc";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_STARTTIME = "startTime";
    private static final String FIELD_ENDTIME = "endTime";

    private static final String FIELD_HAS_PUSH = "hasPush";

    public static final int MEDAL_ID_HEADER_REVIEW = 1;
    @Autowired
    @Qualifier("mongoTemplate")
    protected MongoTemplate mongoTemplate;


    public List<Medal> getValidMedals() throws Exception {
        Date date = new Date();
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_STARTTIME).lessThanEquals(date)
                .and(FIELD_ENDTIME).greaterThanEquals(date);
        Cursor cursor = mongoTemplate.getCollection(COLLECTION_NAME).find(queryBuilder.get());
        List<Medal> list = new ArrayList<>();
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            list.add(db2JavaObj(mongoTemplate, Medal.class, obj, FIELD_MID));
        }
        return list;
    }

    public boolean isValid(int medalId) throws Exception {
        Date date = new Date();
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(medalId).and(FIELD_STARTTIME).lessThan(date)
                .and(FIELD_ENDTIME).greaterThanEquals(date);
        return mongoTemplate.getCollection(COLLECTION_NAME).count(queryBuilder.get())>0;
    }
    public Medal getMedal2Push(int medalId) throws Exception {
        Date date = new Date();
        QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(medalId).and(FIELD_STARTTIME).lessThan(date)
                .and(FIELD_ENDTIME).greaterThanEquals(date).and(FIELD_HAS_PUSH).notEquals(true);
        Update update = new Update().set(FIELD_HAS_PUSH, true);
        DBObject dbObject = mongoTemplate.getCollection(COLLECTION_NAME).findAndModify(queryBuilder.get(), null, null, false, update.getUpdateObject(),
                false, false);
        return db2JavaObj(mongoTemplate, Medal.class, dbObject, FIELD_MID);
    }


}
