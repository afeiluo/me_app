package com.yy.me.web.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.QueryBuilder;
import com.yy.me.message.FormatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Repository
public class ReportAppealMongoDBMapper {

	private static final Logger logger = LoggerFactory.getLogger(PopupAdMongoDBMapper.class);
	
	@Autowired
	@Qualifier("mongoTemplateLiveshow")
	private MongoTemplate mongoTemplate;
	
	private static String COLLECTION_REPORTCOUNT="report_count";
	private static String FIELD_TOUID="toUid";
	
	private static String COLLECTION_REPORTAPPEAL="report_appeal";
	private static String FIELD_UID="uid";
	private static String FIELD_APPRALREASON="appealReason";
	private static String FIELD_STATUS="status";
	private static String FIELD_CREATETIME="createTime";
	private static String FIELD_ALL_FROM_HOME_PAGE_AND_IM="isAllReportFromGongpingAndIm";
	private static enum AppealStatus {
		UNTREATED(0), TREATED_ILLEGAL(1),TREATED_LEAGAL(2);
        private int status;

        AppealStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
	
	/**
	 * 获取封禁详情（等级）
	 * @param uid
	 * @return
	 */
	public DBObject getClosureGrade(long uid){
		DBObject whereObj=new BasicDBObject(FIELD_TOUID,uid);
		return mongoTemplate.getDb().getCollection(COLLECTION_REPORTCOUNT).findOne(whereObj);
		
	}
	
	/**
	 * 提交申诉
	 * @param uid
	 * @param appealReason
	 * @param isAllReportFromGongpingAndIm
	 */
	public int  reportAppeal(long uid, String appealReason, boolean isAllReportFromGongpingAndIm){
		//如果存在未处理的history，则返回		
//		DBObject insertObject=new BasicDBObject(FIELD_UID,uid);
//		insertObject.put(FIELD_STATUS,AppealStatus.UNTREATED.getStatus());
		QueryBuilder query = QueryBuilder.start();
		query.and(new BasicDBObject(FIELD_UID,uid)).and(new BasicDBObject(FIELD_STATUS,AppealStatus.UNTREATED.getStatus()));
		int count=mongoTemplate.getDb().getCollection(COLLECTION_REPORTAPPEAL).find(query.get()).count();
		if(count!=0){
			return 0;
		}
		DBObject insertObject=new BasicDBObject(FIELD_UID,uid);
		insertObject.put(FIELD_STATUS,AppealStatus.UNTREATED.getStatus());
		insertObject.put(FIELD_APPRALREASON,appealReason);
		insertObject.put(FIELD_CREATETIME, new Date());
		insertObject.put(FIELD_ALL_FROM_HOME_PAGE_AND_IM, isAllReportFromGongpingAndIm);

		mongoTemplate.getDb().getCollection(COLLECTION_REPORTAPPEAL).save(insertObject);
		return 1;
	}

	/**
	 * 查询是否申诉,及违规结果
	 * @param uid
	 */
	public Map<String, Object> getAppealStatus(long uid) {
		Map<String, Object> map = new HashMap<>();
		QueryBuilder query = QueryBuilder.start();
		query.and(new BasicDBObject(FIELD_UID, uid)).or(new BasicDBObject(FIELD_STATUS, AppealStatus.UNTREATED.getStatus()),
				new BasicDBObject(FIELD_STATUS, AppealStatus.TREATED_ILLEGAL.getStatus()));
		DBObject dbObject = mongoTemplate.getDb().getCollection(COLLECTION_REPORTAPPEAL).findOne(query.get());//正常情况下应该只有未处理或违规一种情况
		map.put("status", dbObject != null);//true不能再申诉，false可以申诉
		DBObject dbObjectIllegal = mongoTemplate.getDb().getCollection(COLLECTION_REPORTAPPEAL).findOne(
				new BasicDBObject(FIELD_UID, uid).append(FIELD_STATUS, AppealStatus.TREATED_ILLEGAL.getStatus()));
		if (dbObjectIllegal != null) {
			map.put("extraMsg", FormatMessage.APPEAL_ILLEGAL);//返回违规信息
		}
		return map;
	}
	
}
