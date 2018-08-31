package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.web.dao.DeviceMongoDBMapper.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.dao.HeaderReviewMongoDBMapper;
import com.yy.me.dao.MedalMongoDBMapper;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.user.OnlineStatus;
import com.yy.me.user.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.mongodb.DBObject;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.config.CntConfService;
import com.yy.me.enums.ViolationMsgType;
import com.yy.me.geo.thrift.GeoHelper;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.thrift.ClientType;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.service.inner.FillService;
import com.yy.me.config.GeneralConfService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.time.MaskClock;
import com.yy.me.user.UserHessianService;
import com.yy.me.util.VersionUtil;
import com.yy.me.web.dao.DeviceMongoDBMapper;
import com.yy.me.web.dao.PopupAdMongoDBMapper;
import com.yy.me.web.dao.SplashInfoMongoDBMapper;
import com.yy.me.web.entity.PopupAd;
import com.yy.me.web.entity.SplashInfo;
import com.yy.me.message.thrift.push.OsType;

/**
 * @author qiaolinfei
 */
@Service
public class AppService {
    private static Logger logger = LoggerFactory.getLogger(AppService.class);
    private String progress = "app";

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    @Autowired
    private MessageMongoDBMapper dbMapper;
    @Autowired
    private FillService fillService;
    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    @Qualifier("geoThriftService")
    private ReferenceFactory<com.yy.me.geo.thrift.GeoService> geoThriftService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    private SplashInfoMongoDBMapper splashMongoDBMapper;

    @Autowired
    private PopupAdMongoDBMapper popupAdMongoDBMapper;

    @Autowired
    private GeneralConfService generalConfService;

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private DeviceMongoDBMapper deviceMongoDBMapper;

    @Autowired
    private CntConfService cntConfService;
    @Autowired
    private CntBetaUpgradeService betaUpgradeService;
    @Autowired
    private HeaderReviewMongoDBMapper headerReviewMongoDBMapper;
    @Autowired
    private MedalMongoDBMapper medalMongoDBMapper;

    public void checkDevice(long uid, int osType, String deviceId, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            DBObject ret = null;
            if (StringUtils.isNotBlank(deviceId) && (osType == 0 || osType == 1)) {
                ClientType clientType = osType == 0 ? ClientType.ANDROID : ClientType.IOS;
                ret = deviceMongoDBMapper.checkBanned(clientType.getValue(), deviceId);
            }
            if (ret != null) {
                Date banedActionTime = (Date) ret.get(FIELD_DEVICE_BANED_ACTION_TIME);
                String banedType = (String) ret.get(FIELD_DEVICE_BANED_TYPE);
                Date banedEndTime = (Date) ret.get(FIELD_DEVICE_BANED_END_TIME);
                String banStr = messageMongoDBMapper.fm(banedType, MessageService.dateFormatter.format(banedActionTime),
                        MessageService.dateFormatter.format(banedEndTime));

                sendResponse(request, response, genMsgObj(DEVICE_BANED, banStr));
            } else {
                sendResponse(request, response, genMsgObj(SUCCESS));
            }
        } catch (Exception e) {
            logger.error("check device fail, uid:" + uid, e);
            sendResponse(request, response, genMsgObj(FAILED, "check device fail."));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "checkDevice", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    /**
     * 上报在线状态已经上次活跃的时间
     * 
     * @param uid
     * @param status
     * @param asyncContext
     */
    public void reportOnlineAndLastActiveTime(Long uid, int status, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            userHessianService.getClient().updateOnlineStatus(uid, OnlineStatus.findByValue(status));
            long cur = System.currentTimeMillis();
            logger.info("[reportOnlineAndLastActiveTime-1] cost:" + (cur - t) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("report online error.", e);
            sendResponse(request, response, genMsgObj(FAILED, "report online status error."));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "reportOnlineAndLastActiveTime", 1,
                    MaskClock.getCurtime() - t, rescode);
        }
    }

    public void registerPush(Long uid, int osType, int notifyType, String notifyId, int connType, String connId, String deviceId,
            HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            String version = request.getHeader(HEADER_X_CLIENT_VER);
            String locale = request.getHeader(HEADER_X_LOCALE);
            String clientStr = request.getHeader(HEADER_X_CLIENT);

            if (version != null && clientStr != null) {
                String androidMinVer = cntConfService.findMinVer(FIELD_FORCE_UPGRADE_ANDROID);
                String iosMinVer = cntConfService.findMinVer(FIELD_FORCE_UPGRADE_IOS);

                if ("iOS".equals(clientStr) && VersionUtil.compareVersion(version, iosMinVer) < 0) {
                    sendResponse(request, response, genMsgObj(VERSION_TOO_OLD, "iOS Should Update!")); // 强制升级
                    return;
                }
                if ("Android".equals(clientStr) && VersionUtil.compareVersion(version, androidMinVer) < 0) {
                    sendResponse(request, response, genMsgObj(VERSION_TOO_OLD, "Android Should Update!")); // 强制升级
                    return;
                }
            }

            if (StringUtils.isNotBlank(deviceId) && (osType == 0 || osType == 1)) {
                try {
                    ClientType clientType = osType == 0 ? ClientType.ANDROID : ClientType.IOS;
                    DBObject retObj = deviceMongoDBMapper.checkBanned(clientType.getValue(), deviceId);
                    if (retObj != null) {
                        Date banedActionTime = (Date) retObj.get(FIELD_DEVICE_BANED_ACTION_TIME);
                        String banedType = (String) retObj.get(FIELD_DEVICE_BANED_TYPE);
                        Date banedEndTime = (Date) retObj.get(FIELD_DEVICE_BANED_END_TIME);
                        String banStr = messageMongoDBMapper.fm(banedType, MessageService.dateFormatter.format(banedActionTime),
                                MessageService.dateFormatter.format(banedEndTime));
                        if (banedType.equals(ViolationMsgType.U_BANED_TIPS_LS_DEVICE_3.getValue())
                                || banedType.equals(ViolationMsgType.U_BANED_TIPS_LS_DEVICE_4.getValue())) {
                            String item = (String) retObj.get(FIELD_RULE_DESC_ITEM);
                            String subItem = (String) retObj.get(FIELD_RULE_DESC_SUB_ITEM);
                            String desc = (String) retObj.get(FIELD_RULE_DESC_DESC);
                            banStr = messageMongoDBMapper.fm(banedType, MessageService.dateFormatter.format(banedActionTime),
                                    MessageService.dateFormatter.format(banedEndTime), null, item, subItem, desc);
                            Map<String, String> banMap = new HashMap<>();
                            banMap.put(FIELD_RULE_DESC_ITEM, item);
                            banMap.put(FIELD_RULE_DESC_SUB_ITEM, subItem);

                            sendResponse(request, response, genMsgObj(DEVICE_BANED, banStr, banMap));
                            return;
                        }
                        sendResponse(request, response, genMsgObj(DEVICE_BANED, banStr));
                        return;
                    } else {
                        deviceMongoDBMapper.insert(osType, deviceId);
                    }
                    deviceMongoDBMapper.insertUserLoginDevice(uid,clientType.getValue(), deviceId);
                } catch (Exception e) {
                    logger.error("RegisterPush 4 device fail ,uid:{}, deviceId:{}, errmsg:{}", uid, deviceId, e.getMessage(), e);
                }
            }

            sendResponse(request, response, genMsgObj(SUCCESS));

            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "loginReport", 3, MaskClock.getCurtime() - t, rescode);
            long t3 = System.currentTimeMillis();

            com.yy.me.message.thrift.push.UserInfo userInfo = new com.yy.me.message.thrift.push.UserInfo();
            userInfo.setUid(uid);
            userInfo.setOsType(OsType.valueOf(osType));
            userInfo.setPushId(connId);
            userInfo.setVersion(version);
            userInfo.setLocale(locale);

            boolean ret = dbMapper.registerPushMessage(userInfo);
            if (!ret) {
                logger.error("Register push info failed.");
            }
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "loginReport", 4, MaskClock.getCurtime() - t3, rescode);
        } catch (Exception e) {
            logger.error("RegisterPush fail ,uid:" + uid, e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));

            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "loginReport", 1, MaskClock.getCurtime() - t, rescode);
        }

    }

    public void unRegister(Long uid, int osType, int notifyType, String notifyId, int connType, String connId, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String version = request.getHeader(HEADER_X_CLIENT_VER);
            String locale = request.getHeader(HEADER_X_LOCALE);

            com.yy.me.message.thrift.push.UserInfo userInfo = new com.yy.me.message.thrift.push.UserInfo();
            userInfo.setUid(uid);
            userInfo.setOsType(OsType.valueOf(osType));
            userInfo.setPushId(connId);
            userInfo.setVersion(version);
            userInfo.setLocale(locale);

            Boolean ret = dbMapper.unregisterPushMessage(userInfo);
            if (ret) {
                sendResponse(request, response, genMsgObj(SUCCESS));
            } else {
                sendResponse(request, response, genMsgObj(FAILED));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }

    }

    public void getSplashAndPopupAd(String channelId, HttpServletRequest request, HttpServletResponse response) {
        try {
            Date current = new Date();
            List<SplashInfo> splashList = splashMongoDBMapper.findByTime(channelId, current);
            List<PopupAd> adList = popupAdMongoDBMapper.findByTime(current);

            for (PopupAd popupAd : adList) {
                if (popupAd.getJumpType() == PopupAd.PopupAdJumpType.LIVEROOM.getValue()) {// 跳直播间
                    LiveShowDto liveShow = GeneralLiveShowClient.getLsByLid(popupAd.getJumpLid());// 这里的jumpUrl实际上是lid(直播id)
                    if (liveShow != null && liveShow.getStartingNow() == true) {
                        popupAd.setLiveShow(fillService.fillLiveShow(userHessianService.getClient().getUserByUid(liveShow.getUid(), false), liveShow));
                    }
                } else if (popupAd.getJumpType() == PopupAd.PopupAdJumpType.LIVEUIDROOM.getValue()) {// 根据uid跳直播间,有直播就设置lid
                    LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(popupAd.getJumpUid());
                    if (liveShow != null) {
                        if (liveShow.getStartingNow()) {
                            popupAd.setLiveShow(fillService.fillLiveShow(userHessianService.getClient().getUserByUid(liveShow.getUid(), false),
                                    liveShow));
                            popupAd.setJumpLid(liveShow.getLid());
                        }
                    }
                }
            }

            List<String> animList = generalConfService.fetchStartAnimations();

            Map<String, List<?>> infoMap = Maps.newHashMap();
            infoMap.put("splashs", splashList);
            infoMap.put("popupAds", adList);
            infoMap.put("animations", animList);

            sendResponse(request, response, genMsgObj(SUCCESS, null, infoMap));
        } catch (Exception e) {
            logger.error("Get splash info and popup ad error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void getBetaUpgradeConf(HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            String clientStr = request.getHeader(HEADER_X_CLIENT);
            String ver = request.getHeader(HEADER_X_CLIENT_VER);
            String iosVer = (String) betaUpgradeService.getConf(CntBetaUpgradeService.KEY_IOSVER);
            String androidVer = (String) betaUpgradeService.getConf(CntBetaUpgradeService.KEY_ANDROIDVER);
            if ("Android".equals(clientStr) && !StringUtils.isEmpty(ver) && !StringUtils.isEmpty(androidVer)
                    && VersionUtil.compareVersion(ver, androidVer) < 0) {
                sendResponse(request, response, genMsgObj(SUCCESS, null, betaUpgradeService.getAndroidConfMap()));
            } else if ("iOS".equals(clientStr) && !StringUtils.isEmpty(ver) && !StringUtils.isEmpty(iosVer)
                    && VersionUtil.compareVersion(ver, iosVer) < 0) {
                sendResponse(request, response, genMsgObj(SUCCESS, null, betaUpgradeService.getIosConfMap()));
            } else {
                sendResponse(request, response, genMsgObj(SUCCESS));
            }
        } catch (Exception e) {
            logger.error("get beta upgrade conf error", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getBetaUpgradeConf", 1, MaskClock.getCurtime() - t,
                    rescode);
        }
    }

    public void getAllBusiness(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, userHessianService.getClient().getAllBusiness()));
        } catch (Exception e) {
            logger.error("getAllBusiness error", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient
                    .report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getAllBusiness", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void getAllInterest(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, userHessianService.getClient().getAllInterest()));
        } catch (Exception e) {
            logger.error("getAllInterest error", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient
                    .report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getAllInterest", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void getAllInterestWithDefault(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, userHessianService.getClient().getAllInterestWithDefault()));
        } catch (Exception e) {
            logger.error("getAllInterestWithDefault error", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getAllInterestWithDefault", 1, MaskClock.getCurtime()
                    - t, rescode);
        }
    }

    public void findLocationCity(long uid, Double longitude, Double latitude, String reqIp, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            String city = GeoHelper.findCity(uid, longitude, latitude, reqIp, geoThriftService, metricsClient);
            sendResponse(request, response, genMsgObj(SUCCESS, null, BaseServletUtil.getLocalObjMapper().createObjectNode().put("cityName", city)));
        } catch (Exception e) {
            logger.error("find  Locations error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "findLocationCity", 1, MaskClock.getCurtime() - t,
                    rescode);
        }
    }
}
