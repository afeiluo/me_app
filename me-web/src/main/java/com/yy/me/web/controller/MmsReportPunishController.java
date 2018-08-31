package com.yy.me.web.controller;

import static com.yy.me.http.BaseServletUtil.*;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.yy.me.web.service.ReportService;
import com.yy.me.yycloud.AppConstants;

@RestController
@RequestMapping("/mms")
public class MmsReportPunishController {
    private static final Logger logger = LoggerFactory.getLogger(MmsReportPunishController.class);

    private static final String APP_KEY = "101017";
    private static final String APP_LIVE_KEY = String.valueOf(AppConstants.YY_CLOUD_APPID);
    private static final String PUB_KEY = "MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAP1R1jLPc1kikRwexRvKZhmR01hxFTCYrRaDX8/g+gmQAWWHf0fOrAi0R7dr6BRlT3unfNMgAi8U2+Iet7vpSz1EgG4ZXRc4XSK704jhMV0FPF98OFKFDBWlxJsNnt/MwKiwIA9KHbC89OzJGSap02Mqfa0f8LzMUkP848EZDJkD";
    private static final String TXT_APP_KEY = "999911050";
    private static final String APP_KEY_GONGPING_1 = "999911044";
    private static final String APP_KEY_GONGPING_2 = "999911047";
    public static final String video_appKey = "301030";
    public static final String album_appKey = "301029";
    @Autowired
    private ReportService reportService;

    @RequestMapping("/punish")
    public void punish(@RequestParam String appKey, @RequestParam String serial, @RequestParam String cmd,
            @RequestParam(required = false) String reason, @RequestParam(required = false) String msg,
            @RequestParam(required = false) String extParUrlEncoder, @RequestParam String sign, @RequestParam String status,
            HttpServletRequest request, HttpServletResponse response) {
        logger.info(" >> MMS punish - appKey: {}, serial: {}, cmd: {}, reason: {}, msg: {}, extParUrlEncoder: {}, sign: {}, status: {}", appKey,
                serial, cmd, reason, msg, extParUrlEncoder, sign, status);

        if (!StringUtils.equals(appKey, APP_KEY) && !StringUtils.equals(appKey, TXT_APP_KEY) && !StringUtils.equals(appKey, APP_KEY_GONGPING_1)
                && !StringUtils.equals(appKey, APP_KEY_GONGPING_2) && !StringUtils.equals(appKey, video_appKey)
                && !StringUtils.equals(appKey, album_appKey)) {
            logger.error("[MMS] appKey not match for punish action.");
            sendOuterResponse(request, response, makeResult(0, "-1:appKey mismatch"));
            return;
        }

        if (StringUtils.isBlank(serial)) {
            logger.error("[MMS] serial is required for punish action.");
            sendOuterResponse(request, response, makeResult(0, "-11:serial missed"));
            return;
        }

        // if (!StringUtils.equals(cmd, "C0")) {
        // logger.error("[MMS] Not supported command: {}", cmd);
        // sendResponse(request, response, makeResult(0, "-21:Directive not exist"));
        // return;
        // }

        if (!auth(String.format("%s%s%s%s%s%s", appKey, serial, cmd, reason, msg, extParUrlEncoder), sign)) {
            logger.error("[MMS] Validate sign error: data[" + String.format("%s%s%s%s%s%s", appKey, serial, cmd, reason, msg, extParUrlEncoder)
                    + "], sign[" + sign + "].");
            sendOuterResponse(request, response, makeResult(0, "-1:auth fail"));
            return;
        }

        try {
            int result = Integer.parseInt(status);

            logger.info("MMS Punish result - serial:{} status: {}, reason: {}, msg: {}, exPar: {}", serial, result, reason, msg, extParUrlEncoder);

            if (result == 1 || result == 2) {
                reportService.verifyImgCallback(appKey, serial, result, cmd, reason, extParUrlEncoder);
            }

            sendOuterResponse(request, response, makeResult(1, "punish finish"));
        } catch (Exception e) {
            logger.error("MMS Punish error.", e);
            sendOuterResponse(request, response, makeResult(0, "-29:punish error"));
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping("/punishLive")
    public void punishLive(@RequestParam String appKey, @RequestParam String serial, @RequestParam String cmd,
            @RequestParam(required = false) String reason, @RequestParam(required = false) String msg,
            @RequestParam(required = false) String extParUrlEncoder, @RequestParam String sign, @RequestParam(required = false) String status,
            HttpServletRequest request, HttpServletResponse response) {
        logger.info(" >> MMS punishLive - appKey: {}, serial: {}, cmd: {}, reason: {}, msg: {}, extParUrlEncoder: {}, sign: {}, status: {}", appKey,
                serial, cmd, reason, msg, extParUrlEncoder, sign, status);

        if (!StringUtils.equals(appKey, APP_LIVE_KEY)) {
            logger.error("[MMS] appKey not match for punish action.");
            sendOuterResponse(request, response, makeResult(0, "-1:appKey mismatch"));
            return;
        }

        if (StringUtils.isBlank(serial)) {
            logger.error("[MMS] serial is required for punish action.");
            sendOuterResponse(request, response, makeResult(0, "-11:serial missed"));
            return;
        }

        if (!auth(String.format("%s%s%s%s%s%s", appKey, serial, cmd, reason, msg, extParUrlEncoder), sign)) {
            logger.error("[MMS] Validate sign error: data[" + String.format("%s%s%s%s%s%s", appKey, serial, cmd, reason, msg, extParUrlEncoder)
                    + "], sign[" + sign + "].");
            sendOuterResponse(request, response, makeResult(0, "-1:auth fail"));
            return;
        }

        try {
            logger.info("MMS Punish live result - cmd: {}, status: {}, reason: {}, msg: {}, exPar: {}", cmd, status, reason, msg, extParUrlEncoder);

            // {uid:100001001,appid:1240050001,sid:100001001}
            Map<String, Object> exParm = getLocalObjMapper().readValue(extParUrlEncoder, Map.class);
            if (exParm == null || exParm.isEmpty() || !exParm.containsKey("uid")) {
                logger.error("Invalid extParUrlEncoder: {}", extParUrlEncoder);
                sendOuterResponse(request, response, makeResult(0, "-89:extension param error"));
                return;
            }

            long uid = Double.valueOf(exParm.get("uid").toString()).longValue();
            if (uid <= 0L) {
                logger.error("[MMS] Invalid extParUrlEncoder: {}", extParUrlEncoder);
                sendOuterResponse(request, response, makeResult(0, "-89:extension param error"));
                return;
            }
            String ruleDesc = "";
            if (exParm.get("ruleDesc") != null) {
                ruleDesc = exParm.get("ruleDesc").toString();
            }

            reportService.punishLive(uid, cmd, reason, ruleDesc);

            sendOuterResponse(request, response, makeResult(1, "punish finish"));
        } catch (Exception e) {
            logger.error("MMS Punish error.", e);
            sendOuterResponse(request, response, makeResult(0, "-29:punish error"));
        }
    }

    /**
     * 校验上报处理的请求。
     * 
     * @param content
     * @param sign
     * @return 校验结果
     */
    private boolean auth(String content, String sign) {
        boolean ret = false;

        try {
            byte[] keyBytes = Base64.decodeBase64(PUB_KEY);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance(keyFactory.getAlgorithm());
            signature.initVerify(pubKey);
            signature.update(content.getBytes());
            ret = signature.verify(Base64.decodeBase64(sign));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Verify request[" + content + "] encounter NoSuchAlgorithmException.", e);
        } catch (InvalidKeySpecException e) {
            logger.error("Verify request[" + content + "] encounter InvalidKeySpecException.", e);
        } catch (InvalidKeyException e) {
            logger.error("Verify request[" + content + "] encounter InvalidKeyException.", e);
        } catch (SignatureException e) {
            logger.error("Verify request[" + content + "] encounter SignatureException.", e);
        } catch (Exception e) {
            logger.error("Verify request[" + content + "] encounter error.", e);
        }
        return ret;
    }

    private Map<String, Object> makeResult(int code, String message) {
        Map<String, Object> ret = Maps.newHashMap();
        ret.put("code", code);
        ret.put("message", message);

        return ret;
    }

}
