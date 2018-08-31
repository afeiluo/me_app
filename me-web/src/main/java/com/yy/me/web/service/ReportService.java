package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.getLocalObjMapper;
import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.RET_LS_LID;
import static com.yy.me.mongo.MongoUtil.appendObjId;
import static com.yy.me.mongo.MongoUtil.javaObj2Db;
import static com.yy.me.service.inner.ServiceConst.MMS_ACTION_TIME;
import static com.yy.me.service.inner.ServiceConst.MMS_HEAD_URL;
import static com.yy.me.service.inner.ServiceConst.MMS_TYPE;
import static com.yy.me.service.inner.ServiceConst.MMS_VIDEO_URL;
import static com.yy.me.service.inner.ServiceConst.RET_U_DEVICE_ID;
import static com.yy.me.service.inner.ServiceConst.RET_U_LINK_DEVICE_ID;
import static com.yy.me.service.inner.ServiceConst.RET_U_LINK_OS_TYPE;
import static com.yy.me.service.inner.ServiceConst.RET_U_OS_TYPE;
import static com.yy.me.service.inner.ServiceConst.RET_U_UID;
import static com.yy.me.user.UserInfo.Fields.FIELD_U_HEADER_URL;

import java.util.Map;

import com.yy.me.dao.MedalMongoDBMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.bs2.Bs2Service;
import com.yy.me.dao.HeaderReviewMongoDBMapper;
import com.yy.me.dao.UserBanMongoDBMapper;
import com.yy.me.enums.MmsType;
import com.yy.me.enums.PunishSource;
import com.yy.me.enums.PunishType;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LinkUserInfo;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.client.util.LiveShowUtil;
import com.yy.me.liveshow.thrift.ClientType;
import com.yy.me.service.BanService;
import com.yy.me.service.LiveShowService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserInfoUtil;
import com.yy.me.user.entity.MultiInfo;
import com.yy.me.util.search.HiddoSearch;

/**
 * 
 * @author jiangchengyan
 * 
 */
@Service
public class ReportService {
    private static Logger logger = LoggerFactory.getLogger(ReportService.class);

    /**
     * 秩序组审核记录
     */
    public static final String COLLETION_MMS_REPORT_REV_NAME = "mms_report_rev";

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    private LiveShowService liveShowService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserBanMongoDBMapper userBanMongoDBMapper;

    @Autowired
    private HeaderReviewMongoDBMapper headerReviewMongoDBMapper;

    @Autowired
    private MedalMongoDBMapper medalMongoDBMapper;

    @Autowired
    private AppService appService;

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate generalMongoTemplate;

    @Autowired
    private BanService banService;

    @Autowired
    private HiddoSearch hiddoSearch;

    private static String aclass = "1";

    private static String bclass = "2";

    private static String cclass = "4";

    /**
     * 图片处罚回调。
     * 
     * @param resultCode 1:不违规，2:违规
     * @param exJsonParam
     */
    public void verifyImgCallback(String appkey, String serial, int resultCode, String cmd, String reason, String exJsonParam) {// cmd=1
                                                                                                                                // 严重就是A类
                                                                                                                                // =2
        // 普通就是B类
        logger.info("MMS verifyImgCallback - serial:{}, resultCode: {}, cmd:{}, exJsonParam: {}", serial, resultCode, cmd, exJsonParam);
        ObjectNode jo = null;
        try {
            jo = (ObjectNode) getLocalObjMapper().readTree(exJsonParam);
            // jo = getLocalObjMapper().convertValue(exJsonParam, ObjectNode.class);
            int type = jo.get(MMS_TYPE).asInt();
            long uid = jo.get(RET_U_UID).asLong();
            long actionTime = jo.get(MMS_ACTION_TIME) == null ? System.currentTimeMillis() : jo.get(MMS_ACTION_TIME).asLong();
            logger.info("MMS verifyImgCallback type:{} uid:{} actionTime:{} resultCode:{}", type, uid, actionTime, resultCode);
            if (resultCode == 2) {
                String ruleDesc = "";
                if (jo.get("ruleDesc") != null) {
                    ruleDesc = jo.get("ruleDesc").toString();
                }
                if (type == MmsType.LS_SNAPSHOT.getValue()) {

                    LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
                    String lid = liveShow == null ? null : liveShow.getLid();

                    String deviceId = (jo.get(RET_U_DEVICE_ID) == null) ? null : jo.get(RET_U_DEVICE_ID).asText();
                    int osType = (jo.get(RET_U_OS_TYPE) == null) ? null : jo.get(RET_U_OS_TYPE).asInt();

                    UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
                    if (userInfo != null) {
                        logger.info("MMS verifyLiveCallback: Ban User Ls by Snapshot - resultCode: {}, exJsonParam:{}, userInfo:{}", resultCode,
                                exJsonParam, userInfo);
                    } else {
                        logger.warn("MMS verifyLiveCallback: Ban User Ls by Snapshot - resultCode: {}, UserInfo Not exist! exJsonParam:{}",
                                resultCode, exJsonParam);
                    }

                    if (cmd.equals(aclass)) {
                        banService.lsAViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, osType, deviceId, false, ruleDesc);
                    }
                    if (cmd.equals(bclass) || cmd.equals(cclass)) {
                        banService.lsBViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, false, ruleDesc);
                    }
                } else if (type == MmsType.LS_LINK_SNAPSHOT.getValue()) {
                    String lid = (jo.get(RET_LS_LID) == null) ? null : jo.get(RET_LS_LID).asText();
                    String deviceId = (jo.get(RET_U_DEVICE_ID) == null) ? null : jo.get(RET_U_LINK_DEVICE_ID).asText();
                    Integer osType = (jo.get(RET_U_OS_TYPE) == null) ? null : jo.get(RET_U_LINK_OS_TYPE).asInt();

                    UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
                    if (userInfo != null) {
                        logger.info("MMS verifyLiveCallback: Ban Link User Ls by Snapshot - resultCode: {}, exJsonParam:{}, userInfo:{}", resultCode,
                                exJsonParam, userInfo);
                    } else {
                        logger.warn("MMS verifyLiveCallback: Ban Link User Ls by Snapshot - resultCode: {}, UserInfo Not exist! exJsonParam:{}",
                                resultCode, exJsonParam);
                    }

                    if (cmd.equals(aclass)) {
                        banService.lsAViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, osType, deviceId, true, ruleDesc);
                    }
                    if (cmd.equals(bclass) || cmd.equals(cclass)) {
                        banService.lsBViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, true, ruleDesc);
                    }
                    // banService.lsLinkUserViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, osType, deviceId);
                } else if (type == MmsType.USER_HEAD.getValue()) {
                    UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
                    if (userInfo != null) {
                        logger.info("MMS verifyLiveCallback: Ban User Header Url - resultCode: {}, exJsonParam:{}, userInfo:{}", resultCode,
                                exJsonParam, userInfo);
                        banService.headViolate(PunishSource.AUDIT, uid, reason, actionTime);
                    } else {
                        logger.warn("MMS verifyLiveCallback: Ban User Header Url - resultCode: {}, UserInfo Not exist! uid:{}, exJsonParam:{}",
                                resultCode, uid, exJsonParam);
                    }
                    if (!UserInfoUtil.DEFAULT_PIC.equals(userInfo.getHeaderUrl())) {
                        Bs2Service.deletePicUrl(userInfo.getHeaderUrl());// 删除图片具体内容
                    }
                } else if (type == MmsType.USER_NICK.getValue()) {
                    // 如果是ME自己的appkey表示是昵称违规，要做额外处理，不然是公屏处罚，不做额外处理
                    banService.textViolate(PunishSource.AUDIT, PunishType.NICK, uid, reason, actionTime, "999911050".equals(appkey), hiddoSearch);
                } else if (type == MmsType.USER_SIGNATURE.getValue()) {
                    // 这里同nick处理
                    banService
                            .textViolate(PunishSource.AUDIT, PunishType.SIGNATURE, uid, reason, actionTime, "999911050".equals(appkey), hiddoSearch);
                } else if (type == MmsType.USER_VIDEO.getValue()) {// 3.1个人资料视频违规处理
                    String videoUrl = (jo.get(MMS_VIDEO_URL) == null) ? null : jo.get(MMS_VIDEO_URL).asText();
                    MultiInfo multiInfo = new MultiInfo();
                    multiInfo.setPicUrl(videoUrl);
                    multiInfo.setType(1);
                    userHessianService.getClient().delUserMultiInfo(uid, multiInfo);
                    // TODO 发送系统消息给违规用户
                } else if (type == MmsType.USER_MULTI_HEAD.getValue()) {// 3.1个人资料相册违规处理
                    String headUrl = (jo.get(MMS_HEAD_URL) == null) ? null : jo.get(MMS_HEAD_URL).asText();
                    // UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, false);
                    // if (headUrl.equals(userInfo.getHeaderUrl())) {
                    // Bs2Service.deletePicUrl(userInfo.getHeaderUrl());
                    // UserInfo update = new UserInfo();
                    // update.setUid(uid);
                    // update.setHeaderUrl(UserInfoUtil.DEFAULT_PIC);
                    // userHessianService.getClient().updateByUid(update);// 删除并恢复默认头像
                    // }
                    MultiInfo multiInfo = new MultiInfo();
                    multiInfo.setPicUrl(headUrl);
                    multiInfo.setType(0);
                    logger.info("delete user:{}'s album pic:{}", uid, headUrl);
                    userHessianService.getClient().delUserMultiInfo(uid, multiInfo);
                    // TODO 发送系统消息给违规用户
                }
                // TODO　发送系统消息给违规用户
            } else if (resultCode == 1) {
                if (type == MmsType.USER_HEAD.getValue()) {
                    String headerUrl = jo.get(FIELD_U_HEADER_URL).asText();
                    if (StringUtils.isNotEmpty(headerUrl) && !UserInfoUtil.DEFAULT_PIC.equals(headerUrl)) {
                        // 3.0以后的新版本上传的头像,加入审核库
                        if(medalMongoDBMapper.isValid(MedalMongoDBMapper.MEDAL_ID_HEADER_REVIEW)){
                            headerReviewMongoDBMapper.upsertRecord(uid, headerUrl);
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (jo != null) {
            try {
                jo.put("serial", serial);
                jo.put("resultCode", resultCode);
                DBObject obj = javaObj2Db(generalMongoTemplate, getLocalObjMapper().convertValue(jo, Map.class), null);
                appendObjId(obj);
                generalMongoTemplate.getDb().getCollection(COLLETION_MMS_REPORT_REV_NAME).save(obj);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 直播处罚回调。
     * 
     * @param uid 违规主播uid
     * @param ruleDesc
     */
    public void punishLive(long uid, String cmd, String reason, String ruleDesc) {
        logger.info("MMS punishLive - uid:{}, cmd:{}", uid, cmd);
        try {
            Boolean isLinkGuest = null;
            Integer osType = null;
            String deviceId = null;
            LiveShowDto liveShow = GeneralLiveShowClient.getLsByUid(uid);
            if (liveShow != null) {
                isLinkGuest = false;
                if (liveShow.getClientType() != null) {
                    osType = "iOS".equals(liveShow.getClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue();
                    deviceId = liveShow.getDeviceId() != null ? liveShow.getDeviceId() : null;
                }
                logger.info("MMS punishLive: Close LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
            } else {
                liveShow = GeneralLiveShowClient.getLsByLinkUid(uid);
                if (liveShow != null) {
                    LinkUserInfo linkUser = LiveShowUtil.findLinkInfo(liveShow, uid);
                    if (linkUser != null) {
                        isLinkGuest = true;
                        if (linkUser.getClientType() != null) {
                            osType = "iOS".equals(linkUser.getClientType()) ? ClientType.IOS.getValue() : ClientType.ANDROID.getValue();
                            deviceId = linkUser.getDeviceId() != null ? linkUser.getDeviceId() : null;
                        }
                        logger.info("MMS punishLive: Cancel Link from LiveShow - uid: {}, LiveShow:{}", uid, liveShow);
                    }
                } else {
                    logger.info("MMS punishLive: Not linking - uid: {}, LiveShow:{}", uid, liveShow);
                }
            }
            long actionTime = System.currentTimeMillis();
            String lid = liveShow == null ? null : liveShow.getLid();

            if (cmd.equals(aclass)) {
                banService.lsAViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, osType, deviceId, isLinkGuest, ruleDesc);
            }
            if (cmd.equals(bclass) || cmd.equals(cclass)) {
                banService.lsBViolate(PunishSource.AUDIT, uid, lid, reason, actionTime, isLinkGuest, ruleDesc);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
