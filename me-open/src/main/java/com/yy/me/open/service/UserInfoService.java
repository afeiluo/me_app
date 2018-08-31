package com.yy.me.open.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.mongo.MongoUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.user.UserInfo.Fields.*;
import static com.yy.me.user.UserInfoUtil.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.anchor.family.entity.AnchorRecommendType;
import com.yy.me.bs2.Bs2Service;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.PaymentAccountMongoDBMapper;
import com.yy.me.entity.PaymentAccount;
import com.yy.me.enums.MmsType;
import com.yy.me.user.VerifyApplicationStatus;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.json.JsonUtil;
import com.yy.me.liveshow.client.entity.LsAction;
import com.yy.me.liveshow.client.mq.LsBroadcastAloProducer;
import com.yy.me.liveshow.client.util.LiveShowServletUtil;
import com.yy.me.liveshow.client.util.LsHeaderChecker;
import com.yy.me.liveshow.enums.StatisticsType;
import com.yy.me.liveshow.thrift.LsRequestHeader;
import com.yy.me.liveshow.util.GeneralUtil;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.metrics.MetricsClient.ProtocolType;
import com.yy.me.open.util.ReserveIdsUtil;
import com.yy.me.service.inner.FillService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.service.inner.StatisticsLogService;
import com.yy.me.time.MaskClock;
import com.yy.me.user.Sex;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserSource;
import com.yy.me.util.ContentMatcher;
import com.yy.me.util.search.HiddoSearch;

/**
 * 用户个人数据Service
 * 
 * @author JCY
 */
@Service
public class UserInfoService {

    private static Logger logger = LoggerFactory.getLogger(UserInfoService.class);
    private static String progress = "user";

    @Autowired
    private MetricsClient metricsClient;

    @Autowired
    private ArrangeAnchorService arrangeAnchorService;


    @Autowired
    private MessageMongoDBMapper messageMapper;

    @Autowired
    private HiddoSearch hiddoSearch;
    @Autowired
    private FillService fillService;

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    @Qualifier("mongoTemplateUser")
    private MongoTemplate userMongoTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MmsReportService mmsReportService;// 图片送审

    @Autowired
    private CensorWordWebService censorWordService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    private StatisticsLogService statisticsLogService;

    @Autowired
    private PaymentAccountMongoDBMapper paymentAccountMapper;

    @Autowired
    private AnchorRpcService anchorService;

    @Autowired
    private LsBroadcastAloProducer lsBroadcastAloProducer;

    @Autowired
    private ReferenceFactory<UserHessianService> userHessianService;

    private static Cache<Long, ObjectNode> userCache4AuthFilter = CacheBuilder.newBuilder().maximumSize(50000).expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    /**
     * @param uid
     * @param otherUid
     * @return
     * @throws Exception
     */
    public RetMsgObj find(Long uid, Long otherUid) throws Exception {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        // step1: 查本地缓存,若没有则查询DB
        ObjectNode otherUserInfoJo = userCache4AuthFilter.getIfPresent(otherUid);
        UserInfo otherUserInfo = null;
        if (otherUserInfoJo == null) {
            otherUserInfo = userHessianService.getClient().getUserByUid(otherUid,true);
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "find", 2, MaskClock.getCurtime() - t, rescode);
            if (otherUserInfo == null) {
                // TODO 被攻击！缓存失效
                logger.warn("Not Exist otherUid!uid:{}, otherUid:{}", uid, otherUid);
            } else {
                otherUserInfoJo = fillUserStat2(uid, otherUserInfo);
                metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "find", 3, MaskClock.getCurtime() - t, rescode);
                userCache4AuthFilter.put(otherUid, otherUserInfoJo);
            }
        } else {
            otherUserInfo = getLocalObjMapper().convertValue(otherUserInfoJo, UserInfo.class);
        }
        if (otherUserInfo == null) {
            Map<String, String> jo = Maps.newHashMap();
            jo.put(FIELD_U_USERNAME, genRankUsername());
            return genMsgObj(USER_NOT_EXIST, "Uid " + otherUid + " not Exist!", jo);// 用户不存在
        }
        if (otherUserInfo.getBaned() != null && otherUserInfo.getBaned()) {
            Date actionTime = otherUserInfo.getBanedActionTime();
            if (actionTime == null) {
                actionTime = otherUserInfo.getBanedEndTime();
            }
            String banStr = messageMongoDBMapper.fm(otherUserInfo.getBanedType(), MessageService.dateFormatter.format(actionTime),
                    MessageService.dateFormatter.format(otherUserInfo.getBanedEndTime()));
            return genMsgObj(USER_BANED,banStr);// 用户被封禁了
        }

        otherUserInfoJo.put(FIELD_U_LIVED, true);// 曾经是否直播过的标示
        if (otherUserInfo.getHistoryTotalGuestCount() == null || otherUserInfo.getHistoryTotalGuestCount() == 0) {
            otherUserInfoJo.put(FIELD_U_LIVED, false);
        }

        Anchor anchor = anchorService.findByUid(otherUid);
        int anchorType = anchor == null ? 0 : anchor.getRecommandType();
        logger.info("uid:{} anchor:{} anchor type is :{}", uid, anchor, anchorType);
        otherUserInfoJo.put(RET_U_ANCHOR_TYPE, anchorType);// 主播类型

        boolean idVerified = false;
        if (uid == otherUid) {
            try {
                PaymentAccount account = paymentAccountMapper.findById(uid);
                if (account != null) {
                    idVerified = true;
                }
            } catch (Exception e) {
                logger.error("Get user payment account error.", e);
            }
        }
        otherUserInfoJo.put(RET_U_IDENTITY_VERIFIED, idVerified);

        return genMsgObj(SUCCESS,null, otherUserInfoJo);
    }

    public String genUsername(Long uid) {
        long start = MaskClock.getCurtime();
        String username = null;
        int count = 0;
        try {
            boolean exist = true;
            do {
                username = genRankUsername();
                exist = userHessianService.getClient().checkUserExistByUsername(username);
                count++;
            } while (exist);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            username = genRankUsername();
            count = 0;
        } finally {
            metricsClient.report(ProtocolType.HTTP, progress, this.getClass(), "genUsername", MaskClock.getCurtime() - start, count + 1);
        }
        return username;
    }

    /**
     * @param uid
     * @param otherUserInfo
     * @return
     * @throws Exception
     */
    public ObjectNode fillUserStat2(long uid, UserInfo otherUserInfo) throws Exception {
        try {
            ObjectNode data = JsonUtil.createDefaultMapper().convertValue(otherUserInfo,ObjectNode.class);
            if (uid != otherUserInfo.getUid()) {
                // 不暴露用户的信息
                data.remove(FIELD_SERVER);
                data.remove(FIELD_U_USER_SOURCE);
                data.remove(FIELD_U_MY_LOCALE);
                data.remove(FIELD_U_CLIENT_TYPE);
                data.remove(FIELD_U_CLIENT_VER);
                data.remove(FIELD_U_REG_PUSH_ID);
            }

            return data;
        } catch (Exception e) {
            throw e;
        }
    }

    private final static String opAddOrUpdate = "addorupdate";

    public void addOrUpdate(UserInfo userInfo, String clientType, String clientVer, String pushId, String channel, HttpServletRequest request, HttpServletResponse response) {
        long start = MaskClock.getCurtime();
        UserInfo dbObj = null;
        boolean shouldSend2Mms = false;
        boolean sendNick2Mms = false;
        boolean sendSignature2Mms = false;
        LsRequestHeader header = LiveShowServletUtil.genHeader(request);
        header = LsHeaderChecker.checkHeader(header);
        try {
            if (userInfo.getUid() == null) {
                sendResponse(request, response, genMsgObj(FAILED, "Uid is Null!"));
                metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 2);
                return;
            }
            if (StringUtils.isNotBlank(userInfo.getUsername()) && !legalUsername(userInfo.getUsername())) {// username非null且不合法（产品意义上的用户ID），不能更新
                sendResponse(request, response, genMsgObj(USERNAME_NOT_LEGAL, "Username (" + userInfo.getUsername() + ") Not Legal!"));
                metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 3);
                return;
            }
            if (StringUtils.isNotBlank(userInfo.getUsername())
                    && (ReserveIdsUtil.goodUsername(userInfo.getUsername()) || userHessianService.getClient().getUserByUsername(
                    userInfo.getUsername()) != null)) {
                logger.warn("Username ({}) exist", userInfo.getUsername());
                sendResponse(request, response, genMsgObj(USERNAME_EXISTED, "Username (" + userInfo.getUsername() + ") Exist!"));
                if (userHessianService.getClient().getUserByUid(userInfo.getUid(),false) == null) {// 新用户注册
                    metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 4);
                } else {
                    metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 7);
                }
                return;
            }

            String rawNick = userInfo.getNick();

            if (userInfo.getHeaderUrl() != null && !userInfo.getHeaderUrl().startsWith(Bs2Service.DOWNLOAD_SMALL_AND_CDN_HOST)) {
                 userInfo.setHeaderUrl(null);// 不是我方的图片，不能设置为头像
            }
            if (userInfo.getNick() != null) {
                userInfo.setNick(censorWordService.verifyCensorText(userInfo.getNick().trim()));
            }
            if (userInfo.getSignature() != null) {
                if (StringUtils.isBlank(userInfo.getSignature())) {// 简介可以设置为空格
                    userInfo.setSignature("");
                } else {
                    userInfo.setSignature(censorWordService.verifyCensorText(userInfo.getSignature()));
                }
            }
            if (userInfo.getSex() != null && userInfo.getSex() == 0) {
                userInfo.setSex(null);
            }
            boolean firstTime = false;
            dbObj =  userHessianService.getClient().getUserByUid(userInfo.getUid(),false);
            userInfo.setNewUser(true);// 是3.0的用户
            if (dbObj == null) {
                if (StringUtils.isBlank(userInfo.getNick())) {
                    sendResponse(request, response, genMsgObj(FAILED, "Nick is Empty!"));
                    metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 5);
                    return;
                }

                userInfo.setUserSource(UserSource.findUserSource(userInfo.getUserSource()).getValue());
                if (userInfo.getSex() == null || userInfo.getSex() == 0) {// 一次都没有改过
                    userInfo.setSex(Sex.FEMALE.getValue());// 新用户默认是女的
                    userInfo.setChangeSexCount(0);
                } else if (userInfo.getUserSource() != UserSource.PHONE.getValue()) {
                    userInfo.setChangeSexCount(0);
                } else {
                    userInfo.setChangeSexCount(2);
                }
                userInfo.setVerified(false);
                userInfo.setCreateTime(new Date());
                userInfo.setUpdateTime(new Date());
                //userInfo.setFeedCount(0L);
                //userInfo.setLikeCount(0L);
                userInfo.setChangeNameCount(0);
                userInfo.setBaned(null);
                userInfo.setAnchorType(AnchorRecommendType.NormalAnchor.getType());// 用户一进来都是新用户
                userInfo.setMyLocale(getLocaleStr(request));
                if (StringUtils.isBlank(userInfo.getUsername())) {// username为空则自动分配
                    userInfo.setUsername(genRankUsername());
                }
                if (StringUtils.isBlank(userInfo.getNick())) {// nick也为空则自动使用username值
                    userInfo.setNick(userInfo.getUsername());
                }
                String nick = ContentMatcher.replaceAllPatternChars(userInfo.getNick());
                userInfo.setNick(ContentMatcher.replaceAllPatternCharsExceptStar(userInfo.getNick()));// 昵称不能屏蔽星星
                userInfo.setPinyinNick(PinyinHelper.convertToPinyinString(nick, "", PinyinFormat.WITHOUT_TONE));
                userInfo.setShortPyNick(PinyinHelper.getShortPinyin(nick));

                if (userInfo.getHeaderUrl() != null) {
                    shouldSend2Mms = true;
                } else {// 没有头像就采用默认头像
                    userInfo.setHeaderUrl(DEFAULT_PIC);
                }
                if (StringUtils.isNotBlank(userInfo.getNick())) {
                    sendNick2Mms = true;
                }
                if (StringUtils.isNotBlank(userInfo.getSignature())) {
                    sendSignature2Mms = true;
                }
                if (StringUtils.isBlank(pushId)) {// 尽可能补上PushId
                    List<com.yy.me.message.thrift.push.UserInfo> user = messageMapper.findUserDevices(userInfo.getUid());
                    if (user != null && !user.isEmpty()) {
                        pushId = user.get(0).getPushId();
                    }
                }
                if (userInfo.getNick() != null && userInfo.getNick().equals("")) {
                    userInfo.setNick("佚名");
                }
                userHessianService.getClient().insert(userInfo, clientType, clientVer, pushId, channel);
                statisticsLogService.logUserRegist(userInfo, clientType, clientVer, pushId, channel);

                String transId = GeneralUtil.findTransId(header);
                LsAction lsAction = LsAction.start(transId, StatisticsType.USER_REGIST, messageService.genPartialOrder()).initByHeader(header)
                        .setActUid(userInfo.getUid()).setUpdateUsername(userInfo.getUsername()).setUpdateSex(userInfo.getSex())
                        .setUpdateNick(userInfo.getNick()).setUpdateHeaderUrl(userInfo.getHeaderUrl()).setUpdateSignature(userInfo.getSignature())
                        .setUpdateUserSource(userInfo.getUserSource());
                lsBroadcastAloProducer.sendLsActMsg(lsAction);

                firstTime = true;
            } else {
                if (dbObj.getBaned() instanceof Boolean && dbObj.getBaned()) {
                    String banStr = messageMongoDBMapper.fm(dbObj.getBanedType(),
                            MessageService.dateFormatter.format(dbObj.getBanedEndTime()));
                    sendResponse(request, response, genMsgObj(USER_BANED, banStr)); // 用户被封禁了
                    return;
                }
                if (userInfo.getNick() != null && StringUtils.isBlank(userInfo.getNick())) {// 需要更新昵称（非null），但发现昵称为空
                    sendResponse(request, response, genMsgObj(FAILED, "Nick is Empty!"));
                    return;
                }
                userInfo.set_ttserver(null);
                userInfo.setUserSource(null);
                userInfo.setVerified(null);
                userInfo.setCreateTime(null);
                userInfo.setUpdateTime(new Date());
                //userInfo.setFeedCount(null);
                //userInfo.setLikeCount(null);
                userInfo.setBaned(null);
                userInfo.setChangeNameCount(null);
                userInfo.setChangeSexCount(null);
                if (userInfo.getUsername() != null) {
                    if (dbObj.getChangeNameCount() > 1) {
                        userInfo.setUsername(null);// 不允许超过1次更新username
                    } else {// 相同的话，也算是更新
                        // 允许更新username，且更新次数+1
                        userInfo.setChangeNameCount(dbObj.getChangeNameCount() + 1);
                    }
                }
                if (userInfo.getSex() != null) {
                    if (dbObj.getChangeSexCount() == null) {
                        dbObj.setChangeSexCount(0);
                    }
                    if (dbObj.getChangeSexCount() > 1) {
                        userInfo.setSex(null);// 不允许超过1次更新sex
                    } else if (dbObj.getSex().equals(userInfo.getSex())) {// 相同的话，不算是更新
                        userInfo.setSex(null);
                    } else {
                        // 允许更新sex，且更新次数+1
                        userInfo.setChangeSexCount(dbObj.getChangeSexCount() + 1);
                    }
                }
                if (StringUtils.isNotBlank(userInfo.getNick()) || StringUtils.isNotBlank(userInfo.getUsername())) {
                    // 如果是实名用户，在改昵称或ID后需要重新审核！
                    if (dbObj.getVerified()) {
                        userInfo.setVerified(false);
                        userInfo.setNick(rawNick);// 认证用户可以不用经过关键字过滤地更改昵称1次
                        userHessianService.getClient().updateVerifyApplicationStatus(userInfo.getUid(), VerifyApplicationStatus.WAIT.getValue(), null);
                    }
                }
                if (StringUtils.isNotBlank(userInfo.getNick())) {
                    String nick = ContentMatcher.replaceAllPatternChars(userInfo.getNick());
                    userInfo.setNick(ContentMatcher.replaceAllPatternCharsExceptStar(userInfo.getNick()));
                    userInfo.setPinyinNick(PinyinHelper.convertToPinyinString(nick, "", PinyinFormat.WITHOUT_TONE).toLowerCase());
                    userInfo.setShortPyNick(PinyinHelper.getShortPinyin(nick).toLowerCase());
                }
                if (StringUtils.isNotBlank(userInfo.getNick())) {
                    sendNick2Mms = true;
                }
                if (StringUtils.isNotBlank(userInfo.getSignature())) {
                    sendSignature2Mms = true;
                }
                if (userInfo.getHeaderUrl() != null) {
                    shouldSend2Mms = true;
                }
                if (userInfo.getNick() != null && userInfo.getNick().equals("")) {
                    userInfo.setNick("佚名");
                }

                userInfo = userHessianService.getClient().findAndModifyByUid(userInfo.getUid(), userInfo);
                if (StringUtils.isNotBlank(userInfo.getHeaderUrl())) {
                    statisticsLogService.logUserChangeHeader(userInfo);
                }

            }

            ObjectNode jo = JsonUtil.createDefaultMapper().convertValue(userInfo,ObjectNode.class);
            jo.put(RET_U_FIRSTTIME, firstTime);// TODO 可干掉
            sendResponse(request, response, genMsgObj(SUCCESS, null,jo));
            metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 6);
        }
        try {
            userHessianService.getClient().insertHiidoUserUpdate(userInfo.getUid());
            hiddoSearch.addOrUpdateUserInfo(userInfo, 0, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        if (shouldSend2Mms) {
            try {
                // 图片送审
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(MMS_TYPE, MmsType.USER_HEAD.getValue());
                jo.put(FIELD_U_UID, userInfo.getUid());
                if (dbObj != null) {
                    jo.put(MMS_PIC_U_OLD_HEADER_URL, dbObj.getHeaderUrl());
                }
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushImgReport(userInfo.getUid(), userInfo.getHeaderUrl(), JsonUtil.instance.toJson(jo));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (sendNick2Mms) {
            try {
                // 昵称送审
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(MMS_TYPE, MmsType.USER_NICK.getValue());
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushTxtReport(userInfo, JsonUtil.instance.toJson(jo), MmsType.USER_NICK);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (sendSignature2Mms) {
            try {
                // 昵称送审
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(MMS_TYPE, MmsType.USER_SIGNATURE.getValue());
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushTxtReport(userInfo, JsonUtil.instance.toJson(jo), MmsType.USER_SIGNATURE);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
