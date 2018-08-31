package com.yy.me.web.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

/**
 * 记录别人查看自己个人主页的历史记录
 * 
 * @author qiaolinfei
 * db.check_userInfo.createIndex( { otherUid: 1 }, { background: true } ) 
 */
//TODO 生产环境加索引
@Repository
public class CheckUserInfoMongoDBMapper {

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION_CHECK_USERINFO = "check_userInfo";
    private static final String FIELD_UID = "uid";
    private static final String FIELD_OTHERUID = "otherUid";
    private static final String FIELD_CHECKTIME = "checkTime";

    /**
     * uid 查看了otherUid的个人主页
     * 
     * @param uid
     * @param otherUid
     */
    public void insertRecord(long uid, long otherUid) {
        QueryBuilder query = QueryBuilder.start(FIELD_UID).is(uid);
        query.and(FIELD_OTHERUID).is(otherUid);
        BasicDBObject update = new BasicDBObject(FIELD_CHECKTIME, System.currentTimeMillis());
        mongoTemplate.getDb().getCollection(COLLECTION_CHECK_USERINFO)
                .findAndModify(query.get(), null, null, false, new BasicDBObject(OP_SET, update), false, true);
    }

    /**
     * 查询自己被谁查看过个人主页
     * 
     * @param uid
     * @return
     */
    public Map<Long, Long> getCheckedUserList(Long uid) {
        Map<Long, Long> retMap = Maps.newLinkedHashMap();
        QueryBuilder query = QueryBuilder.start(FIELD_OTHERUID).is(uid);
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_CHECK_USERINFO).find(query.get())
                .sort(new BasicDBObject(FIELD_CHECKTIME, -1)).limit(100);// 最多显示100条查看的历史记录
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            retMap.put((Long) obj.get(FIELD_UID), (Long) obj.get(FIELD_CHECKTIME));
        }
        return retMap;
    }
}
