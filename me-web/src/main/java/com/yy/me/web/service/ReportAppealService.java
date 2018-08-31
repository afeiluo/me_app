package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.NOT_SECOND_GRADE;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.WAITTING_APPEAL;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponseAuto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.dao.ReportCountMongoDBMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.DBObject;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.mongo.MongoUtil;
import com.yy.me.time.MaskClock;
import com.yy.me.web.dao.ReportAppealMongoDBMapper;

/**
 * 申诉
 * @author dudusmida
 *
 */
@Service
public class ReportAppealService {

	private static final Logger logger = LoggerFactory.getLogger(ReportAppealService.class);
	
	@Autowired
	private ReportAppealMongoDBMapper reportAppealMongoDBMapper;

	@Autowired
	private ReportCountMongoDBMapper reportCountMongoDBMapper;
	
	@Autowired
    private MetricsClient metricsClient;
	
	private static String FIELD_REPORTCOUNT="reportCount";
	
	
	/**
	 * 提交申诉
	 * @param uid
	 * @param appealReason
	 * @param request
	 * @param response
	 */
	public void postAppeal(long uid,String appealReason,HttpServletRequest request, HttpServletResponse response){
		long start = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try{
			//判断是否为2级封禁
			DBObject reportObj=reportAppealMongoDBMapper.getClosureGrade(uid);
			if(reportObj==null||(reportObj!=null&&MongoUtil.getInt(reportObj.get(FIELD_REPORTCOUNT), 0)<ReportCountMongoDBMapper.SECOND_FORBID_COUNT)){
				logger.info("the user {} , closure-grede is not the second grade",uid);
				sendResponseAuto(request, response, genMsgObj(NOT_SECOND_GRADE, "closure-grede is not the second grade"));
	            return;
			}
			boolean isAllReportFromGongpingAndIm = reportCountMongoDBMapper.isAllReportFromGongpingAndIm(uid);
			int resultstatus=reportAppealMongoDBMapper.reportAppeal(uid,appealReason,isAllReportFromGongpingAndIm);

			if(resultstatus==0){
				sendResponseAuto(request, response, genMsgObj(WAITTING_APPEAL));
			}
			sendResponseAuto(request, response, genMsgObj(SUCCESS));
        }catch (Exception e){
        	logger.error("there are some exception about error", e);
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "ReportAppealService", "postAppeal", MaskClock.getCurtime() - start, rescode);
        }
		
	}
}
