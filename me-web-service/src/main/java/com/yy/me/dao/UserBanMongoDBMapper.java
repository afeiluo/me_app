package com.yy.me.dao;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.yy.me.entity.RuleDesc;
import com.yy.me.entity.UserBan;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

import static com.yy.me.mongo.MongoUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.user.UserInfo.Fields.*;

/**
 * 违规记录/用户封禁
 * db.user_ban.ensureIndex({"banedEndTime":1})
 * 
 * db.user_head_ban.ensureIndex({"banedEndTime":1})
 * db.user_ls_ban_b.ensureIndex({"banedEndTime":1})
 * db.device_ls_ban.ensureIndex({"banedEndTime":1})
 * db.device_ls_ban.ensureIndex({"osType":1,"deviceId":1},{unique:true})
 * 
 * db.device_ban_history.ensureIndex({"uid":1,"osType":1,"deviceId":1})
 * 
 * 
 * @author Jiang Chengyan
 * 
 */
@Repository
public class UserBanMongoDBMapper {

    private static Logger logger = LoggerFactory.getLogger(UserBanMongoDBMapper.class);
    
    /**
     * 用户违规/封禁集合(旧)
     */
    public static final String COLLETION_USER_BAN_NAME = "user_ban";
    
    public static final String FIELD_UB_UID = "uid";
    public static final String FIELD_UB_VIOLATECOUNT = "violateCount";
    public static final String FIELD_UB_BANED_TYPE = "banedType";
    public static final String FIELD_UB_BANED_ACTION_TIME = "banedActionTime";
    public static final String FIELD_UB_BANED_DAYS = "banedDays";
    public static final String FIELD_UB_BANED_END_TIME = "banedEndTime";
    public static final String FIELD_UB_BANED_END_DONE = "banedEndDone";
    
    /**
     * 用户头像违规/封禁集合
     */
    public static final String COLLETION_USER_HEAD_BAN_NAME = "user_head_ban";
    
    /**
     * 用户连麦违规/封禁集合
     */
    public static final String COLLETION_USER_LS_LINK_BAN_NAME = "user_ls_link_ban";
    
    /**
     * 用户直播违规/封禁B类集合
     */
    public static final String COLLETION_USER_LS_BAN_B_NAME = "user_ls_ban_b";
    /**
     * 设备封禁集合
     */
    public static final String COLLETION_DEVICE_LS_BAN_NAME = "device_ls_ban";
    
    /**
     * 设备封禁历史集合
     */
    public static final String COLLETION_DEVICE_BAN_HISTORY_NAME = "device_ban_history";
    
    /**
     * 设备集合
     */
    public static final String COLLETION_DEVICE_INFO_NAME = "device_info";

    /**
     * 用户最近登录设备
     */
    public static final String COLLETION_USER_LOGIN_DEVICE = "user_login_device_info";

    public static final String FIELD_DEVICE_ID = "deviceId";
    public static final String FIELD_DEVICE_OS_TYPE = "osType";
    public static final String FIELD_DEVICE_BANED = "baned";
    public static final String FIELD_DEVICE_BANED_TYPE = "banedType";
    public static final String FIELD_DEVICE_BANED_ACTION_TIME = "banedActionTime";
    public static final String FIELD_DEVICE_BANED_END_TIME = "banedEndTime";
    public static final String FIELD_DEVICE_BANED_END_DONE = "banedEndDone";
    public static final String FIELD_DEVICE_UID = "uid";
    public static final String FIELD_DEVICE_LID = "lid";

    @Autowired
    @Qualifier("mongoTemplateUser")
    private MongoTemplate mongoTemplate;

    /**
     * 增加违规次数
     * 
     * @param uid
     * @throws Exception
     */
    public int incUserBan(long uid) throws Exception {
        long start = System.currentTimeMillis();
        int violateCount = 0;
        try {
            DBObject queryObj = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_UB_VIOLATECOUNT, 1));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_BAN_NAME)
                    .findAndModify(queryObj, null, null, false, updateCommand, true, true);
            if (obj.get(FIELD_UB_VIOLATECOUNT) instanceof Integer) {
                violateCount = (Integer) obj.get(FIELD_UB_VIOLATECOUNT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, incUserBan, uid:" + uid);
        }
        return violateCount;
    }

    public int incUserHeadBan(long uid) throws Exception {
        long start = System.currentTimeMillis();
        int violateCount = 0;
        try {
            DBObject queryObj = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_UB_VIOLATECOUNT, 1));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_HEAD_BAN_NAME)
                    .findAndModify(queryObj, null, null, false, updateCommand, true, true);
            if (obj.get(FIELD_UB_VIOLATECOUNT) instanceof Integer) {
                violateCount = (Integer) obj.get(FIELD_UB_VIOLATECOUNT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, incUserBan, uid:" + uid);
        }
        return violateCount;
    }

    public int incLinkUserBan(long uid) throws Exception {
        long start = System.currentTimeMillis();
        int violateCount = 0;
        try {
            DBObject queryObj = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_UB_VIOLATECOUNT, 1));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_LINK_BAN_NAME)
                    .findAndModify(queryObj, null, null, false, updateCommand, true, true);
            if (obj.get(FIELD_UB_VIOLATECOUNT) instanceof Integer) {
                violateCount = (Integer) obj.get(FIELD_UB_VIOLATECOUNT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, incLinkUserBan, uid:" + uid);
        }
        return violateCount;
    }

    public int incDeviceLsBan(int osType, String deviceId) throws Exception {
        long start = System.currentTimeMillis();
        int violateCount = 0;
        try {
            DBObject queryObj = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
            BasicDBObject updateCommand = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_UB_VIOLATECOUNT, 1));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_LS_BAN_NAME)
                    .findAndModify(queryObj, null, null, false, updateCommand, true, true);
            if (obj.get(FIELD_UB_VIOLATECOUNT) instanceof Integer) {
                violateCount = (Integer) obj.get(FIELD_UB_VIOLATECOUNT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("{}ms, incDeviceLsBan, deviceId:{}", (System.currentTimeMillis() - start), deviceId);
        }
        return violateCount;
    }

    public int incUserLsBanB(long uid) throws Exception {
        long start = System.currentTimeMillis();
        int violateCount = 0;
        try {
            DBObject queryObj = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_UB_VIOLATECOUNT, 1));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME)
                    .findAndModify(queryObj, null, null, false, updateCommand, true, true);
            if (obj.get(FIELD_UB_VIOLATECOUNT) instanceof Integer) {
                violateCount = (Integer) obj.get(FIELD_UB_VIOLATECOUNT);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("{}ms, incUserLsBanB, uid:{}", (System.currentTimeMillis() - start), uid);
        }
        return violateCount;
    }

    public List<UserBan> findUserBanEndedAndClear() throws Exception {
        long start = System.currentTimeMillis();
        List<UserBan> ret = null;
        try {
            QueryBuilder query = QueryBuilder.start(FIELD_UB_BANED_END_TIME).greaterThan(0).and(FIELD_UB_BANED_END_TIME)
                    .lessThan(System.currentTimeMillis()).and(FIELD_UB_BANED_END_DONE).is(false);
            DBObject orderBy = new BasicDBObject(FIELD_UB_BANED_END_TIME, -1);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_USER_BAN_NAME).find(query.get()).sort(orderBy);
            if (!cursor.hasNext()) {
                return null;
            }
            ret = Lists.newArrayList();
            List<Long> uids = Lists.newArrayList();
            while (cursor.hasNext()) {
                UserBan tmp = db2JavaObj(mongoTemplate, UserBan.class, cursor.next(), FIELD_UB_UID);
                ret.add(tmp);
                uids.add(tmp.getUid());
            }
            QueryBuilder clearQuery = QueryBuilder.start();
            BasicDBList tmpList = new BasicDBList();
            tmpList.addAll(uids);
            clearQuery.and(FIELD_OBJ_ID);
            clearQuery.in(tmpList);
            BasicDBObject updateCommand = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_U_BANED, 1).append(FIELD_U_BANED_TYPE, 1)
                    .append(FIELD_U_BANED_ACTION_TIME, 1).append(FIELD_U_BANED_END_TIME, 1));
            updateMultiByIdInner(COLLETION_USER_INFO_NAME, clearQuery.get(), updateCommand, false, WriteConcern.UNACKNOWLEDGED);

            DBObject update = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(
                    FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            ;
            mongoTemplate.getDb().getCollection(COLLETION_USER_BAN_NAME).update(query.get(), update, false, true, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, findUserBanEnded, ret size:" + (ret == null ? 0 : ret.size()));
        }
        return ret;
    }

    public List<UserBan> findUserHeadBanEndedAndClear() throws Exception {
        long start = System.currentTimeMillis();
        List<UserBan> ret = null;
        try {
            QueryBuilder query = QueryBuilder.start(FIELD_UB_BANED_END_TIME).greaterThan(0).and(FIELD_UB_BANED_END_TIME)
                    .lessThan(System.currentTimeMillis()).and(FIELD_UB_BANED_END_DONE).is(false);
            DBObject orderBy = new BasicDBObject(FIELD_UB_BANED_END_TIME, -1);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_USER_HEAD_BAN_NAME).find(query.get()).sort(orderBy);
            if (!cursor.hasNext()) {
                return null;
            }
            ret = Lists.newArrayList();
            List<Long> uids = Lists.newArrayList();
            while (cursor.hasNext()) {
                UserBan tmp = db2JavaObj(mongoTemplate, UserBan.class, cursor.next(), FIELD_UB_UID);
                ret.add(tmp);
                uids.add(tmp.getUid());
            }
            QueryBuilder clearQuery = QueryBuilder.start();
            BasicDBList tmpList = new BasicDBList();
            tmpList.addAll(uids);
            clearQuery.and(FIELD_OBJ_ID);
            clearQuery.in(tmpList);
            BasicDBObject updateCommand = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_U_BANED, 1).append(FIELD_U_BANED_TYPE, 1)
                    .append(FIELD_U_BANED_ACTION_TIME, 1).append(FIELD_U_BANED_END_TIME, 1));
            updateMultiByIdInner(COLLETION_USER_INFO_NAME, clearQuery.get(), updateCommand, false, WriteConcern.UNACKNOWLEDGED);

            DBObject update = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(
                    FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            ;
            mongoTemplate.getDb().getCollection(COLLETION_USER_HEAD_BAN_NAME).update(query.get(), update, false, true, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, findUserHeadBanEndedAndClear, ret size:" + (ret == null ? 0 : ret.size()));
        }
        return ret;
    }

    public List<UserBan> findUserLinkBanEndedAndClear() throws Exception {
        long start = System.currentTimeMillis();
        List<UserBan> ret = null;
        try {
            QueryBuilder query = QueryBuilder.start(FIELD_UB_BANED_END_TIME).greaterThan(0).and(FIELD_UB_BANED_END_TIME)
                    .lessThan(System.currentTimeMillis()).and(FIELD_UB_BANED_END_DONE).is(false);
            DBObject orderBy = new BasicDBObject(FIELD_UB_BANED_END_TIME, -1);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_LINK_BAN_NAME).find(query.get()).sort(orderBy);
            if (!cursor.hasNext()) {
                return null;
            }
            ret = Lists.newArrayList();
            List<Long> uids = Lists.newArrayList();
            while (cursor.hasNext()) {
                UserBan tmp = db2JavaObj(mongoTemplate, UserBan.class, cursor.next(), FIELD_UB_UID);
                ret.add(tmp);
                uids.add(tmp.getUid());
            }
            QueryBuilder clearQuery = QueryBuilder.start();
            BasicDBList tmpList = new BasicDBList();
            tmpList.addAll(uids);
            clearQuery.and(FIELD_OBJ_ID);
            clearQuery.in(tmpList);
            BasicDBObject updateCommand = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_U_BANED, 1).append(FIELD_U_BANED_TYPE, 1)
                    .append(FIELD_U_BANED_ACTION_TIME, 1).append(FIELD_U_BANED_END_TIME, 1));
            updateMultiByIdInner(COLLETION_USER_INFO_NAME, clearQuery.get(), updateCommand, false, WriteConcern.UNACKNOWLEDGED);

            DBObject update = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(
                    FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            ;
            mongoTemplate.getDb().getCollection(COLLETION_USER_LS_LINK_BAN_NAME).update(query.get(), update, false, true, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, findUserLinkBanEndedAndClear, ret size:" + (ret == null ? 0 : ret.size()));
        }
        return ret;
    }

    public List<UserBan> findUserLsBanEndedAndClear() throws Exception {
        long start = System.currentTimeMillis();
        List<UserBan> ret = null;
        try {
            QueryBuilder query = QueryBuilder.start(FIELD_UB_BANED_END_TIME).greaterThan(0).and(FIELD_UB_BANED_END_TIME)
                    .lessThan(System.currentTimeMillis()).and(FIELD_UB_BANED_END_DONE).is(false);
            DBObject update = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(
                    FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            ;
            mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).update(query.get(), update, false, true, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, findUserLsBanEndedAndClear, ret size:" + (ret == null ? 0 : ret.size()));
        }
        return ret;
    }

    public int clearAllUserLsBan() throws Exception {
        long start = System.currentTimeMillis();
        int n = 0;
        try {
            QueryBuilder query = QueryBuilder.start();
            WriteResult result = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).remove(query.get(), WriteConcern.ACKNOWLEDGED);
            n = result.getN();
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("{}ms, clearAllUserLsBan, ret size:", (System.currentTimeMillis() - start), n);
        }
        return n;
    }

    public List<DBObject> findDeviceBanEndedAndClear() throws Exception {
        long start = System.currentTimeMillis();
        List<DBObject> ret = null;
        try {
            QueryBuilder query = QueryBuilder.start(FIELD_DEVICE_BANED_END_TIME).greaterThan(0).and(FIELD_DEVICE_BANED_END_TIME)
                    .lessThan(System.currentTimeMillis()).and(FIELD_DEVICE_BANED_END_DONE).is(false);
            DBObject orderBy = new BasicDBObject(FIELD_DEVICE_BANED_END_TIME, -1);
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_LS_BAN_NAME).find(query.get()).sort(orderBy);
            if (!cursor.hasNext()) {
                return null;
            }
            ret = Lists.newArrayList();
            List<Object[]> criteria = Lists.newArrayList();
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                int osType = getInt(obj.get(FIELD_DEVICE_OS_TYPE), 1);
                String deviceId = (String) obj.get(FIELD_DEVICE_ID);
                ret.add(obj);
                criteria.add(new Object[] { osType, deviceId });
            }
            QueryBuilder clearQuery = QueryBuilder.start().or();
            for (Object[] obj : criteria) {
                clearQuery.or(new BasicDBObject(FIELD_DEVICE_OS_TYPE, obj[0]).append(FIELD_DEVICE_ID, obj[1]));
            }
            BasicDBObject updateCommand = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_DEVICE_BANED, 1).append(FIELD_DEVICE_BANED_TYPE, 1)
                    .append(FIELD_DEVICE_BANED_ACTION_TIME, 1).append(FIELD_DEVICE_BANED_END_TIME, 1));
            updateMultiByIdInner(COLLETION_DEVICE_INFO_NAME, clearQuery.get(), updateCommand, false, WriteConcern.UNACKNOWLEDGED);

            DBObject update = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_DEVICE_BANED_END_TIME, 1).append(FIELD_DEVICE_BANED_TYPE, 1)
                    .append(FIELD_DEVICE_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_DEVICE_BANED_END_DONE, true));
            mongoTemplate.getDb().getCollection(COLLETION_DEVICE_LS_BAN_NAME).update(query.get(), update, false, true, WriteConcern.ACKNOWLEDGED);

        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, findDeviceBanEndedAndClear, ret size:" + (ret == null ? 0 : ret.size()));
        }
        return ret;
    }

    public void banUser(long uid, String banedType, long actionTime, long banedEndTime, RuleDesc ruleDesc) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject setObject1 = new BasicDBObject(FIELD_UB_BANED_TYPE, banedType)
                    .append(FIELD_UB_BANED_ACTION_TIME, actionTime).append(FIELD_UB_BANED_END_TIME, banedEndTime)
                    .append(FIELD_UB_BANED_END_DONE, false);
            if (ruleDesc != null) {
                setObject1.append(FIELD_RULE_DESC_ITEM, ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM, ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC, ruleDesc.getDesc());
            }
            BasicDBObject updateCommand1 = new BasicDBObject(OP_SET,setObject1);

            BasicDBObject setObject2 =  new BasicDBObject(FIELD_U_BANED, true).append(FIELD_U_BANED_TYPE, banedType)
                    .append(FIELD_U_BANED_ACTION_TIME, new Date(actionTime)).append(FIELD_U_BANED_END_TIME, new Date(banedEndTime));
            if (ruleDesc != null) {
                setObject2.append(FIELD_RULE_DESC_ITEM, ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM, ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC, ruleDesc.getDesc());
            }

            updateByIdInner(COLLETION_USER_BAN_NAME, queryObj1, updateCommand1, true, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, banUser, uid:" + uid + ", banedType:" + banedType + ", banedEndTime"
                    + new Date(banedEndTime));
        }
    }

    public void banUserHead(long uid, String banedType, long actionTime, long banedEndTime) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand1 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_UB_BANED_TYPE, banedType)
                    .append(FIELD_UB_BANED_ACTION_TIME, actionTime).append(FIELD_UB_BANED_END_TIME, banedEndTime)
                    .append(FIELD_UB_BANED_END_DONE, false));

            DBObject queryObj2 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand2 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_U_BANED, true).append(FIELD_U_BANED_TYPE, banedType)
                    .append(FIELD_U_BANED_ACTION_TIME, new Date(actionTime)).append(FIELD_U_BANED_END_TIME, new Date(banedEndTime)));

            updateByIdInner(COLLETION_USER_HEAD_BAN_NAME, queryObj1, updateCommand1, true, WriteConcern.ACKNOWLEDGED);
            updateByIdInner(COLLETION_USER_INFO_NAME, queryObj2, updateCommand2, false, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, banUserHead, uid:" + uid + ", banedType:" + banedType + ", banedEndTime"
                    + new Date(banedEndTime));
        }
    }

    public void banUserText(long uid, String banedType, long actionTime, long banedEndTime) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj2 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand2 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_U_BANED, true).append(FIELD_U_BANED_TYPE, banedType)
                    .append(FIELD_U_BANED_ACTION_TIME, new Date(actionTime)).append(FIELD_U_BANED_END_TIME, new Date(banedEndTime)));

            updateByIdInner(COLLETION_USER_INFO_NAME, queryObj2, updateCommand2, false, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, banUserText, uid:" + uid + ", banedType:" + banedType + ", banedEndTime"
                    + new Date(banedEndTime));
        }
    }

    public void banDevice(long uid, String lid, int osType, String deviceId, String banedType, long actionTime, long banedEndTime, RuleDesc ruleDesc) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
            BasicDBObject updateCommand1 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_DEVICE_BANED_TYPE, banedType)
                    .append(FIELD_DEVICE_BANED_ACTION_TIME, actionTime).append(FIELD_DEVICE_BANED_END_TIME, banedEndTime)
                    .append(FIELD_DEVICE_BANED_END_DONE, false)
                    .append(FIELD_RULE_DESC_ITEM,ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM,ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC,ruleDesc.getDesc()));

            DBObject queryObj2 = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
            BasicDBObject updateCommand2 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_DEVICE_BANED, true)
                    .append(FIELD_DEVICE_BANED_TYPE, banedType).append(FIELD_DEVICE_BANED_ACTION_TIME, new Date(actionTime))
                    .append(FIELD_DEVICE_BANED_END_TIME, new Date(banedEndTime))
                    .append(FIELD_RULE_DESC_ITEM,ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM,ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC,ruleDesc.getDesc()));

            updateByIdInner(COLLETION_DEVICE_LS_BAN_NAME, queryObj1, updateCommand1, false, WriteConcern.ACKNOWLEDGED);
            updateByIdInner(COLLETION_DEVICE_INFO_NAME, queryObj2, updateCommand2, true, WriteConcern.ACKNOWLEDGED);

            BasicDBObject saveObj = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId).append(FIELD_DEVICE_UID, uid)
                    .append(FIELD_DEVICE_BANED_TYPE, banedType)
                    .append(FIELD_DEVICE_BANED_ACTION_TIME, new Date(actionTime)).append(FIELD_DEVICE_BANED_END_TIME, new Date(banedEndTime))
                    .append(FIELD_RULE_DESC_ITEM,ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM,ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC,ruleDesc.getDesc());
            if (StringUtils.isNotBlank(lid)) {
                saveObj.append(FIELD_DEVICE_LID, new ObjectId(lid));
            }
            mongoTemplate.getDb().getCollection(COLLETION_DEVICE_BAN_HISTORY_NAME).save(saveObj);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("{}ms, banDevice, osType:{}, deviceId:{}, banedType:{}, banedEndTime:{}", (System.currentTimeMillis() - start), osType,
                    deviceId, banedType, new Date(banedEndTime));
        }
    }

    public void banUserLs(long uid, String banedType, long actionTime, long banedEndTime, String banedDays, RuleDesc ruleDesc) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand1 = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_UB_BANED_TYPE, banedType)
                    .append(FIELD_UB_BANED_ACTION_TIME, actionTime).append(FIELD_UB_BANED_DAYS, banedDays)
                    .append(FIELD_UB_BANED_END_TIME, banedEndTime).append(FIELD_UB_BANED_END_DONE, false)
                    .append(FIELD_RULE_DESC_ITEM,ruleDesc.getItem()).append(FIELD_RULE_DESC_SUB_ITEM,ruleDesc.getSubItem()).append(FIELD_RULE_DESC_DESC,ruleDesc.getDesc()));
            updateByIdInner(COLLETION_USER_LS_BAN_B_NAME, queryObj1, updateCommand1, false, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, banUserLs, uid:" + uid + ", banedType:" + banedType + ", banedEndTime"
                    + new Date(banedEndTime));
        }
    }

    // TODO liveshow project
    public DBObject checkUserLsCanStart(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid).append(FIELD_UB_BANED_END_DONE, false);
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).findOne(queryObj1);
            return obj;
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info("{}ms, checkUserLsCanStart, uid:{}", (System.currentTimeMillis() - start), uid);
        }
    }

    public void clearUserLastDeviceBanNotRecord(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_DEVICE_UID, uid);
            DBObject sort = new BasicDBObject(FIELD_OBJ_ID, -1);
            DBCursor cur = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_BAN_HISTORY_NAME).find(queryObj1).sort(sort).limit(1);
            if (!cur.hasNext()) {
                logger.warn("Admin Clear Empty Ban Record! uid:{}", uid);
            } else {
                DBObject obj = cur.next();
                int osType = (Integer)obj.get(FIELD_DEVICE_OS_TYPE);
                String deviceId = (String)obj.get(FIELD_DEVICE_ID);
                QueryBuilder qb1 = QueryBuilder.start(FIELD_DEVICE_OS_TYPE).is(osType).and(FIELD_DEVICE_ID).is(deviceId);

                BasicDBObject updateCommand1 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_DEVICE_BANED_END_TIME, 1).append(FIELD_DEVICE_BANED_TYPE, 1)).append(OP_SET, new BasicDBObject(FIELD_DEVICE_BANED_END_DONE, true));
                DBObject obj1 = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_LS_BAN_NAME).findAndModify(qb1.get(), null, null, false, updateCommand1, false, false);
                if (obj1 == null) {
                    logger.warn("Admin Clear Empty Not Record Device Ban Record! uid:{}", uid);
                }
                
                DBObject queryObj2 = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
                BasicDBObject updateCommand2 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_DEVICE_BANED, 1)
                        .append(FIELD_DEVICE_BANED_TYPE, 1).append(FIELD_DEVICE_BANED_ACTION_TIME, 1).append(FIELD_DEVICE_BANED_END_TIME, 1));
                updateByIdInner(COLLETION_DEVICE_INFO_NAME, queryObj2, updateCommand2, true, WriteConcern.ACKNOWLEDGED);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, clearUserLastDeviceBan, uid:" + uid);
        }
    }

    public void clearUserLastDeviceBan(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_DEVICE_UID, uid);
            DBObject sort = new BasicDBObject(FIELD_OBJ_ID, -1);
            DBCursor cur = mongoTemplate.getDb().getCollection(COLLETION_DEVICE_BAN_HISTORY_NAME).find(queryObj1).sort(sort).limit(1);
            if (!cur.hasNext()) {
                logger.warn("Admin Clear Empty Ban Record! uid:{}", uid);
            } else {
                DBObject obj = cur.next();
                int osType = (Integer)obj.get(FIELD_DEVICE_OS_TYPE);
                String deviceId = (String)obj.get(FIELD_DEVICE_ID);
                QueryBuilder qb1 = QueryBuilder.start(FIELD_DEVICE_OS_TYPE).is(osType).and(FIELD_DEVICE_ID).is(deviceId);
                mongoTemplate.getDb().getCollection(COLLETION_DEVICE_LS_BAN_NAME).remove(qb1.get());
                
                DBObject queryObj2 = new BasicDBObject(FIELD_DEVICE_OS_TYPE, osType).append(FIELD_DEVICE_ID, deviceId);
                BasicDBObject updateCommand2 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_DEVICE_BANED, 1)
                        .append(FIELD_DEVICE_BANED_TYPE, 1).append(FIELD_DEVICE_BANED_ACTION_TIME, 1).append(FIELD_DEVICE_BANED_END_TIME, 1));
                updateByIdInner(COLLETION_DEVICE_INFO_NAME, queryObj2, updateCommand2, true, WriteConcern.ACKNOWLEDGED);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, clearUserLastDeviceBan, uid:" + uid);
        }
    }

    public void clearBBanNotRecord(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj2 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand2 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            DBObject obj2 = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).findAndModify(queryObj2, null, null, false, updateCommand2, false, false);
            if (obj2 == null) {
                logger.warn("Admin Clear Empty Ls Ban B Record! uid:{}", uid);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, clearBan, uid:" + uid);
        }
    }

    public void clearBan(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid);
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_BAN_NAME).findAndModify(queryObj1, null, null, true, null, false, false);
            if (obj == null) {
                logger.warn("Admin Clear Empty Not Record Ban Record! uid:{}", uid);
            }
            DBObject queryObj2 = new BasicDBObject(FIELD_OBJ_ID, uid);
            DBObject obj2 = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).findAndModify(queryObj2, null, null, true, null, false, false);
            if (obj2 == null) {
                logger.warn("Admin Clear Empty Not Record Ls Ban B Record! uid:{}", uid);
            }
            DBObject queryObj3 = new BasicDBObject(FIELD_OBJ_ID, uid);
            DBObject obj3 = mongoTemplate.getDb().getCollection(COLLETION_USER_HEAD_BAN_NAME).findAndModify(queryObj3, null, null, true, null, false, false);
            if (obj3 == null) {
                logger.warn("Admin Clear Empty Not Record User Head Ban Record! uid:{}", uid);
            }
            
            DBObject queryObjUser = QueryBuilder.start(FIELD_OBJ_ID).is(uid).and(FIELD_U_BANED).exists(true).get();
            BasicDBObject updateCommandUser = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_U_BANED, 1).append(FIELD_U_BANED_TYPE, 1).append(FIELD_U_BANED_ACTION_TIME, 1).append(FIELD_U_BANED_END_TIME, 1));

            updateByIdInner(COLLETION_USER_INFO_NAME, queryObjUser, updateCommandUser, false, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, clearBan, uid:" + uid);
        }
    }
    
    public void clearBanNotRecord(long uid) throws Exception {
        long start = System.currentTimeMillis();
        try {
            DBObject queryObj1 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand1 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_BAN_NAME).findAndModify(queryObj1, null, null, false, updateCommand1, false, false);
            if (obj == null) {
                logger.warn("Admin Clear Empty Ban Record! uid:{}", uid);
            }
            DBObject queryObj2 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand2 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            DBObject obj2 = mongoTemplate.getDb().getCollection(COLLETION_USER_LS_BAN_B_NAME).findAndModify(queryObj2, null, null, false, updateCommand2, false, false);
            if (obj2 == null) {
                logger.warn("Admin Clear Empty Ls Ban B Record! uid:{}", uid);
            }
            DBObject queryObj3 = new BasicDBObject(FIELD_OBJ_ID, uid);
            BasicDBObject updateCommand3 = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_UB_BANED_END_TIME, 1).append(FIELD_UB_BANED_TYPE, 1).append(FIELD_UB_BANED_ACTION_TIME, 1)).append(OP_SET, new BasicDBObject(FIELD_UB_BANED_END_DONE, true));
            DBObject obj3 = mongoTemplate.getDb().getCollection(COLLETION_USER_HEAD_BAN_NAME).findAndModify(queryObj3, null, null, false, updateCommand3, false, false);
            if (obj3 == null) {
                logger.warn("Admin Clear Empty User Head Ban Record! uid:{}", uid);
            }
            
            DBObject queryObjUser = QueryBuilder.start(FIELD_OBJ_ID).is(uid).and(FIELD_U_BANED).exists(true).get();
            BasicDBObject updateCommandUser = new BasicDBObject(OP_UNSET, new BasicDBObject(FIELD_U_BANED, 1).append(FIELD_U_BANED_TYPE, 1).append(FIELD_U_BANED_ACTION_TIME, 1).append(FIELD_U_BANED_END_TIME, 1));

            updateByIdInner(COLLETION_USER_INFO_NAME, queryObjUser, updateCommandUser, false, WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw e;
        } finally {
            logger.info((System.currentTimeMillis() - start) + "ms, clearBan, uid:" + uid);
        }
    }

    private void updateByIdInner(String collName, DBObject query, BasicDBObject updateCommand, boolean upsert, WriteConcern... writeConcern)
            throws Exception {
        DBCollection coll = mongoTemplate.getDb().getCollection(collName);
        WriteConcern concern = null;
        if (writeConcern == null || writeConcern.length == 0) {
            concern = coll.getWriteConcern();
        } else {
            concern = writeConcern[0];
        }
        coll.update(query, updateCommand, upsert, false, concern);
    }

    private void updateMultiByIdInner(String collName, DBObject query, BasicDBObject updateCommand, boolean upsert, WriteConcern... writeConcern)
            throws Exception {
        DBCollection coll = mongoTemplate.getDb().getCollection(collName);
        WriteConcern concern = null;
        if (writeConcern == null || writeConcern.length == 0) {
            concern = coll.getWriteConcern();
        } else {
            concern = writeConcern[0];
        }
        coll.update(query, updateCommand, upsert, true, concern);
    }

    /**
     * 查询用户最近登录的设备信息
     * @param uid
     */
    public LoginDevice getUserLoginDevice(Long uid) {
        LoginDevice loginDevice = null;
        DBObject obj = mongoTemplate.getDb().getCollection(COLLETION_USER_LOGIN_DEVICE).findOne(new BasicDBObject(FIELD_DEVICE_UID, uid));
        if (obj != null) {
            loginDevice = new LoginDevice();
            loginDevice.setOsType((Integer) obj.get(FIELD_DEVICE_OS_TYPE));
            loginDevice.setDeviceId((String) obj.get(FIELD_DEVICE_ID));
        }
        return loginDevice;
    }
    public static class LoginDevice{
        private Integer osType;
        private String deviceId;

        public Integer getOsType() {
            return osType;
        }

        public void setOsType(Integer osType) {
            this.osType = osType;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
    }
}