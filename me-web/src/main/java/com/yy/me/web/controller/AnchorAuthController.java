package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Maps;
import com.yy.me.util.GeneralUtil;
import com.yy.me.web.service.AnchorAuthService;

/**
 * Created by wangke on 2016/8/19.
 */
@Controller
@RequestMapping("/anchorAuth")
public class AnchorAuthController {
    private static Logger logger = LoggerFactory.getLogger(AnchorAuthController.class);

    @Autowired
    private AnchorAuthService anchorAuthService;

    @Value("#{settings['node.productEnv']}")
    private boolean productEnv;

    @RequestMapping(value = "/user")
    public void user(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {

        if (uid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.checkUser(uid, request, response);
    }

    @RequestMapping(value = "/checkPhone")
    public void checkPhone(@RequestParam long uid, HttpServletRequest request, HttpServletResponse response) {

        if (uid <= 0) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.checkUserPhone(uid, request, response);
    }

    @RequestMapping(value = "/sendCaptcha")
    public void sendCaptcha(@RequestParam long uid, @RequestParam String phone, HttpServletRequest request,
            HttpServletResponse response) {

        if (uid <= 0 || StringUtils.isEmpty(phone)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, phone));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.sendCaptcha(uid, phone, request, response);
    }

    @RequestMapping(value = "/verifyCaptcha")
    public void verifyCaptcha(@RequestParam long uid, @RequestParam String phone, @RequestParam String captcha,
            HttpServletRequest request, HttpServletResponse response) {

        if (uid <= 0 || StringUtils.isEmpty(phone) || StringUtils.isEmpty(captcha)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, phone, captcha));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.verifyCaptcha(uid, phone, captcha, request, response);
    }

    @RequestMapping(value = "/checkIdCard")
    public void checkIdCard(@RequestParam long uid, @RequestParam String IdCard, HttpServletRequest request,
            HttpServletResponse response) {

        if (uid <= 0 || StringUtils.isEmpty(IdCard)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, IdCard));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.checkIdCard(uid, IdCard, request, response);
    }

    @RequestMapping(value = "/submit")
    public void submit(@RequestParam long uid, @RequestParam String name, @RequestParam String IdCard,
            @RequestParam String img1, @RequestParam String img2, HttpServletRequest request,
            HttpServletResponse response) {

        if (uid <= 0 || StringUtils.isEmpty(IdCard) || StringUtils.isEmpty(name) || StringUtils.isEmpty(img1)
                || StringUtils.isEmpty(img2)) {
            logger.warn("Req Param Not Right: " + GeneralUtil.genMethodParamStr(uid, IdCard));
            sendResponse(request, response, genMsgObj(FAILED, "Req Param Not Right: uid invalid"));
            return;
        }

        anchorAuthService.submitAnchorAuth(uid, name, IdCard, img1, img2, request, response);
    }

    @RequestMapping(value = "/idCardCallback")
    public void idCardCallback(@RequestParam String username, @RequestParam int status, @RequestParam String callbackid,
            @RequestParam String timestamp, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (StringUtils.isEmpty(username)) {
            response.getWriter().print("param invalid");
            return;
        }

        anchorAuthService.idCardCallback(username, status, request, response);
    }

    @RequestMapping(value = "/realnameCallback")
    public void idCardCallback(@RequestParam long uid, @RequestParam int status,
            @RequestParam(required = false, defaultValue = "") String reason, @RequestParam String sign,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("realnameCallback param:uid={},status={},reason={},sign={}", uid, status, reason, sign);
        if (uid <= 0) {
            sendOuterResponse(request, response, makeResult(0, "param invalid"));
            return;
        }
        if (!realnameCallbackAuth(sign, "" + uid + status + reason)) {
            sendOuterResponse(request, response, makeResult(0, "auth fail"));
            return;
        }

        anchorAuthService.realnameCallback(uid, status, reason, request, response);
    }

    private static final String MD5_SUFFIX = "melive";

    private boolean realnameCallbackAuth(String sign, String params) {
        return !productEnv || DigestUtils.md5Hex(params + MD5_SUFFIX).equalsIgnoreCase(sign);
    }

    private Map<String, Object> makeResult(int code, String message) {
        Map<String, Object> ret = Maps.newHashMap();
        ret.put("code", code);
        ret.put("message", message);

        return ret;
    }

}
