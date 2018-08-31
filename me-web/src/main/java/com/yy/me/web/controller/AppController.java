package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.yy.me.http.HttpUtil;
import com.yy.me.liveshow.thrift.ClientType;
import com.yy.me.web.service.AppService;
import com.yy.me.yycloud.ApTokenUtils;
import com.yy.me.yycloud.AppConstants;

/**
 * 1. loginReport在登录成功后或deviceToken有变化时调用（可重复调用，如刚开始只有许铎长连接的ID或小米的ID，可以调用一次，当后面iOS拿到deviceToken的时候又可以调用一次，但每次要带上尽可能多的参数，
 * 即使之前已经传过长连接ID）
 * 2. logoutReport在用户手工退出app登录时调用
 * 3. validate在用户重新进入app时（app成为前台应用）、或后台线程隐式重登录前 调用，也是能带上的参数就带上
 * 
 * 进入app时，如果不存在登陆信息，或者token已过期，就不用validate
 * 
 * @author Jiang Chengyan
 * 
 */
@RestController
@RequestMapping("/app")
public class AppController {

    private static Logger logger = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private AppService appService;

    /**
     * 
     * @param uid 用户uid
     * @param filename 文件名
     * @param bucket
     * @param request
     * @param response
     */
    @RequestMapping(value = "/genBs2Token")
    public void genToken(@RequestParam long uid, @RequestParam String filename, @RequestParam String bucket, HttpServletRequest request,
            HttpServletResponse response) {
        logger.info("genBs2Token filename:{}, bucket:{}, uid:{}", filename, bucket, uid);
        if (StringUtils.isBlank(bucket) || StringUtils.isBlank(filename)) {
            sendResponse(request, response, genMsgObj(FAILED));
            return;
        }
        if (!filename.startsWith("c" + "_" + uid + "_")) {
            sendResponse(request, response, genMsgObj(FAILED));
            return;
        }

        try {
            Map<String, String> jo = Maps.newHashMap();
            jo.put("bucket", bucket);
            jo.put("filename", filename);
            String context = getLocalObjMapper().writeValueAsString(jo);

            Map<String, Object> ret = Maps.newHashMap();
            int ttl = 86400;
            String token = null;
            if (bucket.equals(AppConstants.MRP_BUCKETNAME)) {
                token = ApTokenUtils.genMobileReportPicToken(uid, context, ttl);
            } else if (bucket.equals(AppConstants.ME_AUTH_BUCKETNAME)) {
                token = ApTokenUtils.genMeAnchorAuthPicToken(uid, context, TimeUnit.MINUTES.toSeconds(10));
            } else {
                token = ApTokenUtils.genSidTokenLocal(uid, "", context, -1, ttl, true);
            }
            ret.put(RET_LS_TOKEN, token);
            ret.put(RET_LS_TOKEN_VALID_TIME, ttl);// 一小时有效
            sendResponse(request, response, genMsgObj(SUCCESS, null, ret));
        } catch (Exception e) {
            logger.error("Gen BS2 token error.", e);

            sendResponse(request, response, genMsgObj(FAILED));
        }

    }

    @RequestMapping(value = "/checkDevice")
    public void checkDevice(@RequestParam long uid, @RequestParam String deviceId, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isBlank(deviceId)) {
            sendResponse(request, response, genMsgObj(FAILED));
            return;
        }
        String clientType = request.getHeader(HEADER_X_CLIENT);
        if (!"iOS".equalsIgnoreCase(clientType) && !"Android".equalsIgnoreCase(clientType)) {
            sendResponse(request, response, genMsgObj(FAILED));
            return;
        }
        int osType = 0;
        if ("iOS".equalsIgnoreCase(clientType)) {
            osType = ClientType.IOS.getValue();
        } else {
            osType = ClientType.ANDROID.getValue();
        }
        appService.checkDevice(uid, osType, deviceId, request, response);
    }

    /**
     * 登录成功后上报APP相关信息
     * http://61.147.186.63:8081/app/loginReport?appId=100001&sign=&data={"uid":1000008,"osType":0,"notifyType":1,
     * "notifyId":"aasdfhjkasdf11112","connType":2,"connId":"jjncnnxm109813"}
     * 
     * @param uid 用户ID
     * @param osType 操作系统（0代表Android，1代表iOS）
     * @param notifyType 通知栏通知类型（0代表米Push，1代表苹果APNS）
     * @param notifyId 通知栏通知ID
     * @param connType 长连接类型（0代表米Push，1代表苹果APNS，2代表mikasa）
     * @param connId 长连接ID
     * @param deviceId 设备ID
     * @param request
     * @param response
     */
    @RequestMapping(value = "/loginReport")
    public void loginReport(@RequestParam long uid, @RequestParam(required = false, defaultValue = "0") int osType,
            @RequestParam(required = false, defaultValue = "0") int notifyType, @RequestParam(required = false, defaultValue = "") String notifyId,
            @RequestParam(required = false, defaultValue = "0") int connType, @RequestParam(required = false, defaultValue = "") String connId,
            @RequestParam(required = false, defaultValue = "") String deviceId, HttpServletRequest request, HttpServletResponse response) {

        appService.registerPush(uid, osType, notifyType, notifyId, connType, connId, deviceId, request, response);
    }

    /**
     * 登出时上报APP相关信息
     * http://61.147.186.63:8081/app/logoutReport?appId=100001&sign=&data={"uid":1000008,"osType":0,"notifyType":1,
     * "notifyId":"aasdfhjkasdf11112","connType":2,"connId":"jjncnnxm109813"}
     * 
     * @param uid 用户ID
     * @param osType 操作系统（0代表Android，1代表iOS）
     * @param notifyType 通知栏通知类型（0代表米Push，1代表苹果APNS）
     * @param notifyId 通知栏通知ID
     * @param connType 长连接类型（0代表米Push，1代表苹果APNS，2代表mikasa）
     * @param connId 长连接ID
     * @param request
     * @param response
     */
    @RequestMapping(value = "/logoutReport")
    public void logoutReport(@RequestParam long uid, @RequestParam(required = false, defaultValue = "0") int osType,
            @RequestParam(required = false, defaultValue = "0") int notifyType, @RequestParam(required = false, defaultValue = "") String notifyId,
            @RequestParam(required = false, defaultValue = "0") int connType, @RequestParam(required = false, defaultValue = "") String connId,
            HttpServletRequest request, HttpServletResponse response) {

        appService.unRegister(uid, osType, notifyType, notifyId, connType, connId, request, response);
    }

    /**
     * 
     * @param uid
     * @param status 0 离线 1 在线
     * @param request
     * @param response
     */
    @RequestMapping(value = "/statusReport")
    public void statusReport(@RequestParam long uid, @RequestParam int status, HttpServletRequest request, HttpServletResponse response) {
        appService.reportOnlineAndLastActiveTime(uid, status, request, response);
    }

    /**
     * 获取闪屏页信息及弹窗广告。
     * 
     * @param request
     * @param response
     */
    @RequestMapping(value = "/splash")
    public void splash(@RequestParam(required = false, defaultValue = "") String channelId, HttpServletRequest request, HttpServletResponse response) {
        appService.getSplashAndPopupAd(channelId, request, response);
    }

    /**
     * 获取所有行业信息
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getAllBusiness")
    public void getAllBusiness(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        appService.getAllBusiness(uid, request, response);
    }

    /**
     * 获取所有兴趣标签
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getAllInterest")
    public void getAllInterest(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        appService.getAllInterest(uid, request, response);
    }

    /**
     * 获取所有兴趣标签
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping(value = "/getAllInterestWithDefault")
    public void getAllInterestWithDefault(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        appService.getAllInterestWithDefault(uid, request, response);
    }

    @RequestMapping(value = "/betaUpgrade/check")
    public void betaUpgradeCheck(HttpServletRequest request, HttpServletResponse response) {
        appService.getBetaUpgradeConf(request, response);
    }

    @RequestMapping(value = "/findLocationCity")
    public void findLocationCity(@RequestParam(required = false, defaultValue = "0") Long uid, @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude, HttpServletRequest request, HttpServletResponse response) {
        appService.findLocationCity(uid, longitude, latitude, HttpUtil.getRemoteIP(request), request, response);
    }
}
