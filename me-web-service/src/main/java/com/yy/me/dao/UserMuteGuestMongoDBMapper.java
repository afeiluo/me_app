package com.yy.me.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
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
import com.mongodb.WriteConcern;

/**
 * 永久禁言
 * db.mute_guest.ensureIndex({"uid":1, "guestUid":1}, {unique:true})
 * db.mute_guest.ensureIndex({"uid":1, "_id":1})
 * 
 * 库表：COLLETION_MUTE_GUEST_NAME
 * 
 * @author JCY
 * 
 */
@Repository
public class UserMuteGuestMongoDBMapper {

    private static Logger logger = LoggerFactory.getLogger(UserMuteGuestMongoDBMapper.class);
    
    /**
     * 禁言集合
     */
    public static final String COLLETION_MUTE_GUEST_NAME = "mute_guest";
    
    public static final String FIELD_UM_UMID = "umid";
    public static final String FIELD_UM_UID = "uid";
    public static final String FIELD_UM_GUEST_UID = "guestUid";
    public static final String FIELD_UM_LID = "lid";// 如有

    @Autowired
    @Qualifier("mongoTemplateLiveshow")
    private MongoTemplate mongoTemplate;

    public void mute(long uid, long guestUid, String lid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            if (uid == guestUid) {
                throw new RuntimeException("Not allow to mute self!");
            }
            DBObject query1 = new BasicDBObject(FIELD_UM_UID, uid).append(FIELD_UM_GUEST_UID, guestUid);
            BasicDBObject setCommand = new BasicDBObject(FIELD_UPDATE_TIME, start);
            if (StringUtils.isNotBlank(lid)) {
                setCommand.append(FIELD_UM_LID, new ObjectId(lid));
            }
            DBObject field1 = new BasicDBObject(OP_SET, setCommand);
            mongoTemplate.getDb().getCollection(COLLETION_MUTE_GUEST_NAME).update(query1, field1, true, false, WriteConcern.UNACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.debug("{}ms, mute uid:{}, guestUid:{}", (System.currentTimeMillis() - start), uid, guestUid);
        }
    }

    public void cancelMute(long uid, long guestUid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            if (uid == guestUid) {
                throw new RuntimeException("Not allow to cancel mute self!");
            }
            BasicDBObject query = new BasicDBObject(FIELD_UM_UID, uid).append(FIELD_UM_GUEST_UID, guestUid);
            mongoTemplate.getDb().getCollection(COLLETION_MUTE_GUEST_NAME).remove(query, WriteConcern.UNACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.debug("{}ms, cancelMute uid:{}, guestUid:{}", (System.currentTimeMillis() - start), uid, guestUid);
        }
    }

    /**
     * 检查是否有关注关系
     * 
     * @param uid
     * @param guestUid
     * @param
     */
    public boolean checkMute(long uid, long guestUid) throws Exception {
        long start = System.currentTimeMillis();
        boolean isMuted = false;
        try {
            if (uid == guestUid) {
                return false;
            }
            BasicDBObject query = new BasicDBObject(FIELD_UM_UID, uid).append(FIELD_UM_GUEST_UID, guestUid);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_MUTE_GUEST_NAME).find(query).limit(1);
            isMuted = cursor.hasNext();
            return isMuted;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.debug("{}ms, checkMute uid:{}, guestUid:{}, ret:{}", (System.currentTimeMillis() - start), uid, guestUid, isMuted);
        }
    }

    public List<Long> findMuteList(long uid) throws Exception {
        long start = System.currentTimeMillis();
        List<Long> ret = null;
        try {
            BasicDBObject query = new BasicDBObject(FIELD_UM_UID, uid);
            DBObject fields = new BasicDBObject(FIELD_UM_GUEST_UID, 1).append(FIELD_OBJ_ID, 0);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_MUTE_GUEST_NAME).find(query, fields);
            if (!cursor.hasNext()) {
                return null;
            }
            ret = Lists.newArrayList();
            while (cursor.hasNext()) {
                ret.add((Long)cursor.next().get(FIELD_UM_GUEST_UID));
            }
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.debug("{}ms, findMuteList uid:{}, ret:{}", (System.currentTimeMillis() - start), uid, genLogObj(ret));
        }
    }

}
