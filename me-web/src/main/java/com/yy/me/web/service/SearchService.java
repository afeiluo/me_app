package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.service.inner.FillService;
import com.yy.me.time.MaskClock;
import com.yy.me.util.search.HiddoSearch;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    private String progress = "SearchService";
    
    @Autowired
    private MetricsClient metricsClient;
    
    @Autowired
    private HiddoSearch hiddoSearch;
    
    @Autowired
    private FillService fillService;
    
    public void searchUser(long uid, String key, int page, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码

        try {
            List<Long> searchUids = hiddoSearch.searchUserInfo(uid, key, null, page);
            
            List<ObjectNode> retList = fillService.fillUserInfo(uid, searchUids);
            
            if (retList == null || retList.isEmpty()) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            
            sendResponse(request, response, genMsgObj(SUCCESS, null, retList));
        } catch (Exception e) {
            rescode = MetricsClient.RESCODE_FAIL;
            logger.error("Search user error.", e);
            
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "searchUser", MaskClock.getCurtime() - t, rescode);
        }
    }

}
