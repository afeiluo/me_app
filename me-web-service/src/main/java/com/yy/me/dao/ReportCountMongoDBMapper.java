package com.yy.me.dao;

import com.google.common.collect.*;
import com.mongodb.*;
import com.yy.me.enums.ReportProcessStatus;
import com.yy.me.enums.ReportStatus;
import com.yy.me.enums.ReportFrom;
import com.yy.me.enums.ReportResult;
import com.yy.me.time.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * V3.1 举报 ,封禁
 */
@Repository
public class ReportCountMongoDBMapper {

    private final static Logger logger = LoggerFactory.getLogger(ReportCountMongoDBMapper.class);

    /**
     * 举报历史
     */
    public static final String COLLETION_REPORT_HISTORY = "report_history";
    /**
     * 举报次数
     */
    public static final String COLLETION_REPORT_COUNT = "report_count";
    /**
     * 恶意举报用户，该用户的举报是无效的
     */
    public static final String COLLETION_REPORT_EVIL_USER = "report_evil_user";

    public static final String FIELD_ID = "_id";
    public static final String FIELD_FROM_UID = "fromUid";
    public static final String FIELD_TO_UID = "toUid";
    public static final String FIELD_CREATE_TIME = "createTime";
    public static final String FIELD_UPDATE_TIME = "updateTime";
    public static final String FIELD_SNAPSHOT_URL = "snapshotUrl";
    public static final String FIELD_GONG_PING = "gongPing";
    public static final String FIELD_LID = "lid";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_STATUS = "status";//违规，不违规
    public static final String FIELD_PROCESS_STATUS = "processStatus";//是否已处理的状态
    public static final String FIELD_INCR_COUNT = "incrCount";//举报增加次数
    public static final String FIELD_REPORT_FROM = "reportFrom";//举报来源 com.yy.me.enums.ReportFrom
    public static final String FIELD_GONGPING_IM_VIOLATE_TEXT = "gongPingImViolateText";//从系统给出的公屏或IM举报入口举报时候的违规文字


    public static final String FIELD_REPORT_COUNT = "reportCount";//被举报统计次数

    public static final String FIELD_EVIL_REPORT_ID = "evilReportId";

    public static final double WARN_COUNT = 1;
    public static final double FIRST_FORBID_COUNT = 2;
    public static final double SECOND_FORBID_COUNT = 4;

    public static final int MAX_DAY_REPORT_COUNT = 10;//同一个账号，一天最多只能举报10次
    public static final int MAX_FIVE_DAY_REPORT_COUNT = 50;//同一个账号，最近5天举报次数50次

    public String findTips(ReportResult result, long toUid) {
        if (result == ReportResult.WARN) {
            DBObject dbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findOne(new BasicDBObject(FIELD_TO_UID, toUid)
                            .append(FIELD_STATUS, ReportStatus.ILLEGAL.getStatus()), null,
                    new BasicDBObject(FIELD_CREATE_TIME, 1));
            return (String) dbObject.get(FIELD_REASON);
        } else if ((result == ReportResult.FIRST_FORBID) || (result == ReportResult.SECOND_FORBID)) {
            DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).find(new BasicDBObject(FIELD_TO_UID, toUid)
                    .append(FIELD_STATUS, ReportStatus.ILLEGAL.getStatus()));
            List<String> reasonList = new ArrayList<>();
            while (cursor.hasNext()) {
                reasonList.add((String) cursor.next().get(FIELD_REASON));
            }
            //淫秽色情 , 恶意骚扰、骂人 ,垃圾广告
            String reason = Multisets.copyHighestCountFirst(HashMultiset.create(reasonList)).elementSet().iterator().next();
//            if ("淫秽色情".equals(reason)) {
//                return "发送淫秽色情内容";
//            } else if ("恶意骚扰、骂人".equals(reason)) {
//                return "恶意骚扰";
//            } else if ("垃圾广告".equals(reason)) {
//                return "发送垃圾广告";
//            }
            return reason;
        }
        return null;
    }

    /**
     * 解封一级封禁用户
     */
    public List<Long> findFirstBandUids() {
        List<Long> uidList = new ArrayList<>();
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_REPORT_COUNT).find(new BasicDBObject( FIELD_REPORT_COUNT, new BasicDBObject(OP_GTE,FIRST_FORBID_COUNT)));
        while (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            uidList.add((Long) dbObject.get(FIELD_TO_UID));
        }
        return uidList;
    }


    @Autowired
    @Qualifier("mongoTemplateLiveshow")
    private MongoTemplate mongoTemplate;

    /**
     * @param fromUid
     * @param toUid
     * @return
     */
    public boolean isAlreadyReportFromHomePageOrImRightMenu(long fromUid, long toUid) {
        BasicDBObject repeatQuery = new BasicDBObject(FIELD_FROM_UID, fromUid).append(FIELD_TO_UID, toUid);
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).find(repeatQuery);
        while (cursor.hasNext()) {
            DBObject obj = cursor.next();
            if (ReportFrom.HOME_PAGE.getCode() == (Integer) obj.get(FIELD_REPORT_FROM) || ReportFrom.IM_RIGHT_MENU.getCode() == (Integer) obj.get(FIELD_REPORT_FROM)) {
                logger.info("already reported from homepage or im right menu,fromUid:{},toUid:{}", fromUid, toUid);
                return true;
            }
        }
        return false;
    }

    public ReportResult report(int from, String gongPingImViolateText, Long fromUid, Long toUid, String snapshotUrl, String gongPing, String lid, String reason, int activeCount) {
        logger.info("report from:{},fromUid:{},toUid:{},lid:{},reason:{},activeCount:{},gongPing:{},gongPingImViolateText:{}",from,fromUid,toUid,lid,reason,activeCount,gongPing,gongPingImViolateText);
        double incrCount =1;
        Date date = new Date();
        if (activeCount < 1) {
            logger.info("not active,fromUid:{},toUid:{},snapshotUrl:{},gongPing:{},lid:{},reason:{},activeCount:{}", fromUid, toUid, snapshotUrl, gongPing, lid, reason, activeCount);
            return ReportResult.NOT_ACTIVE;//活跃天数不够
        }
        if (from == ReportFrom.HOME_PAGE.getCode()) {
            if(isAlreadyReportFromHomePageOrImRightMenu(fromUid,toUid)){//在用户个人主页（包括个人主页和小资料卡）或IM右上角的隐藏菜单，举报总共只算0.5次
                return ReportResult.ALREADY_REPORTED_FROM_HOME_PAGE_OR_IM_RIGHT_MENU;
            }
            incrCount = 0.5;
        } else if (from == ReportFrom.IM_RIGHT_MENU.getCode()) {
            if(isAlreadyReportFromHomePageOrImRightMenu(fromUid,toUid)){//在用户个人主页（包括个人主页和小资料卡）或IM右上角的隐藏菜单，举报总共只算0.5次
                return ReportResult.ALREADY_REPORTED_FROM_HOME_PAGE_OR_IM_RIGHT_MENU;
            }
            incrCount = 0.5;
        } else if (from == ReportFrom.GONG_PING.getCode()) {
            BasicDBObject gongPingQuery = new BasicDBObject(FIELD_LID, lid).append(FIELD_TO_UID, toUid).append(FIELD_GONGPING_IM_VIOLATE_TEXT, gongPingImViolateText);
            DBObject gongPingDbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findOne(gongPingQuery);
            if (gongPingDbObject != null) {
                return ReportResult.GONGPING_REPEAT_REPORT;//在系统给出的公屏或IM举报入口举报，一个入口不论几个人举报只算一次，一个人从多个举报入口举报算多次
            }
            incrCount = 1;
        } else if (from == ReportFrom.IM_REPORT_ENTRANCE.getCode()) {
            BasicDBObject imQuery = new BasicDBObject(FIELD_FROM_UID, fromUid).append(FIELD_TO_UID,toUid).append(FIELD_GONGPING_IM_VIOLATE_TEXT, gongPingImViolateText);
            DBObject imDbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findOne(imQuery);
            if (imDbObject != null) {
                return ReportResult.IM_REPEAT_REPORT;//在系统给出的公屏或IM举报入口举报，一个入口不论几个人举报只算一次，一个人从多个举报入口举报算多次
            }
            incrCount = 1;
        }
        DBObject evilObj = mongoTemplate.getDb().getCollection(COLLETION_REPORT_EVIL_USER).findOne(new BasicDBObject(FIELD_FROM_UID, fromUid));
        if (evilObj != null) {
            return ReportResult.EVIL_USER;//被认定为恶意举报用户，他的举报无效
        }

//        BasicDBObject repeatQuery = new BasicDBObject(FIELD_FROM_UID, fromUid).append(FIELD_TO_UID, toUid);
//        DBObject repeatDbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findOne(repeatQuery);
//        if (repeatDbObject != null) {
//            logger.info("already reported,fromUid:{},toUid:{},snapshotUrl:{},gongPing:{},lid:{},reason:{},activeCount:{}", fromUid, toUid, snapshotUrl, gongPing, lid, reason, activeCount);
//            return ReportResult.ALREADY_REPORTED;//历史上已经举报过
//        }
//        DBObject lastestDbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findOne(new BasicDBObject(FIELD_TO_UID, toUid), null,
//                new BasicDBObject(FIELD_CREATE_TIME, -1));
//        if (lastestDbObject != null && ((Date) lastestDbObject.get(FIELD_CREATE_TIME)).after(DateTimeUtil.addMinutes(new Date(), -3))) {//在3分钟内，对同一个用户的多次举报只算一次
//            logger.info("report in shot time,fromUid:{},toUid:{},snapshotUrl:{},gongPing:{},lid:{},reason:{},activeCount:{}",fromUid,toUid,snapshotUrl,gongPing,lid,reason,activeCount);
//            return ReportResult.SHORT_TIME_REPORT;//在3分钟内，对同一个用户的多次举报无效
//        }

        DBObject oneDayQuery = new BasicDBObject(FIELD_CREATE_TIME, new BasicDBObject(OP_GTE, DateTimeUtil.getBeginOfDate(date))).append(FIELD_FROM_UID, fromUid);
        long oneDayQueryCount = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).count(oneDayQuery);
        if (oneDayQueryCount >= MAX_DAY_REPORT_COUNT) {
            return ReportResult.MAX_DAY_REPORT_COUNT;//同一个账号，一天最多只能举报10次
        }

        DBObject fiveDayQuery = new BasicDBObject(FIELD_CREATE_TIME, new BasicDBObject(OP_GTE, DateTimeUtil.getBeginOfDate(DateTimeUtil.addDays(date,-5)))).append(FIELD_FROM_UID, fromUid);
        long fiveDayQueryCount = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).count(fiveDayQuery);
        if (fiveDayQueryCount > MAX_FIVE_DAY_REPORT_COUNT) {
            mongoTemplate.getDb().getCollection(COLLETION_REPORT_EVIL_USER).insert
                    (new BasicDBObject(FIELD_FROM_UID, fromUid).append(FIELD_CREATE_TIME, new Date()));
            return ReportResult.MAX_FIVE_DAY_REPORT_COUNT;//同一个账号，最近5天如果举报次数达到50次,则该用户以后所有的举报都不再生效
        }


        BasicDBObject query = new BasicDBObject(FIELD_TO_UID, toUid);
//        DBObject oldObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_COUNT).findOne(query);
//        if (oldObject != null && (Double) oldObject.get(FIELD_REPORT_COUNT) >= SECOND_FORBID_COUNT) {
//            return ReportResult.MAX_REPORT_COUNT;
//        }

        BasicDBObject inc = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_REPORT_COUNT, incrCount));
        BasicDBObject set = inc.append(OP_SET, new BasicDBObject(FIELD_UPDATE_TIME, date)).append(OP_SET_ON_INSERT, new BasicDBObject(FIELD_CREATE_TIME, date));
        DBObject dbObject = mongoTemplate.getDb().getCollection(COLLETION_REPORT_COUNT).findAndModify(query, null, null, false, set, true, true);
        double currentReportCount = (Double) dbObject.get(FIELD_REPORT_COUNT);

        BasicDBObject insert = new BasicDBObject(FIELD_FROM_UID, fromUid).append(FIELD_TO_UID, toUid).append(FIELD_CREATE_TIME, date)
                .append(FIELD_REASON, reason)
                .append(FIELD_STATUS, ReportStatus.ILLEGAL.getStatus())
                .append(FIELD_INCR_COUNT,incrCount).append(FIELD_REPORT_FROM,from);
        if (ReportFrom.IM_REPORT_ENTRANCE.getCode() == from || ReportFrom.GONG_PING.getCode() == from) {//从公屏或IM的系统提示入口举报成功的，该条记录自动设定为“违规”，且没有操作按钮
            insert.append(FIELD_PROCESS_STATUS, ReportProcessStatus.DONE.getCode());
        } else {
            insert.append(FIELD_PROCESS_STATUS, ReportProcessStatus.UNDONE.getCode());
        }
        if(StringUtils.isNotBlank(snapshotUrl)){
            insert.append(FIELD_SNAPSHOT_URL, snapshotUrl);
        }
        if(StringUtils.isNotBlank(gongPing)){
            insert.append(FIELD_GONG_PING, gongPing);
        }
        if(StringUtils.isNotBlank(lid)){
            insert.append(FIELD_LID, lid);
        }
        if(StringUtils.isNotBlank(gongPingImViolateText)){
            insert.append(FIELD_GONGPING_IM_VIOLATE_TEXT,gongPingImViolateText);
        }
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).insert(insert, WriteConcern.ACKNOWLEDGED);

        if (currentReportCount >= SECOND_FORBID_COUNT) {//二级封禁
            return ReportResult.SECOND_FORBID;
        } else if ((currentReportCount >= FIRST_FORBID_COUNT) && (currentReportCount - incrCount < FIRST_FORBID_COUNT)) {//一级封禁
            return ReportResult.FIRST_FORBID;
        } else if ((currentReportCount >= WARN_COUNT) && (currentReportCount - incrCount < WARN_COUNT)) {//一次举报发警告
            return ReportResult.WARN;
        }
        return ReportResult.UNKONWN;
    }

    /**
     * 查询从公屏或IM的系统举报入口的举报是否满足二级封禁。
     *
     * @param uid
     * @return
     */
    public boolean isAllReportFromGongpingAndIm(Long uid) {
        DBCursor cursor = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).find(new BasicDBObject(FIELD_TO_UID, uid));
        Double totalCount = 0.0;
        while (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            int from = (Integer) dbObject.get(FIELD_REPORT_FROM);
            if (ReportFrom.GONG_PING.getCode() != from && ReportFrom.IM_REPORT_ENTRANCE.getCode() != from) {//非公屏或非IM的系统提示入口
                return false;
            }
            if (ReportStatus.ILLEGAL.getStatus() == (Integer) dbObject.get(FIELD_STATUS)) {//违规的
                totalCount += (Double) dbObject.get(FIELD_INCR_COUNT);
            }
        }
        return totalCount >= SECOND_FORBID_COUNT;
    }

    public DBObject unViolateReportHistory(String objectId) {
        DBObject history = mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findAndModify(new BasicDBObject(FIELD_ID,new ObjectId(objectId)),
                null,null,false,new BasicDBObject(OP_SET,new BasicDBObject(FIELD_STATUS, ReportStatus.LEGAL.getStatus())),true,false);
        Long fromUid = (Long) history.get(FIELD_FROM_UID);
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_EVIL_USER).insert
                (new BasicDBObject(FIELD_EVIL_REPORT_ID, objectId).append(FIELD_FROM_UID, fromUid).append(FIELD_CREATE_TIME, new Date()));
        return history;
    }


    public void descReportCount(Long toUid, Double incrCount) {
        BasicDBObject query = new BasicDBObject(FIELD_TO_UID, toUid);
        BasicDBObject desc = new BasicDBObject(OP_INC, new BasicDBObject(FIELD_REPORT_COUNT, -incrCount));
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_COUNT).findAndModify(query,desc);
    }

    public void doneProcess(String objectId) {
        BasicDBObject update = new BasicDBObject(OP_SET, new BasicDBObject(FIELD_STATUS, ReportProcessStatus.DONE));
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_HISTORY).findAndModify(new BasicDBObject(FIELD_ID, new ObjectId(objectId)),
                update);
    }
}