package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.FAILED;
import static com.yy.me.http.BaseServletUtil.PARAM_ERROR;
import static com.yy.me.http.BaseServletUtil.SUCCESS;
import static com.yy.me.http.BaseServletUtil.genMsgObj;
import static com.yy.me.http.BaseServletUtil.sendResponse;
import static com.yy.me.http.BaseServletUtil.sendResponseAuto;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.util.GeneralUtil;
import com.yy.me.web.service.ArrangeAnchorScheService;

@RestController
@RequestMapping("/web/arrangeSche")
public class AnchorArrangeScheController {

	private static Logger logger = LoggerFactory.getLogger(AnchorArrangeScheController.class);
	
	@Autowired
    private ArrangeAnchorScheService arrangeAnchorScheService;
	 /**
     * 获取主播本周排班考勤
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value="/thisweek/attendance")
    public void thisweekAttendanceList(@RequestParam Long uid,int isNext, HttpServletRequest request, HttpServletResponse response){
    	if(uid==null||!(isNext==0||isNext==1)){
    		logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
    		sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right"));
            return;
    	}
    	arrangeAnchorScheService.thisweekAttendanceList(uid,isNext,request,response);
    }
    
    
    
    
    
    
    /**
     * 下周抢班列表|本周未来几天抢班改班列表
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value="/week/grablist")
    public void weekGrablist(@RequestParam Long uid,int isNext,String city,String datTimeStr,HttpServletRequest request, HttpServletResponse response){
    	if(uid==null){
    		logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
    		sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: uid is null"));
            return;
    	}
    	arrangeAnchorScheService.weekGrablist(uid,isNext, city, datTimeStr,request,response);
    	
    }
    
    
    
    
    /**
     * 抢班
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value="/week/grab")
    public void weekGrab(@RequestParam Long uid,
    		String datTimeStr,String city,
    		HttpServletRequest request, HttpServletResponse response){
    	if(uid==null||StringUtils.isBlank(datTimeStr)){
    		logger.warn("Req Param Not Right: uid->{},datTimeStr->{} " + GeneralUtil.genMethodParamStr(uid),datTimeStr);
    		sendResponseAuto(request, response, genMsgObj(PARAM_ERROR, "Req Param Not Right: uid is null"));
            return;
    	}
    	 NYYHttpRequestWrapper nYYHttpRequestWrapper=(NYYHttpRequestWrapper)request;
    	 ArrayNode dateNode=(ArrayNode)nYYHttpRequestWrapper.getDataNode().get("bucids");
    	 if(dateNode==null||dateNode.isNull()||dateNode.size()!=2){
    		 logger.warn("Req Param Not Right --> bids");
             sendResponseAuto(request, response, genMsgObj(PARAM_ERROR, "Req Param Not Right: bids is null or  bids length is error"));
             return; 
    	 }

    	 
    	arrangeAnchorScheService.weekGrab(uid,dateNode,datTimeStr,city,request,response);
    	
    }
    
    
    
    /**
     * 改班
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value="/week/editgrab")
    public void weekEditGrab(@RequestParam Long uid, String city,
    		String datTimeStr,
    		HttpServletRequest request, HttpServletResponse response){
    	if(uid==null){
    		logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: uid is null"));
            return;
    	}
    	 if(StringUtils.isBlank(datTimeStr)) {
    		 logger.warn("Req Param Not Right datTimeStr={} ",datTimeStr);
             sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: datTimeStr is null"));
             return;
    	 }
    	 NYYHttpRequestWrapper nYYHttpRequestWrapper=(NYYHttpRequestWrapper)request;
    	 ArrayNode bucids=(ArrayNode)nYYHttpRequestWrapper.getDataNode().get("bucids");
    	 if(bucids==null||bucids.isNull()||bucids.size()!=2){
    		 logger.warn("Req Param Not Right --> bids");
             sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: bids is null or  bids length is error"));
             return;
    	 }
    	 ArrayNode bucOldIds=(ArrayNode)nYYHttpRequestWrapper.getDataNode().get("bucOldIds");
    	 if((bucOldIds!=null&&!bucOldIds.isNull())&&(bucOldIds.size()>0&&bucOldIds.size()!=2)){
    		 logger.warn("Req Param Not Right --> bids");
             sendResponseAuto(request, response, genMsgObj(FAILED, "Req Param Not Right: old bids is null or  bids length is error"));
             return;
    	 }
    	
    	 arrangeAnchorScheService.weekEditGrab(uid,bucids,bucOldIds,city,datTimeStr,request,response);
    	
    }
    
    
    /**
     * 周列表
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value="/week/list")
    public void weekList(@RequestParam Long uid,int isNext, HttpServletRequest request, HttpServletResponse response){
    	
    	arrangeAnchorScheService.weekList(isNext,request,response);
    	
    }
    
   
}
