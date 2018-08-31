package com.yy.me.web.dao;

import static com.yy.me.mongo.MongoUtil.*;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.yy.me.enums.ReportType;

/**
 * db.report_msg.ensureIndex({"rtype":1})
 * db.report_msg.ensureIndex({"rtype":1,"fid":1,"reporterId":1})
 * db.report_msg.ensureIndex({"rtype":1,"cid":1,"reporterId":1})
 * 
 * @author Jiang Chengyan
 * 
 */
@Repository
public class ReportMongoDBMapper {
    
    /**
     * 举报
     */
    public static final String COLLETION_REPORT_NAME = "report_msg";
    
    public static final String FIELD_RP_SNAPSHOT_URL = "snapshotUrl";
    public static final String FIELD_RP_CONTENT = "reportContent";
    public static final String FIELD_RP_ID = "reportId";
    public static final String FIELD_RP_RID = "reporterId";
    public static final String FIELD_RP_U_UID = "uid";
    public static final String FIELD_RP_F_FID = "fid";
    public static final String FIELD_RP_FC_CID = "cid";
    public static final String FIELD_RP_LS_LID = "lid";
    public static final String FIELD_RP_TYPE = "rtype";
    public static final String FIELD_RP_DIFF_REPORTER_COUNT = "diffReporterCount";

    @Autowired
    @Qualifier("mongoTemplateLiveshow")
    private MongoTemplate mongoTemplate;
    
    public boolean countDiffUserReportGreater5(String fid) throws Exception {
        int max = 5;
        // db.report_msg.aggregate([{ "$match" : { "fid" : ObjectId("567cfb3397e4696ec13cc3b9") , "rtype" : 2}}, { "$group" : { "_id" : "$reporterId" , "diffReporterCount" : { "$sum" : 1}}}])
        QueryBuilder query = QueryBuilder.start(FIELD_RP_F_FID).is(new ObjectId(fid)).and(FIELD_RP_TYPE).is(ReportType.FEED.getValue());
        DBObject matchObj = new BasicDBObject(OP_MATCH, query.get());
        DBObject groupObj = new BasicDBObject(OP_GROUP, QueryBuilder.start().and(FIELD_OBJ_ID).is("$" + FIELD_RP_RID)
                .and(FIELD_RP_DIFF_REPORTER_COUNT).is(new BasicDBObject(OP_SUM, 1)).get());
        List<DBObject> queryObjs = Lists.newArrayList(matchObj, groupObj);
        Cursor cur = mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME)
                .aggregate(queryObjs, AggregationOptions.builder().batchSize(max + 1).build(), ReadPreference.primary());// TODO
        int i = 0;
        for (; i <= max && cur.hasNext(); i++, cur.next()) {// 超过5个用户举报，再次送审
        }
        return i == max ? true : false;
    }

    public void insertUser(long reporterId, long uid, String reportContent) throws Exception {
        BasicDBObject obj = genWithObjId().append(FIELD_RP_RID, reporterId).append(FIELD_RP_U_UID, uid).append(FIELD_RP_TYPE, ReportType.USER.getValue())
                .append(FIELD_CREATE_TIME, new Date());
        if (StringUtils.isNotBlank(reportContent)) {
            obj.append(FIELD_RP_CONTENT, reportContent);
        }
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).insert(obj);
    }

    public void insertFeed(long reporterId, String fid, String reportContent) throws Exception {
        BasicDBObject obj = genWithObjId().append(FIELD_RP_RID, reporterId).append(FIELD_RP_F_FID, new ObjectId(fid)).append(FIELD_RP_TYPE, ReportType.FEED.getValue())
                .append(FIELD_CREATE_TIME, new Date());
        if (StringUtils.isNotBlank(reportContent)) {
            obj.append(FIELD_RP_CONTENT, reportContent);
        }
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).insert(obj, WriteConcern.ACKNOWLEDGED);
    }

    public void insertFeedComment(long reporterId, String cid, String reportContent) throws Exception {
        BasicDBObject obj = genWithObjId().append(FIELD_RP_RID, reporterId).append(FIELD_RP_FC_CID, new ObjectId(cid)).append(FIELD_RP_TYPE, ReportType.FEED_COMMENT.getValue())
                .append(FIELD_CREATE_TIME, new Date());
        if (StringUtils.isNotBlank(reportContent)) {
            obj.append(FIELD_RP_CONTENT, reportContent);
        }
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).insert(obj);
    }

    public void insertLiveShow(long reporterId, String lid, String reportContent, String snapshotUrl) throws Exception {
        BasicDBObject obj = genWithObjId().append(FIELD_RP_RID, reporterId).append(FIELD_RP_LS_LID, new ObjectId(lid)).append(FIELD_RP_TYPE, ReportType.LIVESHOW.getValue())
                .append(FIELD_CREATE_TIME, new Date());
        if (StringUtils.isNotBlank(reportContent)) {
            obj.append(FIELD_RP_CONTENT, reportContent);
        }
        if (StringUtils.isNotBlank(snapshotUrl)) {
            obj.append(FIELD_RP_SNAPSHOT_URL, snapshotUrl);
        }
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).insert(obj);
    }
    
    public void deleteReport4User(long uid) {
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).remove(new BasicDBObject(FIELD_RP_U_UID, uid));
    }
    
    public void deleteReport4Feed(String fid) {
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).remove(new BasicDBObject(FIELD_RP_F_FID, new ObjectId(fid)));
    }
    
    public void deleteReport4LiveShow(String lid) {
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).remove(new BasicDBObject(FIELD_RP_LS_LID, new ObjectId(lid)));
    }
    
    public void deleteReport4FeedComment(String cid) {
        mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).remove(new BasicDBObject(FIELD_RP_FC_CID, new ObjectId(cid)));
    }

    public List<DBObject> find(int skip, int limit, Integer rtype) throws Exception {
        QueryBuilder query = QueryBuilder.start();
        if (rtype != null) {
            query.and(FIELD_RP_TYPE).is(rtype);
        }
        DBCursor cur = mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).find(query.get()).sort(new BasicDBObject(FIELD_OBJ_ID, -1)).skip(skip)
                .limit(limit);
        if (!cur.hasNext()) {
            return null;
        }
        List<DBObject> ret = Lists.newArrayList();
        while (cur.hasNext()) {
            ret.add(cur.next());
        }
        return ret;
    }

    public long count(Integer rtype) {
        QueryBuilder query = QueryBuilder.start();
        if (rtype != null) {
            query.and(FIELD_RP_TYPE).is(rtype);
        }
        return mongoTemplate.getDb().getCollection(COLLETION_REPORT_NAME).count(query.get());
    }
}