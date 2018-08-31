package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.web.service.CountdownService;

/**
 * Created by ben on 16/7/20.
 */
@RestController
@RequestMapping("/countdown")
public class CountdownController {
    private static Logger logger = LoggerFactory.getLogger(CountdownController.class);

    @Autowired
    private CountdownService countdownService;

    /**
     * 主播测设置倒计时插件
     * 
     * @param uid
     * @param lid
     * @param duration
     * @param request
     * @param response
     */
    @RequestMapping("/set")
    public void setCountdown(Long uid, String lid, Long duration, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(lid) || duration <= 0) {
            logger.warn("Req Param Not Right. uid: {}, lid: {}, duration: {}", uid, lid, duration);
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: duration <= 0 || lid == null"));
            return;
        }
        countdownService.setCountdown(uid, lid, duration, request, response);
    }

    /**
     * 新进直播间的用户获取倒计时插件信息
     * 
     * @param anchorUid
     * @param lid
     * @param request
     * @param response
     */
    @RequestMapping("/get")
    public void getCountdown(Long anchorUid, String lid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(lid) || anchorUid <= 0) {
            logger.warn("Req Param Not Right. anchorUid: {}, lid: {}", anchorUid, lid);
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: duration <= 0 || lid == null"));
            return;
        }
        countdownService.getCountdown(anchorUid, lid, request, response);
    }

    /**
     * 主播测开始倒计时 
     * 
     * @param uid
     * @param lid
     * @param request
     * @param response
     */
    @RequestMapping("/start")
    public void startCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(lid)) {
            logger.warn("Req Param Not Right. uid: {}, lid: {}", uid, lid);
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right:lid==null"));
        }
        countdownService.startCountdown(uid, lid, request, response);
    }

    /**
     * 主播侧停止倒计时(关闭)
     * 
     * @param uid
     * @param lid
     * @param request
     * @param response
     */
    @RequestMapping("/stop")
    public void stopCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(lid)) {
            logger.warn("Req Param Not Right. uid: {}, lid: {}", uid, lid);
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right:lid==null"));
        }
        countdownService.stopCountdown(uid, lid, request, response);
    }

    /**
     * 主播测暂停倒计时
     * 
     * @param uid
     * @param lid
     * @param request
     * @param response
     */
    @RequestMapping("/pause")
    public void pauseCountdown(Long uid, String lid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isEmpty(lid)) {
            logger.warn("Req Param Not Right. uid: {}, lid: {}", uid, lid);
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right:lid==null"));
        }
        countdownService.pauseCountdown(uid, lid, request, response);
    }
}
