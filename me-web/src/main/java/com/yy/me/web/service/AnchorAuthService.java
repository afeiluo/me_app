package com.yy.me.web.service;

import static com.yy.me.config.CntConfService.DEFAULT_CONF_ANCHOR_AUTH_SWITCH;
import static com.yy.me.http.BaseServletUtil.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.service.inner.MessageService;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

import com.google.common.collect.Maps;
import com.yy.cnt.ControlCenterService;
import com.yy.cs.base.redis.RedisClientFactory;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.AnchorService;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.anchor.family.entity.AnchorRecommendType;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.AnchorAuthMongoDBMapper;
import com.yy.me.entity.AnchorAuth;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.cache.LsImportantActCallback;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.client.entity.LsAction;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.service.SendSmsService;
import com.yy.me.time.MaskClock;
import com.yy.me.web.dao.UserExtentionMongoDBMapper;

@Service
public class AnchorAuthService {
    private static final Logger logger = LoggerFactory.getLogger(AnchorAuthService.class);

    @Autowired
    private AnchorAuthMongoDBMapper anchorAuthMongoDBMapper;
    @Autowired
    private UserExtentionMongoDBMapper userExtentionMongoDBMapper;
    @Autowired
    @Qualifier("anchorServiceHessianClient")
    private ReferenceFactory<AnchorService> anchorService;

    @Autowired
    private MetricsClient metricsClient;
    @Autowired
    private RedisClientFactory cntRedisFactory;
    @Autowired
    private SendSmsService sendSmsService;
    @Value("#{settings['metrics.appName']}")
    private String appName;
    @Value("#{settings['node.productEnv']}")
    private boolean productEnv;
    @Autowired
    private ControlCenterService controlCenterService;
    @Autowired
    private CntConfService cntConfService;
    @Autowired
    private MessageService messageService;

    private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyyMMdd");

    private static final String IDCARD_AES_SECRET = "yyDFFKiMf34285DM";

    @PostConstruct
    public void incrUserLiveCount() {
        GeneralLiveShowClient.initLsImportantActCallback(appName, controlCenterService, new LsImportantActCallback() {
            @Override
            public void start(long timestamp, long partialOrder, String lid, long sid, long anchorUid, LiveShowDto dto,
                    LsAction mqData) {
                userExtentionMongoDBMapper.incrLiveCount(anchorUid);
            }
        }, false);
    }

    public void checkUser(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            if (!cntConfService.getAnchorAuthSwitch()) {
                sendResponseAuto(request,response, genMsgObj(SUCCESS));
                return;
            }
            Anchor anchor = anchorService.getClient().findByUid(uid);
            if (anchor != null && anchor.getRecommandType() == AnchorRecommendType.SignAnchor.getType()) {
                sendResponseAuto(request,response, genMsgObj(SUCCESS));
                return;
            }
            AnchorAuth anchorAuth = anchorAuthMongoDBMapper.find(uid);
            int count=cntConfService.fetchConfInt(DEFAULT_CONF_ANCHOR_AUTH_SWITCH,"count",3);
            if (anchorAuth == null ) {
                if (getNormalUserAuthStatus(uid, count) == 2) {
                    sendResponseAuto(request, response, genMsgObj(SUCCESS));
                    return;
                }
            }
            genAnchorAuthResponse(anchorAuth, request,response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponseAuto(request,response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "checkUser", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    private int getNormalUserAuthStatus(long uid,int count) throws Exception {
        if (userExtentionMongoDBMapper.getLiveCount(uid) < count) {
            return 2;
        }
        return 0;
    }

    public void checkUserPhone(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            AnchorAuth anchorAuth = anchorAuthMongoDBMapper.find(uid);
            boolean bind = anchorAuth != null && anchorAuth.getPhone() != null;
            Map<String, Object> map = Maps.newHashMap();
            map.put("bind", bind);
            sendResponse(request, response, genMsgObj(SUCCESS, null, map));
        } catch (Exception e) {
            logger.error("Check user phone error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "checkUserPhone", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    public void sendCaptcha(long uid, String phone, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            long countPhone = anchorAuthMongoDBMapper.countPhone(phone);
            if (countPhone >= 3) {
                sendResponse(request, response, genMsgObj(ANCHOR_AUTH_PHONE_BIND_EXCEED_LIMIT));
                return;
            }
            sendCaptchaSms(uid, phone);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("Send captcha error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "sendCaptcha", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    public void verifyCaptcha(long uid, String phone, String captcha, HttpServletRequest request,
            HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            if (verifyCaptcha(uid, phone, captcha)) {
                long countPhone = anchorAuthMongoDBMapper.countPhone(phone);
                if (countPhone >= 3) {
                    sendResponse(request, response, genMsgObj(ANCHOR_AUTH_PHONE_BIND_EXCEED_LIMIT));
                    return;
                }
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.find(uid);
                if (anchorAuth != null && anchorAuth.getPhone() != null) {
                    sendResponse(request, response, genMsgObj(ANCHOR_AUTH_ALREADY_BIND_PHONE));
                    return;
                } else {
                    anchorAuthMongoDBMapper.bindPhone(uid, phone);
                    sendResponse(request, response, genMsgObj(SUCCESS));
                    return;
                }
            } else {
                sendResponse(request, response, genMsgObj(ANCHOR_AUTH_CAPTCHA_VERIFY_FAIL));
                return;
            }
        } catch (Exception e) {
            logger.error("Verify captcha error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "verifyCaptcha", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    public void checkIdCard(long uid, String idCard, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            long count = anchorAuthMongoDBMapper.countIdCard(idCard);
            if (count >= 3) {
                sendResponse(request, response, genMsgObj(ANCHOR_AUTH_IDCARD_BIND_EXCEED_LIMIT));
            } else {
                sendResponse(request, response, genMsgObj(SUCCESS));
            }
        } catch (Exception e) {
            logger.error("Check id card error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "checkIdCard", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    public void submitAnchorAuth(long uid, String name, String idCard, String image1, String image2,
            HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            image1 = fixImageUrl(image1);
            image2 = fixImageUrl(image2);
            long count = anchorAuthMongoDBMapper.countIdCard(idCard);
            if (count >= 3) {
                sendResponse(request, response, genMsgObj(ANCHOR_AUTH_IDCARD_BIND_EXCEED_LIMIT));
                return;
            }
            if (anchorAuthMongoDBMapper.saveAuthMsg(uid, name, idCard, image1, image2)) {
                sendResponse(request, response, genMsgObj(SUCCESS));
            } else {
                sendResponse(request, response, genMsgObj(ANCHOR_AUTH_ALREADY_SUBMIT));
            }

        } catch (Exception e) {
            logger.error("Submit anchor auth info error.", e);
            sendResponse(request, response, genMsgObj(FAILED));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "checkIdCard", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    private String fixImageUrl(String img) {
        return img.replace("mobilereportpic", "meimage");
    }

    private void genAnchorAuthResponse(AnchorAuth anchorAuth, HttpServletRequest request, HttpServletResponse response) {
        if (anchorAuth == null || anchorAuth.getStatus() == 0) {
            sendResponse(request, response, genMsgObj(ANCHOR_NOT_AUTH));
        } else if (anchorAuth.getStatus() == 1) {
            sendResponse(request, response, genMsgObj(ANCHOR_AUTH_REVIEWING));
        } else if (anchorAuth.getStatus() == 2) {
            sendResponse(request, response, genMsgObj(SUCCESS));
        } else if (anchorAuth.getStatus() == -1) {
            sendResponse(request, response, genMsgObj(ANCHOR_AUTH_FAIL, anchorAuth.getReason()));
        } else {
            throw new IllegalStateException();
        }
    }

    private void sendCaptchaSms(long uid, String phone) {
        long timestamp = System.currentTimeMillis();

        String captcha = RandomStringUtils.randomNumeric(6);
        String sms = "您的ME实名认证验证码是:" + captcha + ",五分钟内有效";
        try (Jedis jedis = cntRedisFactory.getMasterPool().getResource()) {
            String hashKey = "anchorAuth" + dateFormatter.print(timestamp);
            long count = jedis.hincrBy(hashKey, "phoneDayLimit" + phone, 1);
            if (count > 100) {
                return;
            }

            jedis.hset(hashKey, "captcha_" + uid + "_" + phone, captcha + "_" + timestamp);
            sendSmsService.sendToMobile("86" + phone, sms);
        }
    }

    private boolean verifyCaptcha(long uid, String phone, String captcha) {
        long timestamp = System.currentTimeMillis();
        try (Jedis jedis = cntRedisFactory.getSlavePool().getResource()) {
            String hashKey = "anchorAuth" + dateFormatter.print(timestamp);

            String ret = jedis.hget(hashKey, "captcha_" + uid + "_" + phone);
            if (ret == null) {
                return false;
            }
            String redisCaptcha = null;
            Long redisTimeStamp = null;
            try {
                String[] tmp = ret.split("_");
                if (tmp.length == 2) {
                    redisCaptcha = tmp[0];
                    redisTimeStamp = Long.valueOf(tmp[1]);
                }
            } catch (Exception e) {
                // eat it
            }
            if (redisCaptcha == null || redisTimeStamp == null) {
                return false;
            }
            if (redisCaptcha.equals(captcha) && timestamp - redisTimeStamp < TimeUnit.MINUTES.toMillis(5)) {
                return true;
            }
        }
        return false;
    }

    public void idCardCallback(String username, int status, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            long uid;
            if(productEnv) {
                String raw = aesDecrypt(username, DigestUtils.md5(IDCARD_AES_SECRET));
                uid = Long.parseLong(raw);
            }else {
                uid = Long.parseLong(username);
            }
            if (status == 1 || status == 2) {
                //成功
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.setReviewResult(uid, 1, true, "");
                if (anchorAuth != null && anchorAuth.getResult2() == 1) {
                    anchorAuthMongoDBMapper.updateStatus(uid, 2, "");
                    resultNotify(anchorAuth,true);
                }
            } else {
                //失败
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.setReviewResult(uid, 1, false, "身份证姓名验证失败");
                if (anchorAuth != null && anchorAuth.getResult1() != -1 && anchorAuth.getResult1() != -1) {
                    resultNotify(anchorAuth,false);
                }
            }
            sendOuterResponse(request, response, "ok");
        } catch (Exception e) {
            logger.error("Process id card callback error.", e);
            sendOuterResponse(request, response, "server error");
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "idCardCallback", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    private static String aesDecrypt(String data, byte[] secret) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(secret, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] from = Hex.decodeHex(data.toCharArray());
        byte[] to = cipher.doFinal(from);
        return new String(to, "UTF-8");
    }

    public void realnameCallback(long uid, int status, String reason, HttpServletRequest request,
            HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        Map<String, Object> map = Maps.newHashMap();
        map.put("code", 1);
        map.put("message", "");
        try {
            if (status == 1) {
                //通过
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.setReviewResult(uid, 2, true, "");
                if (anchorAuth != null && anchorAuth.getResult1() == 1) {
                    anchorAuthMongoDBMapper.updateStatus(uid, 2, "");
                    resultNotify(anchorAuth,true);
                }
            } else {
                //失败
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.setReviewResult(uid, 2, false, reason);
                if (anchorAuth != null && anchorAuth.getResult1() != -1 && anchorAuth.getResult2() != -1) {
                    resultNotify(anchorAuth,false);
                }
            }
            sendOuterResponse(request, response, map);
        } catch (Exception e) {
            logger.error("Process real name callback error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
            map.put("code", 0);
            sendOuterResponse(request, response, map);
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, "anchorAuth", this.getClass(), "realnameCallback", 1,
                    MaskClock.getCurtime() - t, rescode);
        }

    }

    private void resultNotify(AnchorAuth anchorAuth,boolean pass){
        try {
            messageService.pushAnchorAuthResult2User(anchorAuth.getUid(),pass);
        } catch (Exception e) {
            logger.error("resultNotify sendPush error",e);
        }
        try {
            sendSmsService.sendToMobile("86" + anchorAuth.getPhone(),pass?"你的实名认证申请已通过审核，快来ME开播吧~":"你的实名认证申请未通过审核，请重新提交申请。");
        } catch (Exception e) {
            logger.error("resultNotify sendSms error",e);
        }
    }
}
