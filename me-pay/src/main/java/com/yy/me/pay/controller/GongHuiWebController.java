package com.yy.me.pay.controller;

import com.yy.me.pay.service.GongHuiWebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yy.me.http.BaseServletUtil.*;

/**
 * Created by Chris on 16/5/6.
 */
@RestController
@RequestMapping("/pay/web/gonghui")
public class GongHuiWebController {
    private static final Logger logger = LoggerFactory.getLogger(GongHuiWebController.class);

    @Autowired
    private GongHuiWebService gongHuiService;

    @RequestMapping("/getInfo")
    public void getGongHuiInfo(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        gongHuiService.getInfoJsonp(uid, request, response);
    }

    @RequestMapping("/exchange")
    public void exchange(@RequestParam long uid, @RequestParam long amount, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L || amount <= 0L) {
            logger.warn("uid or amount is invalid. uid: {}, amount: {}", uid, amount);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "parameter is invalid."));
            return;
        }

        gongHuiService.exchangeJsonp(uid, amount, request, response);
    }

    @RequestMapping("/sendGift")
    public void sendGift(@RequestParam long uid, @RequestParam long anchorUid, @RequestParam int propId, @RequestParam int propAmount,
                         HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L || anchorUid <= 0L || propId <= 0 || propAmount <= 0) {
            logger.warn("Miss required parameters. uid: {}, anchorUid: {}, propId: {}, propAmount: {}", uid, anchorUid, propId, propAmount);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "miss required parameters."));
            return;
        }

        gongHuiService.sendGiftJsonp(uid, anchorUid, propId, propAmount, request, response);
    }

}
