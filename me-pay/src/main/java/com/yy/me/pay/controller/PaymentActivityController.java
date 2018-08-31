package com.yy.me.pay.controller;

import com.yy.me.pay.service.PaymentActivityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yy.me.http.BaseServletUtil.*;

@RestController
@RequestMapping("/pay/web/act")
public class PaymentActivityController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentActivityController.class);

    @Autowired
    private PaymentActivityService activityService;

    @RequestMapping("/getInfos")
    public void getActInfos(@RequestParam long uid, HttpServletRequest request,
            HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("Missed required parameters. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss required parameter."));
            return;
        }

        activityService.getActInfos(uid, request, response);
    }

    @RequestMapping("/checkEligibility")
    public void checkEligibility(@RequestParam long uid, @RequestParam int propId, HttpServletRequest request,
            HttpServletResponse response) {
        if (uid <= 0L || propId <= 0) {
            logger.warn("Missed required parameters. uid: {}, actId: {}", uid, propId);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss required parameter."));
            return;
        }

        activityService.checkEligibility(uid, propId, request, response);
    }

}
