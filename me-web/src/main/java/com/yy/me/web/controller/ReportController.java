package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.web.service.ReportAppealService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.http.HttpUtil;
import com.yy.me.util.GeneralUtil;
import com.yy.me.web.service.MmsReportService;

/**
 * 
 * @author Jiang Chengyan
 * 
 */
@RestController
@RequestMapping("/userReport")
public class ReportController {

    private static Logger logger = LoggerFactory.getLogger(ReportController.class);
    
    @Autowired
    private MmsReportService mmsReportService;
    
    /**
     * 用户举报直播(含截屏)
     * http://test.me.yy.com/userReport/reportLs?appId=100001&sign=&data={"uid":1000009,"lid":"5772926f00008a4caf5e9d76","snapshotUrl":"http://aaa.bs2.yy.com/xxx.jpg","reportedType":"S01"}
     * 
     * @param uid 举报用户ID
     * @param deviceId 举报者设备ID
     * @param lid 直播频ID
     * @param reportedType 举报类型
     * @param snapshotUrl 举报截图
     * @param request
     * @param response
     * @param reason 3.1新增，举报原因
     * @param gongPing 3.1新增，公屏文字
     */
    @RequestMapping(value = "/reportLs")
    public void reportLs(@RequestParam long uid, @RequestParam String deviceId, @RequestParam String lid,
            @RequestParam(required = false) String huanJuYunSerial, @RequestParam String reportedType, @RequestParam String snapshotUrl,
            @RequestParam(required = false)  String reason,@RequestParam(required = false)  String gongPing,
            @RequestParam(required = false) Integer from,@RequestParam(required = false)  String gongPingImViolateText,
            HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L || StringUtils.isBlank(lid) || StringUtils.isBlank(reportedType) || StringUtils.isBlank(snapshotUrl)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, lid, reportedType, snapshotUrl));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0L || StringUtils.isBlank(lid) || StringUtils.isBlank(reportedType) || StringUtils.isBlank(snapshotUrl)"));
            return;
        }
        String ip = HttpUtil.getRemoteIP(request);
        sendResponse(request, response, genMsgObj(SUCCESS));
        mmsReportService.reportLs(ip, uid, deviceId, lid, huanJuYunSerial, reportedType, snapshotUrl,reason,gongPing,from,gongPingImViolateText);
    }
    
    /**
     * 用户举报直播连麦嘉宾(含截屏)
     * http://test.me.yy.com/userReport/reportLsLink?appId=100001&sign=&data={"uid":1000009,"lid":"5772926f00008a4caf5e9d76","linkUid":100001410,"snapshotUrl":"http://aaa.bs2.yy.com/xxx.jpg","reportedType":"S01"}
     * 
     * @param uid 举报用户ID
     * @param deviceId 举报者设备ID
     * @param lid 直播频ID
     * @param reportedType 举报类型
     * @param snapshotUrl 举报截图
     * @param request
     * @param response
     */
    @RequestMapping(value = "/reportLsLink")
    public void reportLsLink(@RequestParam long uid, @RequestParam String deviceId, @RequestParam String lid,
            long linkUid, @RequestParam(required = false) String huanJuYunSerial,
            @RequestParam String reportedType, @RequestParam String snapshotUrl,
            @RequestParam(required = false)  String reason,@RequestParam(required = false)  String gongPing,
            @RequestParam(required = false) Integer from,@RequestParam(required = false)  String gongPingImViolateText,
            HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L || StringUtils.isBlank(lid) || StringUtils.isBlank(reportedType) || StringUtils.isBlank(snapshotUrl) || linkUid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, lid, reportedType, snapshotUrl, linkUid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0L || StringUtils.isBlank(lid) || StringUtils.isBlank(reportedType) || StringUtils.isBlank(snapshotUrl) || linkUid <= 0"));
            return;
        }
        String ip = HttpUtil.getRemoteIP(request);
        sendResponse(request, response, genMsgObj(SUCCESS));
        mmsReportService.reportLsLink(ip, uid, deviceId, lid, linkUid, huanJuYunSerial, reportedType, snapshotUrl,reason,gongPing,from,gongPingImViolateText);
    }
    /**
     * 举报观众
     * http://test.me.yy.com/userReport/reportGuest?appId=100001&sign=&data={"uid":1000009,"lid":"5772926f00008a4caf5e9d76","snapshotUrl":"http://aaa.bs2.yy.com/xxx.jpg","reportedType":"S01"}
     */
    @RequestMapping(value = "/reportGuest")
    public void reportGuest(@RequestParam long uid,@RequestParam long guestUid, @RequestParam(required = false) String lid, @RequestParam(required = false) String snapshotUrl,
                            @RequestParam  String reason,@RequestParam(required = false)  String gongPing,@RequestParam Integer from ,
                            @RequestParam(required = false)  String gongPingImViolateText,
                            HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L  ) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, lid,  snapshotUrl));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0L "));
            return;
        }
        String ip = HttpUtil.getRemoteIP(request);
        sendResponse(request, response, genMsgObj(SUCCESS));
        mmsReportService.reportGuest(from,ip, uid, guestUid, lid,snapshotUrl,reason,gongPing,gongPingImViolateText);
    }

    /**
     * 查询是否已经申述
     */
    @RequestMapping(value = "/getAppealStatus")
    public void getAppealStatus(@RequestParam long uid,
                                HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0L "));
            return;
        }
        sendResponse(request, response, genMsgObj(SUCCESS, null,mmsReportService.getAppealStatus(uid)));
    }
}
