package com.yy.me.pay.controller;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.pay.service.PaymentWebService;

@RestController
@RequestMapping("/pay")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentWebService paymentService;

    /**
     * 生成营收专用token。
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/genToken")
    public void genIncomeToken(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        paymentService.genToken(uid, request, response);
    }

    /**
     * 红包功能使用，生成营收专用token。
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/genToken4RedEnvelope")
    public void genToken4RedEnvelope(@RequestParam long uid, @RequestParam(required = false) String lid,
            HttpServletRequest request, HttpServletResponse response) {
        paymentService.genToken4RedEnvelope(uid, lid, request, response);
    }

    /**
     * APP内获取我的收益.
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping("myIncome")
    public void getUserIncome(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("Miss required parameters. uid: {}", uid);
            
            sendResponse(request, response, genMsgObj(FAILED, "miss required parameter."));
            return;
        }

        paymentService.getMyIncome(uid, request, response);
    }

    /**
     * 分页获取礼物记录。
     * 
     * @param uid
     * @param limit
     * @param lastRecId
     * @param request
     * @param response
     */
    @RequestMapping("/getGiftHist")
    public void getGiftsHistory(@RequestParam long uid, @RequestParam int limit,
            @RequestParam(required = false) Long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponse(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        if (limit <= 0) {
            limit = 50;
        }

        if (lastRecId == null || lastRecId <= 0) {
            lastRecId = 0L;
        }

        paymentService.getBeansHistory(uid, limit, lastRecId, request, response);
    }

}
