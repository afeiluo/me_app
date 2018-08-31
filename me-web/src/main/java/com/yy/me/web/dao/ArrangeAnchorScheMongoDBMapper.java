package com.yy.me.web.dao;

import static com.yy.me.mongo.MongoUtil.OP_INC;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.time.DateTimeUtil;

@Repository
public class ArrangeAnchorScheMongoDBMapper {

	private static final String COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY="arrange_schedule_bucket_day";
	private static final String FIELD_START_TIME_DATE="startTimeDate";
	private static final String FIELD_END_TIME_DATE="endTimeDate";
	private static final String FIELD_ALL_PERSON="allPerson";
	
	
	private static final String COLLECTION_ARRANGE_SCHEDULE_GRAB="arrange_schedule_grab";
	private static final String FIELD_BID="bid";
	private static final String FIELD_CITY="city";
	private static final String FIELD_UID="uid";
	private static final String FIELD_GRAB_PERSON="grabPerson";
	private static final String FIELD_DAY_TIME_STR="dayTimeStr";
	private static final String FIELD_ARRANGE_START_TIME="arrangeStartTime";
	private static final String FIELD_ARRANGE_END_TIME="arrangeEndTime";
	private static final String FIELD_VALID_LIVE_TIME_MIN="validLiveTimeMin";
	private static final String FIELD_CREATE_TIME="createTime";
	private static final String FIELD_IS_VALID="isValid";
	
	private static final String COLLECTION_ANCHOR_LIVE="anchor_live";
	private static final String FIELD_ARRANGED="arranged";
	
	public static final Logger logger = LoggerFactory.getLogger(ArrangeAnchorScheMongoDBMapper.class);
	
	@Autowired
	@Qualifier("mongoTemplate")
	private MongoTemplate mongoTemplate;
	
	
	
	
	/**
	 * 提交过来的bid是否有效
	 * @param bucid
	 * @return
	 */
	public  boolean isTheBidIsValid(String bucid,String dayTimeStr,String city){
		DBObject selectObj=new BasicDBObject(MongoUtil.FIELD_OBJ_ID,new ObjectId(bucid));
		selectObj.put(FIELD_DAY_TIME_STR, dayTimeStr);
		selectObj.put(FIELD_CITY, city);
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY).findOne(selectObj)==null?false:true;
	}
	
	
	public boolean isTheOldBidInGrab(Long uid,String oldBid){
		QueryBuilder querySelect=QueryBuilder.start();
		querySelect.and(FIELD_BID).is(oldBid);
		querySelect.and(FIELD_UID).is(uid);
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).findOne(querySelect.get())==null?false:true;
	}
	
	
	/**
	 * 获取本周内的主播考勤情况
	 * @param uid
	 * @param weekStartDate
	 * @param weekEndDate
	 * @return
	 */
	public DBCursor getWeekAttendanceList(Long uid,Date weekStartDate,Date weekEndDate){
		QueryBuilder query=QueryBuilder.start();
		query.and(FIELD_UID).is(uid);
		query.and(FIELD_ARRANGE_START_TIME).greaterThanEquals(weekStartDate);
		query.and(FIELD_ARRANGE_START_TIME).lessThan(weekEndDate);
		
		DBObject sortObj=new BasicDBObject(FIELD_ARRANGE_START_TIME,1);
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).find(query.get()).sort(sortObj);
		
		
	}
	
	
	
	
	/**
	 * 抢排班
	 * @param bid
	 * @param city
	 */
	public DBObject grabScheForLive(String bid){
		
		QueryBuilder query = QueryBuilder.start();
        query.and(MongoUtil.FIELD_OBJ_ID).is(new ObjectId(bid));
        
		 DBObject DbObjectForAllperson=mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY).findOne(query.get());
       
        query.and(FIELD_GRAB_PERSON).lessThan(MongoUtil.getInt(DbObjectForAllperson.get(FIELD_ALL_PERSON), 0));
              
        DBObject fields = new BasicDBObject(FIELD_START_TIME_DATE, 1);
        fields.put(FIELD_END_TIME_DATE, 1);
        fields.put(FIELD_DAY_TIME_STR, 1);
        BasicDBObject updateObj = new BasicDBObject(OP_INC,
                new BasicDBObject(FIELD_GRAB_PERSON, 1));
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY)
                .findAndModify(query.get(), fields, null, false, updateObj, false, false, WriteConcern.ACKNOWLEDGED);
		
	}
	
	/**
	 * 减少抢班人数
	 * @param uid
	 * @param city
	 * @param dayTimeStr
	 * @param buckid
	 * @throws Exception 
	 */
	public DBObject reduceArrangeGrabCount(String buckid,int reduceInt) {
		QueryBuilder query = QueryBuilder.start();
        query.and(MongoUtil.FIELD_OBJ_ID).is(new ObjectId(buckid));
        query.and(FIELD_GRAB_PERSON).greaterThan(0);
       
        DBObject fields = new BasicDBObject(FIELD_START_TIME_DATE, 1);
        fields.put(FIELD_END_TIME_DATE, 1);
        fields.put(FIELD_DAY_TIME_STR, 1);
        BasicDBObject updateObj = new BasicDBObject(OP_INC,
                new BasicDBObject(FIELD_GRAB_PERSON, -reduceInt));
		//回滚数量-1
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY)
                .findAndModify(query.get(), fields, null, false, updateObj, false, false, WriteConcern.ACKNOWLEDGED);
	}

	
	/**
	 * 从排班明细中移除 根据daytimeStr（不减数量）
	 * @param uid
	 * @param city
	 * @param dayTimeStr
	 * @throws Exception 
	 */
	public void removeArrangeGrabFromDayStr(Long uid,String city,String dayTimeStr){
		QueryBuilder removeQuery = QueryBuilder.start();
		removeQuery.and(FIELD_DAY_TIME_STR).is(dayTimeStr);
		removeQuery.and(FIELD_CITY).is(city);
		removeQuery.and(FIELD_UID).is(uid);
		
		//回滚删除
		mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).remove(removeQuery.get());
	}
	

	
	/**
	 * 填充排班信息（抢班成功的前提下）
	 * @param uid 主播id
	 * @param bid bucketid
	 * @param oldBid  旧的bucketid（改班的时候用 抢班的时候设为null）
	 * @param city  直播城市 
	 * @param grabDBObjetct  上文抢班成功返回的结果集
	 * @return
	 */
	public WriteResult grabScheForLiveSaveInfo(Long uid,String bid,String oldBid,String city,DBObject grabDBObjetct) {
			
			QueryBuilder queryUpdate=QueryBuilder.start();
			queryUpdate.and(FIELD_UID).is(uid);
			queryUpdate.and(FIELD_BID).is(bid);
			queryUpdate.and(FIELD_ARRANGE_START_TIME).is((Date)grabDBObjetct.get(FIELD_START_TIME_DATE));
			queryUpdate.and(FIELD_ARRANGE_END_TIME).is((Date)grabDBObjetct.get(FIELD_END_TIME_DATE));			
			queryUpdate.and(FIELD_VALID_LIVE_TIME_MIN).is(0);
			queryUpdate.and(FIELD_CITY).is(city);
			queryUpdate.and(FIELD_IS_VALID).is(0);
			queryUpdate.and(FIELD_DAY_TIME_STR).is(grabDBObjetct.get(FIELD_DAY_TIME_STR).toString());
			queryUpdate.and(FIELD_CREATE_TIME).is(new Date());
			
			QueryBuilder querySelect=QueryBuilder.start();
			if(StringUtils.isBlank(oldBid)){
				querySelect.and(FIELD_BID).is(bid);
				querySelect.and(FIELD_UID).is(uid);
				return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).update(querySelect.get(), queryUpdate.get(), true, false);
			}else{
				querySelect.and(FIELD_BID).is(oldBid);
				querySelect.and(FIELD_UID).is(uid);
				return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).update(querySelect.get(), queryUpdate.get(), false, false);
			}
			
			
		
	}
	
	/**
	 * 获取某一天主播的bucket选择记录
	 * @param uid
	 * @param city
	 * @param weekDay
	 * @return
	 */
	public List<String> getAnchorWeekBucketForday(Long uid,String city,String weekDay){
		QueryBuilder queryBuild=QueryBuilder.start();
		queryBuild.and(FIELD_DAY_TIME_STR).is(weekDay);
		queryBuild.and(FIELD_CITY).is(city);
		queryBuild.and(FIELD_UID).is(uid);
		
		DBCursor dbCursor=mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).find(queryBuild.get());
		List<String> bidList=Lists.newArrayList();
		while(dbCursor.hasNext()){
			DBObject dbObject=dbCursor.next();
			bidList.add(dbObject.get(FIELD_BID).toString());
		}
		return bidList;
	}
	
	/**
	 * 查询一周的bucket
	 * @param uid
	 * @param city
	 * @param resultWeekStrArray
	 * @return
	 */
	public ArrayNode weekBucketForday(String city,String weekDay){
		QueryBuilder queryBuild=QueryBuilder.start();
		queryBuild.and(FIELD_DAY_TIME_STR).is(weekDay);
		queryBuild.and(FIELD_CITY).is(city);
		
		DBObject sortObj=new BasicDBObject();
		sortObj.put(FIELD_START_TIME_DATE, 1);
		DBCursor dbCursor=mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_BUCKET_DAY).find(queryBuild.get()).sort(sortObj);
		ArrayNode resultArray=BaseServletUtil.getLocalObjMapper().createArrayNode();
		
		while(dbCursor.hasNext()){
			DBObject dbObject=dbCursor.next();
			ObjectNode objectNode=BaseServletUtil.getLocalObjMapper().createObjectNode();
			objectNode.put("id",dbObject.get(MongoUtil.FIELD_OBJ_ID).toString());
			String bucketStarStr=dbObject.containsField(FIELD_START_TIME_DATE)?DateTimeUtil.formatDate((Date)dbObject.get(FIELD_START_TIME_DATE), "HH:mm"):null;
			String bucketEndStr=dbObject.containsField(FIELD_END_TIME_DATE)?DateTimeUtil.formatDate((Date)dbObject.get(FIELD_END_TIME_DATE), "HH:mm"):null;
			objectNode.put("bucketStarStr",bucketStarStr);
			objectNode.put("bucketEndStr",bucketEndStr);
			objectNode.put("bucketStr", bucketStarStr.concat("~").concat(bucketEndStr));
			objectNode.put("allPerson", MongoUtil.getInt(dbObject.get(FIELD_ALL_PERSON), 0));
			objectNode.put("grabPerson", MongoUtil.getInt(dbObject.get(FIELD_GRAB_PERSON), 0));
			
			resultArray.add(objectNode);
		}
		
		return resultArray;
	}
	
	
	
	/**
	 * 根据uid和天数日期，获取旧的排班明细
	 * @param uid
	 * @param dayTimeStr
	 * @return
	 */
	public DBCursor getArrangeScheInfoFromDayTimeStr(Long uid,String dayTimeStr){
		DBObject dbObject=new BasicDBObject(FIELD_DAY_TIME_STR, dayTimeStr);
		dbObject.put(FIELD_UID,uid);
		return mongoTemplate.getDb().getCollection(COLLECTION_ARRANGE_SCHEDULE_GRAB).find(dbObject);
	}
	
	/**
	 * 判断该主播是否为排班主播
	 * @param uid
	 * @return
	 */
	public boolean isArrangeAnchor(Long uid){
		DBObject obj=new BasicDBObject(FIELD_UID,uid);
		DBObject resultObj=mongoTemplate.getDb().getCollection(COLLECTION_ANCHOR_LIVE).findOne(obj);
		return resultObj==null?false:resultObj.containsField(FIELD_ARRANGED)?(boolean)resultObj.get(FIELD_ARRANGED):false;
		//return true;
	}
	
	
	
	
}
