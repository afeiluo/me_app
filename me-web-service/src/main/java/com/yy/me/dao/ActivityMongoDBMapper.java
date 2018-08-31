package com.yy.me.dao;

import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.*;
import static com.yy.me.message.MessageMongoDBMapper.*;
import static com.yy.me.message.MsgDataType.*;
import static com.yy.me.mongo.MongoUtil.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.yy.me.entity.Activity;
import com.yy.me.entity.RankActivity;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.message.LsEventType;
import com.yy.me.message.Message;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.message.MsgDataType;
import com.yy.me.message.thrift.push.MessageType;

@Repository
public class ActivityMongoDBMapper {

    private static final Logger logger = LoggerFactory.getLogger(ActivityMongoDBMapper.class);

    private static final String COLLECTION_ACTIVITY = "activity"; //普通活动表
    private static final String COLLECTION_RANK_ACTIVITY = "rank_activity"; //冲榜活动表
    private static final String FIELD_ACT_ID = "actId";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_END_TIME = "endTime";
    public static final String FIELD_MEDAL_SHOW_END_TIME = "medalShowEndTime";
    public static final String FIELD_ACVITITY_STATUS = "status";
    public static final String FIELD_ANCHOR_UID = "anchorUid";

    public static final int ACVITITY_STATUS_INIT = 0;
    public static final int ACVITITY_STATUS_RUNNING = 1;
    public static final int ACVITITY_STATUS_FINESHED = 2;
    public static final String ACTIVITY_MSG = "activity_msg";
    public static final String RET_ACTIVITY = "activity";
    public static final String ACTIVITY_TYPE = "activityType";
    public static final int ACTIVITY_TYPE_COMMON = 1;
    public static final int ACTIVITY_TYPE_RANK = 2;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;


    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate; //活动表放于四级库

    // 冲榜活动 start
    private static final String FIELD_OBJECT_ID = "_id";
    private static final String FIELD_UID = "uid";
    private static final String FIELD_SHOW_END_TIME = "showEndTime";
    private static final String FIELD_RANK = "rank";
    private static final String COLLECTION_RUSH_ANCHOR_RANK = "rush_anchor_rank"; //冲榜荣耀主播uid
    private static final String COLLECTION_RUSH_GUEST_RANK = "rush_guest_rank"; //冲榜荣耀用户uid
    public static  final String  RUSH_ACTIVITY_UNIQE=  MsgDataType.S_RUSH_ACTIVITY_RANK.getValue();
    public static  final String  HOT_ACTIVITY_UNIQE=  MsgDataType.S_HOT_ACTIVITY_RANK.getValue();
    public static final String ACTIVITY_ID = "activityId";
    public static final String ANCHOR_RANK = "anchorRank";
    public static final String GUEST_RANK = "guestRank";

    // 冲榜活动 end

    /**
     * 新增活动
     * @param activity
     * @throws Exception
     */
    public void addActivity(Activity activity) throws Exception{
        if (activity == null) {
            return;
        }
        try {
            DBObject obj = javaObj2Db(mongoTemplate, activity, FIELD_ACT_ID);
            String colName = activity instanceof RankActivity?COLLECTION_RANK_ACTIVITY:COLLECTION_ACTIVITY;
            mongoTemplate.getDb().getCollection(colName).save(obj);
        } catch (Exception e){
            throw new Exception(String.format("新增活动失败,activity:%s",activity));
        }
    }

    /**
     * 推送停止活动入口消息
     */
    public void pushStopMsg(Activity activity) throws Exception {
        Long anchorUid = activity.getAnchorUid();
        if (anchorUid == null) {//全局
            long currTime = System.currentTimeMillis();
            Message msg = new Message();
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_ACTIVITY_ENTRANCE_STOP, null, null);
            msgData.put("activity", activity);
            msgData.put(FIELD_MSG_UNIQUE, ACTIVITY_MSG);// 只允许最近一条同样dataType的消息存在于信箱
            msg.setMsgData(msgData);
            msg.setStartTime(currTime);
//            msg.setEndTime();
            msg.setShouldPersist(true);
            messageMongoDBMapper.insertCtrlMsgAndPush(MessageType.PASS_THROUGH, msg);
            logger.info("stopActivity global,msg:{}",msg);
        }else{ //某个主播
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByUid(anchorUid);
            if (liveshow == null) {
                logger.info("stopActivity current anchor is not live show,activity:{}", activity);
                return;
            }
            String lid = liveshow.getLid();
            Message msg = new Message();
            Map<String, Object> msgData = Maps.newLinkedHashMap();
            msgData.put(RET_LS_LID, liveshow.getLid());
            msgData.put(FIELD_MSG_DATA_TYPE,U_LIVE_DATA.getValue());
            msgData.put("type", LsEventType.STOP_ACTIVITY_ENTRANCE.getValue());
            Map<String, Object> activityData = new HashMap<>();
            activityData.put(RET_LS_LID, liveshow.getLid());
            activityData.put(FIELD_MSG_DATA_TYPE,U_LIVE_DATA.getValue());
            activityData.put("type", LsEventType.START_ACTIVITY_ENTRANCE.getValue());
            activityData.put("uid", anchorUid);
            activityData.put("actId",activity.getActId());
            activityData.put("title", activity.getTitle());
            activityData.put("entryIconUrl",activity.getEntryIconUrl());
            activityData.put("entryIconUrl5s", activity.getEntryIconUrl5s());
            activityData.put("targetUrl", activity.getTargetUrl());
            activityData.put("title5s", activity.getTitle5s());
            activityData.put("startTime", activity.getStartTime());
            activityData.put("endTime", activity.getEndTime());
            msgData.put("activity",activityData);

            msg.setMsgData(msgData);
            // 发送广播
            messageMongoDBMapper.pushTopic(U_LIVE_DATA.getValue() + ":" + lid, MessageType.PASS_THROUGH, msg);
            logger.info("stopActivity anchorUid,msg:{}",msg);
        }

    }
    /**
     * 推送停止冲榜活动勋章消息
     * @param activity
     */
    public void pushStopRushActivityMsg(Activity activity) throws Exception {
        long currTime = System.currentTimeMillis();
//        messageMongoDBMapper.removeGlobalMsgByUniqe(RUSH_ACTIVITY_UNIQE);
        DBObject dbObject = messageMongoDBMapper.findGlobalMsgByUniqe(ActivityMongoDBMapper.RUSH_ACTIVITY_UNIQE);
        if ((dbObject!=null)&&(dbObject.get(ActivityMongoDBMapper.ACTIVITY_ID) != null) && (dbObject.get(ActivityMongoDBMapper.ACTIVITY_ID).toString().equals(activity.getActId()))) {
            messageMongoDBMapper.removeGlobalMsgByUniqe(RUSH_ACTIVITY_UNIQE);
            logger.info("pushStopRushActivityMsg:{}", activity);
            Message msg = new Message();
            Map<String, Object> msgData = MessageMongoDBMapper.genActMsgData(MsgDataType.S_STOP_RUSH_ACTIVITY_RANK, null, null);
            msgData.put(ActivityMongoDBMapper.ACTIVITY_ID, activity.getActId());
            msgData.put(FIELD_MSG_UNIQUE,MsgDataType.S_STOP_RUSH_ACTIVITY_RANK.getValue());// 只允许最近一条同样dataType的消息存在于信箱
            msg.setMsgData(msgData);
            msg.setStartTime(currTime);
//                    msg.setEndTime(rankActivity.getMedalShowEndTime().getTime());
            msg.setShouldPersist(true);
            logger.info("pushStopRushActivityMsg msg:{}", msg);
            messageMongoDBMapper.insertCtrlMsgAndPush(MessageType.PASS_THROUGH, msg);
            logger.info("pushStopRushActivityMsg delete rush activity:{}", activity);
        }

    }

    /**
     * 根据主键删除活动
     * @param actId
     * @throws Exception
     */
    public void removeCommonActById(String actId) throws Exception{
        if (StringUtils.isEmpty(actId)){
            return;
        }
        try {
            QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(actId));
            DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(queryBuilder.get());
            Activity activity = db2JavaObj(mongoTemplate, Activity.class, dbObject, FIELD_ACT_ID);
            pushStopMsg(activity);
            mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY)
                    .remove(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(actId)));
        } catch (Exception e){
            throw new Exception(String.format("删除活动失败,actId:%s",actId));
        }
    }

    /**
     * 根据主键删除冲榜活动
     * @param actId
     * @throws Exception
     */
    public void removeRankActById(String actId) throws Exception{
        if (StringUtils.isEmpty(actId)){
            return;
        }
        try {
            QueryBuilder queryBuilder = QueryBuilder.start(FIELD_OBJ_ID).is(new ObjectId(actId));
            DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).findOne(queryBuilder.get());
            Activity activity = db2JavaObj(mongoTemplate, Activity.class, dbObject, FIELD_ACT_ID);
            pushStopRushActivityMsg(activity);
            mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY)
                    .remove(new BasicDBObject(FIELD_OBJ_ID, new ObjectId(actId)));
        } catch (Exception e){
            throw new Exception(String.format("删除冲榜活动失败,actId:%s",actId));
        }
    }

    /**
     * 更新活动
     * @param activity
     * @throws Exception
     */
    public void updateActivity(Activity activity) throws Exception{
        if (activity == null || StringUtils.isEmpty(activity.getActId())) {
            return;
        }
        try {
            DBObject obj = javaObj2Db(mongoTemplate, activity, FIELD_ACT_ID);
            String colName = activity instanceof RankActivity?COLLECTION_RANK_ACTIVITY:COLLECTION_ACTIVITY;
            obj.removeField(FIELD_OBJECT_ID);
            mongoTemplate.getDb().getCollection(colName).update(new BasicDBObject("_id",new ObjectId(activity.getActId())),new BasicDBObject(OP_SET,obj));
        } catch (Exception e){
            throw new Exception(String.format("更新活动失败,activity:%s",activity));
        }
    }

    /**
     * 查询普通活动
     * @param query
     * @param pageOffset
     * @param pageSize
     * @return
     * @throws Exception
     */
    public List<Activity> findCommonActByPage(Activity query, int pageOffset, int pageSize) throws Exception {
        try {
            QueryBuilder queryBuilder = QueryBuilder.start();
            //TODO 根据传入的查询参数构造queryBuilder
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).find(queryBuilder.get());
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }
            if (pageSize > 0) {
                cursor.limit(pageSize);
            }
            List<Activity> activities = Lists.newArrayList();
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                activities.add(db2JavaObj(mongoTemplate, Activity.class, obj, FIELD_ACT_ID));
            }
            return activities;
        } catch (Exception e){
            throw new Exception(String.format("查询普通活动失败,query:%s,pageOffset:%d,pageSize:%d",query,pageOffset,pageSize));
        }
    }


    public List<RankActivity> findRankActByPage(RankActivity query, int pageOffset, int pageSize) throws Exception {
        try {
            QueryBuilder queryBuilder = QueryBuilder.start();
            //TODO 根据传入的查询参数构造queryBuilder
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).find(queryBuilder.get());
            if (pageOffset > 0) {
                cursor.skip(pageOffset);
            }
            if (pageSize > 0) {
                cursor.limit(pageSize);
            }
            List<RankActivity> activities = Lists.newArrayList();
            while (cursor.hasNext()) {
                DBObject obj = cursor.next();
                activities.add(db2JavaObj(mongoTemplate, RankActivity.class, obj, FIELD_ACT_ID));
            }
            return activities;
        } catch (Exception e){
            throw new Exception(String.format("查询冲榜活动失败,query:%s,pageOffset:%d,pageSize:%d",query,pageOffset,pageSize));
        }
    }

    public Activity findCommonActById(String actId) throws Exception{
        if (StringUtils.isEmpty(actId)){
            return null;
        }
        try{
            QueryBuilder queryBuilder = QueryBuilder.start();
            queryBuilder.and("_id").is(new ObjectId(actId));
            DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(queryBuilder.get());
            return db2JavaObj(mongoTemplate, Activity.class, dbObject, FIELD_ACT_ID);
        }catch (Exception e){
            throw new Exception(String.format("根据ID查询活动失败,actid:%s",actId));
        }
    }

    public RankActivity findRankActById(String actId) throws Exception{
        if (StringUtils.isEmpty(actId)){
            return null;
        }
        try{
            QueryBuilder queryBuilder = QueryBuilder.start();
            queryBuilder.and("_id").is(new ObjectId(actId));
            DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).findOne(queryBuilder.get());
            return db2JavaObj(mongoTemplate, RankActivity.class, dbObject, FIELD_ACT_ID);
        }catch (Exception e){
            throw new Exception(String.format("根据ID查询冲榜活动失败,actid:%s",actId));
        }
    }

    public Boolean checkTimeRange(String actId, Long anchorUid, Date startTime, Date endTime, Integer type){
        if (startTime==null||endTime==null){
            return true;
        }
        if (type==null){
            type = 1;
        }
        try{
            QueryBuilder queryBuilder = QueryBuilder.start();
            queryBuilder.or(
                    QueryBuilder.start().and(QueryBuilder.start("startTime").greaterThanEquals(startTime).get(), QueryBuilder.start("startTime").lessThan(endTime).get()).get(),
                    QueryBuilder.start().and(QueryBuilder.start("endTime").greaterThan(startTime).get(), QueryBuilder.start("endTime").lessThanEquals(endTime).get()).get(),
                    QueryBuilder.start().and(QueryBuilder.start("startTime").lessThanEquals(startTime).get(), QueryBuilder.start("endTime").greaterThanEquals(endTime).get()).get()
            );
            if (type == 1) {
                DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).find(queryBuilder.get());
                if (anchorUid == null) {//全局
//                    return cursor.hasNext();
                    while (cursor.hasNext()) {
                        Activity activity = db2JavaObj(mongoTemplate, Activity.class, cursor.next(), FIELD_ACT_ID);
                        if (activity.getAnchorUid() == null) {//全局，生效时间段不可重叠
                            if (StringUtils.isNotBlank(actId)) {
                                if (activity.getActId().equals(actId)) {
                                    continue;
                                }else{
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                    return false;
                } else {//针对某个主播设置
                    while (cursor.hasNext()) {
                        Activity activity = db2JavaObj(mongoTemplate, Activity.class, cursor.next(), FIELD_ACT_ID);
                        if (activity.getAnchorUid() != null && activity.getAnchorUid() == anchorUid.longValue()) {//同一个主播，生效时间段不可重叠
                            if (StringUtils.isNotBlank(actId)) {
                                if (activity.getActId().equals(actId)) {
                                    continue;
                                }else{
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } else {
                DBCursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).find(queryBuilder.get());
                return cursor.hasNext();
            }

        }catch (Exception e){
            logger.error("failed to check the time range if existed,startTime:{},endTime:{}",startTime,endTime,e);
            return true;
        }
    }


    public long countCommonAct(Activity query) {
        QueryBuilder queryBuilder = QueryBuilder.start();
        //TODO 根据query 构造querybuilder
        return mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).count(queryBuilder.get());
    }

    public long countRankAct(RankActivity query){
        QueryBuilder queryBuilder = QueryBuilder.start();
        //TODO 根据query 构造querybuilder
        return mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).count(queryBuilder.get());
    }

    public RankActivity getRankActForTask() throws Exception {
        try {
            Date date = new Date();
            QueryBuilder queryBuilder = QueryBuilder.start();//活动结束时间 < now < 勋章显示结束时间
            queryBuilder.and(FIELD_END_TIME).lessThanEquals(date).and(FIELD_MEDAL_SHOW_END_TIME).greaterThanEquals(date);
            DBObject obj  = mongoTemplate.getDb().getCollection(COLLECTION_RANK_ACTIVITY).findOne(queryBuilder.get());
            RankActivity activity = db2JavaObj(mongoTemplate, RankActivity.class, obj, FIELD_ACT_ID);
            return activity;
        } catch (Exception e) {
            logger.error("getRankActForTask fail",e);
        }
        return null;
    }

    /**
     * 获取一个时间段内的一个有效活动
     * @param anchorUid
     */
    public Map<String, Object> getValidActivity(Long anchorUid) throws Exception {
        if (anchorUid != null) {//有主播时候先找主播自己的入口
            Date date = new Date();
            QueryBuilder queryBuilder = QueryBuilder.start();
            queryBuilder.and(FIELD_START_TIME).lessThanEquals(date).and(FIELD_END_TIME).greaterThanEquals(date).and(FIELD_ANCHOR_UID).is(anchorUid);
            Map<String, Object> map = new HashMap<>();
            DBObject commonObj = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(queryBuilder.get());
            if (commonObj != null) {
                Activity activity = db2JavaObj(mongoTemplate, Activity.class, commonObj, FIELD_ACT_ID);
                map.put(RET_ACTIVITY, activity);
                map.put(ACTIVITY_TYPE, ACTIVITY_TYPE_COMMON);
            }
            return map;
        }else {
            Date date = new Date();
            QueryBuilder queryBuilder = QueryBuilder.start();
            queryBuilder.and(FIELD_START_TIME).lessThanEquals(date).and(FIELD_END_TIME).greaterThanEquals(date).and(FIELD_ANCHOR_UID).is(null);
            Map<String, Object> map = new HashMap<>();
            DBObject commonObj = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(queryBuilder.get());
            if (commonObj != null) {
                Activity activity = db2JavaObj(mongoTemplate, Activity.class, commonObj, FIELD_ACT_ID);
                map.put(RET_ACTIVITY, activity);
                map.put(ACTIVITY_TYPE, ACTIVITY_TYPE_COMMON);
                return map;
            }
            return map;
        }
    }

    public Activity findShouldStartActivityAndUpdate() throws Exception {
        Date date = new Date();
        QueryBuilder commonActivityQuery = QueryBuilder.start(FIELD_ACVITITY_STATUS).is(ACVITITY_STATUS_INIT).and(FIELD_START_TIME).lessThanEquals(date).and(FIELD_END_TIME).greaterThan(date);
        DBObject commonActivity = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(commonActivityQuery.get());
        //实际情况中，普通活动和冲榜活动在同一个时间合计只有一个生效的活动
        if (commonActivity != null) {
            Activity activity = db2JavaObj(mongoTemplate, Activity.class, commonActivity, FIELD_ACT_ID);
            mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).update(commonActivityQuery.get(), new BasicDBObject(OP_SET, new BasicDBObject(FIELD_ACVITITY_STATUS, ACVITITY_STATUS_RUNNING)));
            return activity;
        }
        else {
            return null;
        }
    }

    public Activity findShouldStopActivityAndUpdate() throws Exception {
        Date date = new Date();
        QueryBuilder commonActivityQuery = QueryBuilder.start(FIELD_ACVITITY_STATUS).is(ACVITITY_STATUS_RUNNING).and(FIELD_END_TIME).lessThanEquals(date);
        DBObject commonActivity = mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).findOne(commonActivityQuery.get());
        //实际情况中，普通活动和冲榜活动在同一个时间合计只有一个生效的活动
        if (commonActivity != null) {
            Activity activity = db2JavaObj(mongoTemplate, Activity.class, commonActivity, FIELD_ACT_ID);
            mongoTemplate.getDb().getCollection(COLLECTION_ACTIVITY).update(commonActivityQuery.get(), new BasicDBObject(OP_SET, new BasicDBObject(FIELD_ACVITITY_STATUS, ACVITITY_STATUS_FINESHED)));
            return activity;
        }
        else {
            return null;
        }
    }



    public static class RushUid {
        private Long uid;
        private int rank;
        private Date showEndTime;

        public Date getShowEndTime() {
            return showEndTime;
        }

        public void setShowEndTime(Date showEndTime) {
            this.showEndTime = showEndTime;
        }

        public Long getUid() {
            return uid;
        }

        public void setUid(Long uid) {
            this.uid = uid;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }

    private Map<Long, Integer> getRushUidRank(String type) throws Exception {
        Map<Long, Integer> retMap = new HashMap<>();
        if (ANCHOR_RANK.equals(type)) {
            Cursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_RUSH_ANCHOR_RANK).find();
            while (cursor.hasNext()) {
                RushUid rankUid = db2JavaObj(mongoTemplate, RushUid.class, cursor.next(), null);
                if(rankUid.getShowEndTime().before(new Date())){
                    break;
                }
                retMap.put(rankUid.getUid(), rankUid.getRank());
            }
            return retMap;
        } else if (GUEST_RANK.equals(type)) {
            Cursor cursor = mongoTemplate.getDb().getCollection(COLLECTION_RUSH_GUEST_RANK).find();
            while (cursor.hasNext()) {
                RushUid rankUid = db2JavaObj(mongoTemplate, RushUid.class, cursor.next(), null);
                if(rankUid.getShowEndTime().before(new Date())){
                    break;
                }
                retMap.put(rankUid.getUid(), rankUid.getRank());
            }
            return retMap;
        } else {
            return retMap;
        }
    }
    /**
     * 获取当前有效冲榜活动荣耀用户
     */
    public Map<Long, Integer> getCurrentValidGuestRank()  throws Exception{
        return getRushUidRank(GUEST_RANK);
    }
    /**
     * 获取当前有效冲榜活动荣耀主播
     */
    public Map<Long, Integer> getCurrentValidAnchorRank()  throws Exception{
        return getRushUidRank(ANCHOR_RANK);
    }

    public void removeAnchorAndGuestRank(String activityId) {
        mongoTemplate.getDb().getCollection(COLLECTION_RUSH_ANCHOR_RANK).remove(new BasicDBObject(FIELD_ACT_ID,activityId));
        mongoTemplate.getDb().getCollection(COLLECTION_RUSH_GUEST_RANK).remove(new BasicDBObject(FIELD_ACT_ID,activityId));
    }

    public void addAnchorRank(List<Long> anchorList, Date medalShowEndTime, String actId) throws Exception{
        try {
            mongoTemplate.getDb().getCollection(COLLECTION_RUSH_ANCHOR_RANK).remove(new BasicDBObject());
            for(int i =0;i<anchorList.size();i++) {
                mongoTemplate.getDb().getCollection(COLLECTION_RUSH_ANCHOR_RANK).insert(new BasicDBObject(FIELD_OBJECT_ID,anchorList.get(i)).append(FIELD_RANK,i+1).append(FIELD_UID,anchorList.get(i)).append(FIELD_SHOW_END_TIME,medalShowEndTime).append(FIELD_ACT_ID,actId));
            }
        } catch (Exception e){
            logger.error("addAnchorRank fail ,anchorList:{}",anchorList);
            throw new Exception("addAnchorRank fail "+e.getMessage());
        }
    }
    public void addGuestRank(List<Long> guestList, Date medalShowEndTime, String actId) throws Exception{
        try {
            mongoTemplate.getDb().getCollection(COLLECTION_RUSH_GUEST_RANK).remove(new BasicDBObject());
            for(int i =0;i<guestList.size();i++) {
                mongoTemplate.getDb().getCollection(COLLECTION_RUSH_GUEST_RANK).insert(new BasicDBObject(FIELD_OBJECT_ID,guestList.get(i)).append(FIELD_RANK,i+1).append(FIELD_UID,guestList.get(i)).append(FIELD_SHOW_END_TIME,medalShowEndTime).append(FIELD_ACT_ID,actId));
            }
        } catch (Exception e){
            logger.error("addGuestRank fail ,guestList:{}",guestList);
            throw new Exception("addGuestRank fail "+e.getMessage());
        }
    }
}
