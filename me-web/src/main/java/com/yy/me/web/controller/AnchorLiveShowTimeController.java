package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

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

import com.yy.me.time.DateTimeUtil;
import com.yy.me.web.service.AnchorLiveTimeService;

@RestController
@RequestMapping("/web")
public class AnchorLiveShowTimeController {
    
    
    @Autowired
    private AnchorLiveTimeService anchorLiveTimeService;
    
    private static Logger logger = LoggerFactory.getLogger(AnchorLiveShowTimeController.class);
    
    private static final String DATE_PARSE_ERROR_MSG = "日期格式错误！";
    
    /**
     * 新添加接口，H5界面主播，我的开播记录接口
     * @param uid
     * @param date
     * @param request
     * @param response
     */
    @RequestMapping("/anchor/liveShowDurationList")
    public void anchorLiveShowDurationList(@RequestParam(required = true) Long uid, @RequestParam(required = false) String date, HttpServletRequest request,
            HttpServletResponse response) {
        if (uid == null || uid < 0) {
            logger.warn("Invalid parameter. uid: {}, date: {}", uid, date);
            sendResponseAuto(request, response, genMsgObj(FAILED, "Invalid parameter: uid < 0"));
            return;
        }
        
        Date  myDate = null;
        //如果日期为空
        if(StringUtils.isNotBlank(date)) {
            try{
                myDate = DateTimeUtil.parseDate(date, DateTimeUtil.COMPACT_MONTH_FORMAT);
            } catch(Exception e) {
                logger.error("Parse date error.", e);
                sendResponseAuto(request, response, genMsgObj(FAILED, DATE_PARSE_ERROR_MSG));
                return;
            }
        }
        anchorLiveTimeService.doSetLiveShowH5DurationDto(uid, myDate, request, response);
    }

}
