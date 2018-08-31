package com.yy.me.pay.controller;

import static com.yy.me.http.BaseServletUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yy.me.config.CntConfService;
import com.yy.me.entity.PaymentAccount;
import com.yy.me.enums.PayAccountType;
import com.yy.me.http.HttpUtil;
import com.yy.me.json.JsonUtil;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.nyy.NYYHttpRequestWrapper;
import com.yy.me.pay.entity.GiftCallbackReq;
import com.yy.me.pay.service.PaymentActivityService;
import com.yy.me.pay.service.PaymentWebService;

/**
 * 营收系统回调接口。
 * 
 * 不验证token，验证服务器白名单。
 * 
 * @author cuixiang
 *
 */
@RestController
@RequestMapping("/pay/revenue")
public class PaymentCallbackController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentCallbackController.class);

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    private PaymentWebService paymentService;
    
    @Autowired
    private PaymentActivityService paymentActivityService;

    /**
     * 送礼物成功后，营收系统回调该接口。
     * <p>
     * 该接口需加入白名单验证.
     * <p>
     * 向客户端发送广播提示，客户端拉取礼物列表。
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/giftCallback")
    public void giftCallback(HttpServletRequest request, HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received gift callback request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("Gift callback request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        GiftCallbackReq giftInfo = null;

        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();
                
                giftInfo = getLocalObjMapper().convertValue(dataNode, GiftCallbackReq.class);
            } catch (Exception e) {
                logger.warn("Convert request error.", e);
                
                sendResponse(request, response, genMsgObj(FAILED, "Convert request error."));
                return;
            }
        }

        if (giftInfo == null) {
            logger.warn("giftInfo is required. giftInfo: {}", giftInfo);
            sendResponse(request, response, genMsgObj(FAILED, "giftinfo is required."));
            return;
        }

        if (StringUtils.isEmpty(giftInfo.getSeq()) || giftInfo.getUid() <= 0L || giftInfo.getRecvUid() <= 0L
                || giftInfo.getUsedTimestamp() <= 0L || giftInfo.getUseInfos() == null
                || giftInfo.getUseInfos().isEmpty() || StringUtils.isEmpty(giftInfo.getExpand())) {
            logger.warn("Missed required gift info. giftInfo: {}", giftInfo);
            sendResponse(request, response, genMsgObj(FAILED, "giftInfo is invalid."));
            return;
        }

        logger.info("Got gift bag: {}", giftInfo);

        paymentService.receiveGiftBag(giftInfo, request, response);
    }

    /**
     * 超级喜欢后，营收系统回调该接口。
     * <p>
     * 该接口需加入白名单验证.
     * <p>
     *
     * @param request
     * @param response
     */
    @RequestMapping("/superLikeCallback")
    public void superLikeCallback(HttpServletRequest request, HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received superLike callback request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("superLike callback request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        GiftCallbackReq giftInfo = null;

        if (request instanceof NYYHttpRequestWrapper) {
            try {
                NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
                JsonNode dataNode = req.getDataNode();

                giftInfo = getLocalObjMapper().convertValue(dataNode, GiftCallbackReq.class);
            } catch (Exception e) {
                logger.warn("Convert request error.", e);

                sendResponse(request, response, genMsgObj(FAILED, "Convert request error."));
                return;
            }
        }

        if (giftInfo == null) {
            logger.warn("superLikeInfo is required. info: {}", giftInfo);
            sendResponse(request, response, genMsgObj(FAILED, "info is required."));
            return;
        }

        if (StringUtils.isEmpty(giftInfo.getSeq()) || giftInfo.getUid() <= 0L || giftInfo.getRecvUid() <= 0L
                || giftInfo.getUsedTimestamp() <= 0L || giftInfo.getUseInfos() == null
                || giftInfo.getUseInfos().isEmpty() || StringUtils.isEmpty(giftInfo.getExpand())) {
            logger.warn("Missed required superLike info. info: {}", giftInfo);
            sendResponse(request, response, genMsgObj(FAILED, "info is invalid."));
            return;
        }

        logger.info("Got superlike: {}", giftInfo);

        paymentService.receiveSuperLike(giftInfo, request, response);
    }

    /**
     * 营收系统回调发送单播，消息透传给客户端。
     * 
     * 该接口需加入白名单。
     * 
     * @param uid
     * @param msg
     * @param request
     * @param response
     */
    @RequestMapping("/unicast")
    public void unicast(@RequestParam long uid, @RequestParam String msg, HttpServletRequest request,
            HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received send unicast request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("Unicast request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        if (uid <= 0 || StringUtils.isBlank(msg)) {
            logger.warn("miss required parameter. uid: {}, msg: {}", uid, msg);
            sendResponse(request, response, genMsgObj(FAILED, "miss required parameter."));
            return;
        }

        logger.info("Got unicast request from turnover system - uid: {}, msg: {}", uid, msg);

        paymentService.unicastToUser(uid, msg, request, response);
    }
    
    /**
     * 充值活动营收系统回调。
     * 
     * @param uid
     * @param request
     * @param response
     */
    @RequestMapping("/actCallback")
    public void actCallback(@RequestParam String seq, @RequestParam long uid, @RequestParam int propId,
            @RequestParam long buyTime, HttpServletRequest request, HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received payment activity callback request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("Payment activity callback request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        if (StringUtils.isBlank(seq) || uid <= 0L || propId <= 0 || buyTime <= 0L) {
            logger.warn("miss required parameter. seq: {}, uid: {}, propId: {}, buyTime: {}", seq, uid, propId, buyTime);
            sendResponse(request, response, genMsgObj(FAILED, "miss required parameter."));
            return;
        }

        logger.info("Got payment activity callback request from turnover system - uid: {}, propId: {}, buyTime: {}, seq: {}",
                uid, propId, buyTime, seq);

        paymentActivityService.updateActInfo(seq, uid, propId, buyTime, request, response);
    }
    
    /**
     * 保存通过实名认证的提现账户。
     *
     * @param uid      用户ID
     * @param name     用户姓名
     * @param idCard   身份证号码
     * @param account  支付宝账号
     * @param request
     * @param response
     */
    @RequestMapping("/accountVerified")
    public void verifiedAccountCallback(@RequestParam long uid, @RequestParam String name, @RequestParam String idCard,
            @RequestParam String account, @RequestParam String phone, HttpServletRequest request,
            HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received payment account verified callback request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("Payment account verified callback request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        if (uid <= 0L || StringUtils.isEmpty(name) || StringUtils.isEmpty(idCard) || StringUtils.isEmpty(account)
                || StringUtils.isBlank(phone)) {
            logger.warn("Missed required parameters. uid: {}, name: {}, idCard: {}, account: {}, phone: {}", uid, name,
                    idCard, account, phone);
            sendResponse(request, response, genMsgObj(FAILED, "Miss required parameter."));
            return;
        }

        PaymentAccount payAccount = new PaymentAccount();
        payAccount.setUid(uid);
        payAccount.setName(name);
        payAccount.setIdCard(idCard);
        payAccount.setType(PayAccountType.ALIPAY.getValue());
        payAccount.setAccount(account);
        payAccount.setPhone(phone);

        paymentService.saveAccount(payAccount, request, response);
    }

    /**
     * 营收广播透传接口
     * 
     * @param request
     * @param response
     */
    @SuppressWarnings("unchecked")
    @RequestMapping("/broadcast")
    public void broadcast(HttpServletRequest request, HttpServletResponse response) {
        String remoteIp = HttpUtil.getRemoteIP(request);
        logger.info("Received gift callback request from " + remoteIp);

        if (!checkIpAllowed(remoteIp)) {
            logger.info("turnover broadcast request from not trusted ip: " + remoteIp);
            sendResponse(request, response, genMsgObj(NOT_ALLOW, "access ip is not allowed"));
            return;
        }

        ObjectNode msg;
        List<String> uidKeyList;
        List<Long> uidList;
        try {
            NYYHttpRequestWrapper req = (NYYHttpRequestWrapper) request;
            JsonNode dataNode = req.getDataNode();
            JsonNode msgNode = dataNode.get("message");
            msg = getLocalObjMapper().readValue(msgNode.asText(), ObjectNode.class);

            if (msg == null) {
                logger.warn("msg is null.");
                sendResponse(request, response, genMsgObj(FAILED));
                return;
            }

            uidKeyList = getValueFromData(dataNode, "fillUserInfo",
                    JsonUtil.instance.contructCollectionType(List.class, String.class), Collections.EMPTY_LIST);
            uidList = getValueFromData(dataNode, "uids",
                    JsonUtil.instance.contructCollectionType(List.class, Long.class), Collections.EMPTY_LIST);
        } catch (Exception e) {
            logger.warn("Convert request error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "Convert request error."));
            return;
        }

        logger.info("turnover push msg: {},uids:{}", msg, uidList);

        paymentService.turnoverBroadcast(msg, uidKeyList, uidList, request, response);
    }

    private <T> T getValueFromData(JsonNode dataNode, String key, JavaType typeOfT, T defaultValue) {
        JsonNode msg = dataNode.get(key);

        if (msg == null || msg.isNull()) {
            return defaultValue;
        }

        return getLocalObjMapper().convertValue(msg, typeOfT);
    }

    private boolean checkIpAllowed(String ip) {
        // 允许公司内网访问，方便测试
        if (DistIpUtil.isCompanyIp(ip, true, false)) {
            return true;
        }

        Set<String> allowIps = cntConfService.fetchPaymentIps();
        if (allowIps == null || allowIps.isEmpty()) {
            return false;
        }

        if (allowIps.contains(ip)) {
            return true;
        }

        return false;
    }

}
