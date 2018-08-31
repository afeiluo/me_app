package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yy.me.dao.ActivityMongoDBMapper;

@Service
public class ActivityService {
    private static Logger logger = LoggerFactory.getLogger(ActivityService.class);
    
    @Autowired
    private ActivityMongoDBMapper activityMongoDBMapper;

    public void getValidActivityList(HttpServletRequest request, HttpServletResponse response) {
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, activityMongoDBMapper.getValidActivity(null)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, "get valid activity error."));
        }
    }

    /**
     * 直播间营收活动入口支持对单个直播间配置
     * 
     */
    public void getAnchorActivity(Long uid, Long anchorUid, HttpServletRequest request, HttpServletResponse response) {
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, activityMongoDBMapper.getValidActivity(anchorUid)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, "get anchor activity error."));
        }
    }

}
