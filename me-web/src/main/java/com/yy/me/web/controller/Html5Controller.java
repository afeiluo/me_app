package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.util.GeneralUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.metrics.MetricsClient;
import com.yy.me.web.service.UserInfoService;

@RestController
@RequestMapping("/web")
public class Html5Controller {
    private static Logger logger = LoggerFactory.getLogger(Html5Controller.class);

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private MetricsClient metricsClient;

    @RequestMapping(value = "/user/find")
    public void findUserInfo(@RequestParam long uid, @RequestParam long otherUid, HttpServletRequest request, HttpServletResponse response) {
        if (uid < 1) {
            logger.warn("Req Param Not Right: " + genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid <= 0"));
            return;
        }
        userInfoService.find4Web(uid, otherUid, request, response);
    }

}
