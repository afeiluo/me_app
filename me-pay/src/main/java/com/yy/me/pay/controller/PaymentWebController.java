package com.yy.me.pay.controller;

import static com.yy.me.http.BaseServletUtil.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yy.me.http.HttpUtil;
import com.yy.me.pay.service.PaymentWebService;

@RestController
@RequestMapping("/pay/web")
public class PaymentWebController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentWebController.class);

    private static final long TOKEN_EXPIRE = 10 * 60 * 1000; // 10分钟

    @Autowired
    private PaymentWebService paymentService;

    /**
     * 获取提现账户。
     * 
     * @param uid 用户ID
     * @param request
     * @param response
     */
    @RequestMapping("/getAccount")
    public void getAccount(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        Long tokenTime = (Long) request.getAttribute(REQ_ATTR_TOKEN_TIMESTAMP);
        String tokenVerified = (String) request.getAttribute(REQ_ATTR_TOKEN_VERIFIED);

        if (tokenTime == null || StringUtils.isEmpty(tokenVerified)) {
            logger.warn("Token is invalid. uid: {}, tokenTime: {}, tokenVerified: {}", uid, tokenTime, tokenVerified);
            
            sendResponseAuto(request, response, genMsgObj(VERIFY_FAILED, "Token is invalid."));
            return;
        }

        if (System.currentTimeMillis() - tokenTime > TOKEN_EXPIRE) {
            logger.warn("Token is expired. uid: {}, tokenTime: {}", uid, tokenTime);
            
            sendResponseAuto(request, response, genMsgObj(VERIFY_FAILED, "Token is expired."));
            return;
        }

        if (!StringUtils.equals(tokenVerified, REQ_ATTR_TOKEN_VERIFIED_VALUE)) {
            logger.warn("Token is not verified. uid: {}, tokenVerified: {}", uid, tokenVerified);
            
            sendResponseAuto(request, response, genMsgObj(VERIFY_FAILED, "Token is not verified."));
            return;
        }

        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        paymentService.getUserAccount(uid, request, response);
    }

    /**
     * 获取我的收益相关信息。
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping("/myIncome")
    public void getMyIncome(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        paymentService.getMyIncomeDetail(uid, request, response);
    }

    /**
     * 分页获取E豆收益记录。
     * 
     * @param uid
     * @param limit
     * @param lastRecId
     * @param request
     * @param response
     */
    @RequestMapping("/getBeansHist")
    public void getBeansHistory(@RequestParam long uid, @RequestParam int limit,
            @RequestParam(required = false) Long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        if (limit <= 0) {
            limit = 20;
        }

        if (lastRecId == null || lastRecId <= 0) {
            lastRecId = 0L;
        }

        paymentService.getBeansHistory(uid, limit, lastRecId, request, response);
    }

    /**
     * 分页获取收入记录。
     * 
     * @param uid
     * @param limit
     * @param lastRecId
     * @param request
     * @param response
     */
    @RequestMapping("/getIncomeHist")
    public void getIncomeHistory(@RequestParam long uid, @RequestParam int limit,
            @RequestParam(required = false) Long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        if (limit <= 0) {
            limit = 20;
        }

        if (lastRecId == null || lastRecId <= 0) {
            lastRecId = 0L;
        }

        paymentService.getIncomeHistory(uid, limit, lastRecId, request, response);
    }

    /**
     * 获取提现信息。
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping("/getWithdraw")
    public void getWithdrawInfo(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        paymentService.getWithdrawInfo(uid, request, response);
    }

    /**
     * 提现。
     * 
     * @param uid
     * @param amount 单位为分
     * @param request
     * @param response
     */
    @RequestMapping("/withdraw")
    public void withdraw(@RequestParam long uid, @RequestParam double amount, HttpServletRequest request,
            HttpServletResponse response) {
        if (uid <= 0L || amount <= 0L) {
            logger.warn("Miss required parameters. uid: {}, amount: {}", uid, amount);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "miss required parameter."));
            return;
        }

        paymentService.withdraw(uid, amount, request, response);
    }

    /**
     * 获取提现记录。
     * 
     * @param uid
     * @param limit
     * @param lastRecId
     * @param request
     * @param response
     */
    @RequestMapping("/getWithdrawHist")
    public void getWithdrawHistory(@RequestParam long uid, @RequestParam int limit,
            @RequestParam(required = false) Long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        if (limit <= 0) {
            limit = 20;
        }

        if (lastRecId == null || lastRecId <= 0) {
            lastRecId = 0L;
        }

        paymentService.getWithdrawHistory(uid, limit, lastRecId, request, response);
    }

    /**
     * 获取兑换M币记录。
     * 
     * @param uid
     * @param limit
     * @param lastRecId
     * @param request
     * @param response
     */
    @RequestMapping("/getExchangeHist")
    public void getExchangeHistory(@RequestParam long uid, @RequestParam int limit,
            @RequestParam(required = false) Long lastRecId, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        if (limit <= 0) {
            limit = 20;
        }

        if (lastRecId == null || lastRecId <= 0) {
            lastRecId = 0L;
        }

        paymentService.getExchangeHistory(uid, limit, lastRecId, request, response);
    }

    /**
     * 获取兑换M币信息。
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping("/getExchangeInfo")
    public void getExchangeInfo(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("uid is invalid. uid: {}", uid);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "uid is requiried."));
            return;
        }

        paymentService.getExchangeInfo(uid, request, response);
    }

    /**
     * 兑换米币。
     * 
     * @param uid
     * @param itemId
     * @param request
     * @param response
     */
    @RequestMapping("/exchange")
    public void exchangeMiBi(@RequestParam long uid, @RequestParam long itemId, HttpServletRequest request,
            HttpServletResponse response) {
        if (uid <= 0L || itemId <= 0L) {
            logger.warn("Miss required parameter. uid: {}, itemId: {}", uid, itemId);
            
            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss requiried parameter."));
            return;
        }

        String deviceType = request.getHeader(HEADER_X_CLIENT);
        String userIp = HttpUtil.getRemoteIP(request);

        paymentService.exchange(uid, itemId, userIp, deviceType, request, response);
    }

    /**
     * 获取用户基础信息（昵称，ME号，M币），M币兑换Y币使用
     * @param uid 用户UID
     */
    @RequestMapping("getMBiBalance")
    public void getMBiBalance(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L) {
            logger.warn("Miss required parameter. uid: {}", uid);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss required parameter."));
            return;
        }

        paymentService.getMBiBalance(uid, request, response);
    }

    /**
     * 获取用户YY信息（YY号，YY昵称），M币兑换Y币使用
     * @param yy 用户YY号
     */
    @RequestMapping("getYYInfo")
    public void getYYInfo(@RequestParam long yy, HttpServletRequest request, HttpServletResponse response) {
        if (yy <= 0L) {
            logger.warn("Miss required parameter. yy: {}", yy);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss required parameter."));
            return;
        }

        paymentService.getYYInfo(yy, request, response);
    }

    /**
     * M币兑换Y币
     * @param uid 用户UID
     * @param yy 用户YY号
     * @param amount 兑换M币数量
     */
    @RequestMapping("m2y")
    public void m2y(@RequestParam long uid, @RequestParam String yy, @RequestParam Long amount, HttpServletRequest request, HttpServletResponse response) {
        if (uid <= 0L || StringUtils.isEmpty(yy) || amount == null || amount <= 0) {
            logger.warn("Miss required parameter or parameter error. uid: {}, yy:{}, amount:{}", uid, yy, amount);

            sendResponseAuto(request, response, genMsgObj(FAILED, "Miss required parameter or parameter error."));
            return;
        }

        paymentService.m2y(uid, yy, amount, request, response);
    }
}
