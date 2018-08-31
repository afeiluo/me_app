package com.yy.me.web.service;

import static com.yy.me.http.BaseServletUtil.*;
import static com.yy.me.liveshow.client.entity.LiveShowSafe.Fields.*;
import static com.yy.me.mongo.MongoUtil.*;
import static com.yy.me.service.inner.ServiceConst.*;
import static com.yy.me.user.UserInfo.Fields.*;
import static com.yy.me.user.UserInfoUtil.genRankUsername;
import static com.yy.me.user.UserInfoUtil.legalUsername;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yy.me.anchor.family.entity.Anchor;
import com.yy.me.contact.ContactHessianService;
import com.yy.me.contact.entity.ContactCode;
import com.yy.me.dao.MedalMongoDBMapper;
import com.yy.me.entity.AnchorAuth;
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
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yy.cs.base.redis.RedisClientFactory;
import com.yy.cs.center.ReferenceFactory;
import com.yy.me.anchor.family.entity.AnchorRecommendType;
import com.yy.me.bs2.Bs2Service;
import com.yy.me.config.CntConfService;
import com.yy.me.dao.AnchorAuthMongoDBMapper;
import com.yy.me.dao.HeaderReviewMongoDBMapper;
import com.yy.me.dao.PaymentAccountMongoDBMapper;
import com.yy.me.entity.PaymentAccount;
import com.yy.me.enums.BubbleMsgType;
import com.yy.me.enums.MmsType;
import com.yy.me.friend.entity.FriendRelation;
import com.yy.me.http.BaseServletUtil;
import com.yy.me.http.BaseServletUtil.RetMsgObj;
import com.yy.me.http.HttpUtil;
import com.yy.me.lbs.DistIpUtil;
import com.yy.me.liveshow.client.cache.GeneralLiveShowClient;
import com.yy.me.liveshow.client.entity.LiveShowDto;
import com.yy.me.liveshow.client.entity.LsAction;
import com.yy.me.liveshow.client.mq.LsBroadcastAloProducer;
import com.yy.me.liveshow.client.util.LiveShowServletUtil;
import com.yy.me.liveshow.client.util.LsHeaderChecker;
import com.yy.me.liveshow.enums.StatisticsType;
import com.yy.me.liveshow.pb.Lsdto.LiveShowDtoPb;
import com.yy.me.liveshow.thrift.LiveShowThriftService;
import com.yy.me.liveshow.thrift.LsRequestHeader;
import com.yy.me.liveshow.thrift.LsResponse;
import com.yy.me.liveshow.thrift.ResponseCode;
import com.yy.me.liveshow.util.GeneralUtil;
import com.yy.me.message.MessageMongoDBMapper;
import com.yy.me.message.MsgDataType;
import com.yy.me.metrics.MetricsClient;
import com.yy.me.metrics.MetricsClient.ProtocolType;
import com.yy.me.service.LiveShowService;
import com.yy.me.service.inner.FillService;
import com.yy.me.service.inner.MessageService;
import com.yy.me.service.inner.StatisticsLogService;
import com.yy.me.thread.ThreadUtil;
import com.yy.me.time.DateTimeUtil;
import com.yy.me.time.MaskClock;
import com.yy.me.user.Sex;
import com.yy.me.user.UserHessianService;
import com.yy.me.user.UserInfo;
import com.yy.me.user.UserInfoUtil;
import com.yy.me.user.UserSource;
import com.yy.me.user.VerifyApplicationStatus;
import com.yy.me.user.entity.InterestNode;
import com.yy.me.user.entity.MeetInfo;
import com.yy.me.user.entity.MultiInfo;
import com.yy.me.util.ContentMatcher;
import com.yy.me.util.search.HiddoSearch;
import com.yy.me.web.dao.CheckUserInfoMongoDBMapper;
import com.yy.me.web.dao.ReportMongoDBMapper;
import com.yy.me.web.util.ReserveIdsUtil;

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
    private LiveShowService liveShowService;

    @Autowired
    private CntConfService cntConfService;

    @Autowired
    private RedisClientFactory cntRedisFactory;

    @Autowired
    @Qualifier("liveShowThriftService")
    private ReferenceFactory<LiveShowThriftService> liveShowThriftService;

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoTemplate mongoTemplate;

    @Autowired
    @Qualifier("mongoTemplateUser")
    private MongoTemplate userMongoTemplate;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReportMongoDBMapper reportMongoDBMapper;

    @Autowired
    private MmsReportService mmsReportService;// 图片送审

    @Autowired
    private CensorWordWebService censorWordService;

    @Autowired
    private MessageMongoDBMapper messageMongoDBMapper;

    @Autowired
    private StatisticsLogService statisticsLogService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private CheckUserInfoMongoDBMapper checkUserInfoMongoDBMapper;

    @Autowired
    private PaymentAccountMongoDBMapper paymentAccountMapper;
    @Autowired
    private AnchorAuthMongoDBMapper anchorAuthMongoDBMapper;
    @Autowired
    private HeaderReviewMongoDBMapper headerReviewMongoDBMapper;
    @Autowired
    private MedalMongoDBMapper medalMongoDBMapper;

    @Autowired
    @Qualifier("userHessianService")
    private ReferenceFactory<UserHessianService> userHessianService;

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<com.yy.me.friend.FriendService> userFriendService;

    @Autowired
    private AnchorRpcService anchorService;

    @Autowired
    private LsBroadcastAloProducer lsBroadcastAloProducer;

    @Autowired
    @Qualifier("friendServiceHessianClient")
    private ReferenceFactory<com.yy.me.friend.FriendService> friendHessianFactory;

    private com.yy.me.friend.FriendService getFriendClient() {
        return friendHessianFactory.getClient();
    }

    @Autowired
    @Qualifier("geoThriftService")
    private ReferenceFactory<com.yy.me.geo.thrift.GeoService> geoThriftService;

    @Autowired
    @Qualifier("contactHessianService")
    private ReferenceFactory<ContactHessianService> contactHessianService;

    private static final int bindLimitCount = 3;// 第三方电话绑定限定次数

    private static final int MAX_LIKE = 20;// 资料不完整的情况下最多like的次数
    private static final int MAX_LIKE_DAY = 3;// 资料不完整的情况下 打到like上限后每日可以like的上限
    /**
     * 记录谁看了谁 24小时
     */
    private Cache<String, Long> visitCache = CacheBuilder.newBuilder().maximumSize(200000).expireAfterWrite(24, TimeUnit.HOURS).build();

    private ThreadFactory threadFactory = ThreadUtil.buildThreadFactory("userGet-executor", true);
    private ExecutorService executorService = new ThreadUtil.CachedThreadPoolBuilder().setThreadFactory(threadFactory).setMaxSize(500).build();
    private ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(executorService);

    public void checkAndFetchMsg(long uid, HttpServletRequest request, HttpServletResponse response) {
        UserInfo userInfo = getUserInfo(request);

        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, messageService.checkoutMyRecentlyUserMsg(userInfo, null, null, 20)));
        } catch (Exception e) {
            logger.error("Fetch message error.", e);

            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void fetchCtrlMsg(String appId, long uid, String pushId, String clientStr, String clientVer, HttpServletRequest request,
            HttpServletResponse response) {
        // move to controller layer
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            UserInfo userInfo = getUserInfo(request);// 从req 属性中获取

            sendResponse(request, response,
                    genMsgObj(SUCCESS, null, messageService.checkoutMyRecentlyCtrlMsg(appId, userInfo, pushId, clientStr, clientVer)));
        } catch (Exception e) {
            logger.error(
                    String.format("fetchCtrlMsg fail appId:%s, uid:%s, clientStr:%s, clientVer%s, errmsg:%s ", appId, uid, clientStr, clientVer,
                            e.getMessage()), e);

            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "fetchCtrlMsg", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void deleteCtrlMsg(String msgDataType, long uid, String clientStr, String clientVer, HttpServletRequest request,
            HttpServletResponse response) {
        // move to controller layer
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            UserInfo userInfo = getUserInfo(request);// 从req 属性中获取
            if ("s_op_medal_add".equals(msgDataType) || "s_op_header_invalid".equals(msgDataType)
                    || MsgDataType.S_REPORT_WARN.getValue().equals(msgDataType) || MsgDataType.S_HEAD_PUNISH.getValue().equals(msgDataType)
                    || MsgDataType.S_NICK_PUNISH.getValue().equals(msgDataType) || MsgDataType.S_SIGNATURE_PUNISH.getValue().equals(msgDataType)) {
                messageService.removeCtrlMsg(userInfo, msgDataType);
            }
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error(String.format("deleteCtrlMsg fail msgDataType:%s, uid:%s, clientStr:%s, clientVer%s, errmsg:%s ", msgDataType, uid,
                    clientStr, clientVer, e.getMessage()), e);

            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "deleteCtrlMsg", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void findUserMsg(long uid, String pushId, String lastMsgId, int limit, HttpServletRequest request, HttpServletResponse response) {
        UserInfo userInfo = getUserInfo(request);
        try {
            sendResponse(request, response, genMsgObj(SUCCESS, null, messageService.checkoutMyRecentlyUserMsg(userInfo, pushId, lastMsgId, limit)));
        } catch (Exception e) {
            logger.error("Fetch user message error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    /**
     * 个人资料页
     * 
     * @param uid
     * @param otherUid
     * @return
     * @throws Exception
     */
    public void find(Long uid, final Long otherUid, Long lastRequest, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            final UserInfo otherUserInfo = userHessianService.getClient().getUserByUid(otherUid, true);
            if (otherUserInfo == null) {
                ObjectNode jo = getLocalObjMapper().createObjectNode();
                jo.put(FIELD_U_USERNAME, genRankUsername());
                sendResponse(request, response, genMsgObj(USER_NOT_EXIST, "Uid " + otherUid + " not Exist!", jo)); // 用户不存在
                return;
            }
            Long userStatus=otherUserInfo.getUserStatus();
            if(userStatus!=null&&(userStatus&1)!=0){
                sendResponse(request, response, genMsgObj(USER_HEADER_INVALID, "header invalid")); // otherUid头像不合格
                return;
            }
            final ObjectNode data = (ObjectNode) BaseServletUtil.getLocalObjMapper().valueToTree(otherUserInfo);
            data.remove(FIELD_SERVER);
            data.remove(FIELD_U_USER_SOURCE);
            data.remove(FIELD_U_MY_LOCALE);
            data.remove(FIELD_U_CLIENT_TYPE);
            data.remove(FIELD_U_CLIENT_VER);
            data.remove(FIELD_U_REG_PUSH_ID);
            data.remove(FIELD_U_LONGITUDE);
            data.remove(FIELD_U_LATITUDE);
            data.remove(FIELD_U_THIRD_PARTY_ID);
            data.remove(FIELD_U_CREATETIME);
            data.remove(FIELD_U_UPDATETIME);

            int latchCount = 0;
            ListenableFuture<Map<String, Object>> userInfoFuture = null;
            ListenableFuture<Map<String, Object>> relationFuture = null;
            ListenableFuture<Boolean> blockFuture = null;
            ListenableFuture<Boolean> userAnchorAuthFuture = null;
            ListenableFuture<Long> friendCountFuture = null;
            ListenableFuture<Long[]> idosFansFuture = null;

            userInfoFuture = listeningExecutorService.submit(new UserInfoGetter(uid, otherUserInfo.getUid(), true));
            latchCount++;
            if (!uid.equals(otherUserInfo.getUid())) {
                relationFuture = listeningExecutorService.submit(new UserRelationGetter(uid, otherUid));
                latchCount++;
                blockFuture = listeningExecutorService.submit(new UserBlockChecker(uid, otherUid));
                latchCount++;
                LiveShowDto liveshow = GeneralLiveShowClient.getLsByUid(otherUid);
                if (liveshow != null) {
                    data.put(RET_LS_LID, liveshow.getLid());
                    data.put(FIELD_U_STARTINGNOW, true);
                }
                LsRequestHeader header = LiveShowServletUtil.genHeader(request);
                // 查询otherUid是否在某一个直播间里面 主播的 uid lid
                LsResponse lsResp = liveShowThriftService.getClient().findUserInLs(header, otherUid);
                if (lsResp.getCode() == ResponseCode.SUCCESS) {
                    byte[] tmp = lsResp.getData();
                    LiveShowDtoPb liveShowDtoPb = LiveShowDtoPb.parseFrom(tmp);
                    data.put(RET_LS_LID, liveShowDtoPb.getLid());
                    data.put(FIELD_U_ANCHORUID, liveShowDtoPb.getUid());
                    data.put(FIELD_U_WATCHINGNOW, true);// 表示正在lid的直播间里面看直播
                }
            } else {// 查看自己
                // data.remove(FIELD_U_YEAR);
                data.put(FIELD_U_INTEGRITY, getIntegrity(otherUserInfo));
                latchCount++;
                userAnchorAuthFuture = listeningExecutorService.submit(new UserAnchorAuthGetter(uid));
                latchCount++;
                friendCountFuture = listeningExecutorService.submit(new UserFriendCountGetter(uid));
                // 增加是否展示"关注和粉丝"的入口
                if (showFansEntrance(otherUserInfo.getCreateTime())) {
                    data.put(FIELD_U_SHOWFANSENTRANCE, true);
                    // 增加粉丝总数和关注的人的总数 fansCount idolCount
                    latchCount++;
                    idosFansFuture = listeningExecutorService.submit(new IdolsFansGetter(uid));
                } else {
                    data.put(FIELD_U_SHOWFANSENTRANCE, false);
                }

                // 增加是否显示"营收入口"入口菜单
                data.put(FIELD_U_SHOW_CASHENTRANCE, showCashEntrance(otherUserInfo.getCreateTime())); // 收益入口
                data.put(FIELD_U_SHOW_PAYENTRANCE, showPayEntrance(otherUserInfo.getCreateTime())); // 充值入口

                if (showAttendSheet(uid, otherUserInfo)) {// 展示 "考勤入口" 标示
                    data.put(FIELD_U_SHOW_ATTEDSHEET, true);
                    data.put(FIELD_U_ATTEDSHEEETURL, cntConfService.findAttendLink());
                } else {
                    data.put(FIELD_U_SHOW_ATTEDSHEET, false);
                    data.put(FIELD_U_ATTEDSHEEETURL, "");
                }

                if (showArrangeEntrance(uid, otherUserInfo)) {
                    data.put(FIELD_U_SHOW_ARRANGEENTRANCE, true);
                    data.put(FIELD_U_ARRANGEENTRANCE_URL, cntConfService.getArrangeLink());// 考勤入口的地址
                } else {
                    data.put(FIELD_U_SHOW_ARRANGEENTRANCE, false);
                    data.put(FIELD_U_ARRANGEENTRANCE_URL, "");
                }
                if (otherUserInfo.getNewUser() == null) {
                    data.put(RET_U_NEWUSER, false);// 不是3.0版本以后的用户
                }
            }
            data.put("extra", cntConfService.getPortraitDes());
            CountDownLatch latch = new CountDownLatch(latchCount);
            dealWithFutureCallback(uid, latch, data, otherUserInfo, userInfoFuture, relationFuture, blockFuture, userAnchorAuthFuture,
                    friendCountFuture, idosFansFuture);
            latch.await();
            logger.info("[find-finally] cost: " + (System.currentTimeMillis() - t) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS, null, data));
        } catch (Exception e) {
            logger.error("Find user error.", e);

            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
            return;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "findNew", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    /**
     * 处理线程的回调
     * 
     * @param latch
     * @param data
     * @param otherUid
     * @param otherUserInfo
     */
    private void dealWithFutureCallback(final Long uid, final CountDownLatch latch, final ObjectNode data, final UserInfo otherUserInfo,
            ListenableFuture<Map<String, Object>> userInfoFuture, ListenableFuture<Map<String, Object>> relationFuture,
            ListenableFuture<Boolean> blockFuture, ListenableFuture<Boolean> userAnchorAuthFuture, ListenableFuture<Long> friendCountFuture,
            ListenableFuture<Long[]> idosFansFuture) {
        if (userInfoFuture != null) {
            Futures.addCallback(userInfoFuture, new FutureCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    if (!uid.equals(otherUserInfo.getUid())) {// 查看别人的信息的时候需要查询自己的一部分信息
                        UserInfo userInfo = (UserInfo) result.get("user");
                        new Thread(new OtherExtraRunner(userInfo, otherUserInfo.getUid())).start();
                        data.put(FIELD_U_DISTANCE, getDistance(userInfo, otherUserInfo));
                        result.remove("user");
                    }
                    data.putAll((ObjectNode) BaseServletUtil.getLocalObjMapper().valueToTree(result));
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("get userInfo error", t);
                    latch.countDown();
                }
            });
        }
        if (blockFuture != null) {
            Futures.addCallback(blockFuture, new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    data.put(FIELD_U_BLOCKED, result);
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("check blockUser error", t);
                    latch.countDown();
                }
            });
        }

        if (relationFuture != null) {
            Futures.addCallback(relationFuture, new FutureCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    data.putAll((ObjectNode) BaseServletUtil.getLocalObjMapper().valueToTree(result));
                    // data.put(FIELD_U_ISFRIEND, (Integer) result.get(FIELD_U_ISFRIEND));
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("check userRelation error", t);
                    latch.countDown();
                }
            });
        }

        if (userAnchorAuthFuture != null) {
            Futures.addCallback(userAnchorAuthFuture, new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (result != null) {
                        data.put(RET_U_IDENTITY_VERIFIED, result);
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("check user friend count error", t);
                    latch.countDown();
                }
            });
        }
        if (friendCountFuture != null) {
            Futures.addCallback(friendCountFuture, new FutureCallback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    data.put(FIELD_U_FRIENDS, result);
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("get friend count request error", t);
                    latch.countDown();

                }
            });
        }
        if (idosFansFuture != null) {
            Futures.addCallback(idosFansFuture, new FutureCallback<Long[]>() {
                @Override
                public void onSuccess(Long[] result) {
                    if (result != null && result.length == 2) {
                        data.put(FIELD_U_FANSCOUNT, result[1]);
                        data.put(FIELD_U_IDOLCOUNT, result[0]);
                    }
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("get idols fans error", t);
                    latch.countDown();
                }
            });
        }
    }

    public UserInfo getUser(long uid) {
        return userHessianService.getClient().getUserByUid(uid, true);
    }

    private class UserInfoGetter implements Callable<Map<String, Object>> {
        private Long uid;
        private Long otherUid;
        private Boolean useCache;

        public UserInfoGetter(Long uid, Long otherUid, Boolean useCache) {
            this.uid = uid;
            this.useCache = useCache;
            this.otherUid = otherUid;
        }

        @Override
        public Map<String, Object> call() throws Exception {
            Map<String, Object> retMap = Maps.newHashMap();
            long start = System.currentTimeMillis();
            if (!uid.equals(otherUid)) {
                UserInfo userInfo = userHessianService.getClient().getUserByUid(uid, useCache);
                logger.debug("[find-UserInfoGetter-selfInfo] cost: " + (System.currentTimeMillis() - start) + " ms");
                start = System.currentTimeMillis();
                retMap.put("user", userInfo);

            }
            // List<MultiInfo> multiInfos = userHessianService.getClient().getUserMultiInfo(otherUid);// 头像信息
            // if (multiInfos != null && !multiInfos.isEmpty()) {
            // retMap.put(FIELD_U_MULTIINFO, BaseServletUtil.getLocalObjMapper().valueToTree(multiInfos));
            // }
            // logger.info("[find-UserInfoGetter-multiInfos] cost: " + (System.currentTimeMillis() - start) + " ms");
            // start = System.currentTimeMillis();
            // List<InterestNode> interests = userHessianService.getClient().getUserInterest(otherUid);// 兴趣标签
            // if (interests != null && !interests.isEmpty()) {
            // retMap.put(FIELD_U_INTERESTS, BaseServletUtil.getLocalObjMapper().valueToTree(interests));
            // }
            // logger.info("[find-UserInfoGetter-interests] cost: " + (System.currentTimeMillis() - start) + " ms");
            return retMap;
        }
    }

    private class UserAnchorAuthGetter implements Callable<Boolean> {
        private Long uid;

        public UserAnchorAuthGetter(Long uid) {
            this.uid = uid;
        }

        @Override
        public Boolean call() throws Exception {
            long start = System.currentTimeMillis();
            Anchor anchor = anchorService.findByUid(uid);
            int anchorType = anchor == null ? 0 : anchor.getRecommandType();
            if (anchorType == AnchorRecommendType.SignAnchor.getType()) {
                return true;
            } else {
                AnchorAuth anchorAuth = anchorAuthMongoDBMapper.find(uid);
                if (anchorAuth != null && anchorAuth.getStatus() == 2) {
                    return true;
                }
            }
            logger.debug("[find-UserAnchorAuthGetter] cost: " + (System.currentTimeMillis() - start) + " ms");
            return false;
        }
    }

    private class IdolsFansGetter implements Callable<Long[]> {
        private Long uid;

        public IdolsFansGetter(Long uid) {
            this.uid = uid;
        }

        @Override
        public Long[] call() throws Exception {
            long start = System.currentTimeMillis();
            Long[] ret = getFriendClient().getIdolFansCount(uid);
            logger.debug("[find-IdolsFansGetter] cost: " + (System.currentTimeMillis() - start) + " ms");
            return ret;
        }

    }

    private class UserFriendCountGetter implements Callable<Long> {
        private Long uid;

        public UserFriendCountGetter(Long uid) {
            this.uid = uid;
        }

        @Override
        public Long call() throws Exception {
            long start = System.currentTimeMillis();
            Long ret = getFriendClient().getFriendCount(uid);
            logger.debug("[find-UserFriendCountGetter] cost: " + (System.currentTimeMillis() - start) + " ms");
            return ret;
        }

    }

    private class UserRelationGetter implements Callable<Map<String, Object>> {
        private Long uid;
        private Long otherUid;

        public UserRelationGetter(Long uid, Long otherUid) {
            this.uid = uid;
            this.otherUid = otherUid;
        }

        @Override
        public Map<String, Object> call() throws Exception {
            Map<String, Object> retMap = Maps.newHashMap();
            long start = System.currentTimeMillis();
            int ret = getFriendClient().getUserInnerRelation(uid, otherUid);
            int liked = 0;
            if (ret == 1) {
                liked = 2;
            } else if (ret == 2) {
                liked = 4;
            }
            retMap.put(RET_U_RELATION, ret);
            retMap.put(FIELD_U_LIKED, liked);
            retMap.put(FIELD_U_ISFRIEND, ret == 2 ? 1 : 0);
            logger.debug("[find-UserRelationGetter-isFriend] cost: " + (System.currentTimeMillis() - start) + " ms");

            retMap.put(RET_U_CANSUPERLIKE, getFriendClient().isTodaySuperLike(uid,otherUid));
            // 喜欢自己的人不受like限制
            retMap.put(FIELD_U_CANLIKE, true);// (当liked为0的时候才判断这个字段)默认可以like otheruid
            start = System.currentTimeMillis();
            List<Long> likedMeUids = getFriendClient().getLikeMeUids(uid);// 喜欢自己的用户列表
            logger.debug("[find-UserRelationGetter-getLikeMeUids] cost: " + (System.currentTimeMillis() - start) + " ms");
            if (likedMeUids != null && likedMeUids.contains(otherUid))// 对方like了我那么我无条件的可以like他
            {
                return retMap;
            }
            start = System.currentTimeMillis();
            Boolean complete = userHessianService.getClient().checkUserIntegrity(uid);// 自己的资料是否完整
            logger.debug("[find-UserRelationGetter-checkUserIntegrity] cost: " + (System.currentTimeMillis() - start) + " ms");
            if (!complete) {// 资料不完整like受限制
                start = System.currentTimeMillis();
                long likedTotal = getFriendClient().getLikeCount(uid, null);// 已经like的人数
                logger.debug("[find-UserRelationGetter-getLikeCount(total)] cost: " + (System.currentTimeMillis() - start) + " ms");
                if (likedTotal >= MAX_LIKE) {
                    // 当天liked了多少人
                    start = System.currentTimeMillis();
                    long likedTody = getFriendClient().getLikeCount(uid, new Date());// 当天like了多少人
                    logger.debug("[find-UserRelationGetter-getLikeCount(today)] cost: " + (System.currentTimeMillis() - start) + " ms");
                    if (likedTody >= MAX_LIKE_DAY) {
                        retMap.put(FIELD_U_CANLIKE, false);
                    }
                }
            }
            return retMap;
        }
    }

    private class UserBlockChecker implements Callable<Boolean> {
        private Long uid;
        private Long otherUid;

        public UserBlockChecker(Long uid, Long otherUid) {
            this.uid = uid;
            this.otherUid = otherUid;
        }

        @Override
        public Boolean call() throws Exception {
            long start = System.currentTimeMillis();
            Boolean ret = getFriendClient().checkShield(uid, otherUid);
            logger.debug("[find-UserBlockChecker] cost: " + (System.currentTimeMillis() - start) + " ms");
            return ret;
        }

    }

    private class OtherExtraRunner implements Runnable {
        private UserInfo userInfo;
        private Long otherUid;

        private OtherExtraRunner(UserInfo userInfo, Long otherUid) {
            this.userInfo = userInfo;
            this.otherUid = otherUid;
        }

        @Override
        public void run() {
            try {
                checkUserInfoMongoDBMapper.insertRecord(userInfo.getUid(), otherUid);
                if (visitCache.getIfPresent(userInfo.getUid() + "_" + otherUid) == null) {
                    long mc = userFriendService.getClient().getMessage2Read(otherUid);
                    messageService.pushBubbleMsg(userInfo, otherUid, BubbleMsgType.VISIT_HOME.getType(), mc);
                    visitCache.put(userInfo.getUid() + "_" + otherUid, otherUid);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private Boolean showAttendSheet(long uid, UserInfo otherUserInfo) {
        try {
            if (uid != otherUserInfo.getUid()) {// 只在自己的个人主页里面展示 "考勤入口"
                return false;
            } else {
                return arrangeAnchorService.getAllSalaryAnchorWithCache().contains(uid);// 判断是否有底薪的主播
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    private Boolean showArrangeEntrance(long uid, UserInfo otherUserInfo) {
        if (uid != otherUserInfo.getUid()) {
            return false;
        } else {
            return arrangeAnchorService.getAllArrangeAnchorWithCache().contains(uid);
        }
    }

    private String getDistance(UserInfo userInfo, UserInfo otherUserInfo) {
        if (userInfo.getLongitude() != null && userInfo.getLatitude() != null && otherUserInfo.getLongitude() != null
                && otherUserInfo.getLatitude() != null) {
            Double distance = DistIpUtil.calDistance(userInfo.getLongitude(), userInfo.getLatitude(), otherUserInfo.getLongitude(),
                    otherUserInfo.getLatitude());
            return DistIpUtil.getDisStr(distance);
        }
        return "";
    }

    private Boolean showFansEntrance(Date date) throws Exception {
        Date dlDate = DateTimeUtil.parseDate(cntConfService.getOldVerDate());

        if (date.before(dlDate)) {
            return true;
        }
        return false;
    }

    private Boolean showCashEntrance(Date date) throws Exception {
        Boolean cashEntrance = cntConfService.fetchConfBool(CntConfService.DEFAULT_CONF_ID_PAYENTRANCES, "cashEntrance", false);
        Date dlDate = DateTimeUtil.parseDate(cntConfService.getOldVerDate());

        if (date.before(dlDate) && cashEntrance) {
            return true;
        }
        return false;
    }

    private Boolean showPayEntrance(Date date) throws Exception {
        Boolean payEntrance = cntConfService.fetchConfBool(CntConfService.DEFAULT_CONF_ID_PAYENTRANCES, "payEntrance", false);
        Date dlDate = DateTimeUtil.parseDate(cntConfService.getOldVerDate());

        if (date.before(dlDate) && payEntrance) {
            return true;
        }
        return false;
    }

    // 头像、昵称、ME号、出生年份、性别、个性签名、居住地、星座、行业、公司/学校、兴趣
    private Integer getIntegrity(UserInfo userInfo) {
        int count = 0;
        if (userInfo.getHeaderUrl() != null) {
            count++;
        }
        if (userInfo.getUsername() != null) {
            count++;
        }
        if (userInfo.getNick() != null) {
            count++;
        }
        if (userInfo.getYear() != null) {
            count++;
        }
        if (userInfo.getSex() != null) {
            count++;
        }
        if (userInfo.getSignature() != null) {
            count++;
        }
        if (userInfo.getPlaceProvince() != null && userInfo.getPlaceCity() != null) {
            count++;
        }
        if (userInfo.getConstellation() != null) {
            count++;
        }
        if (userInfo.getInterest() != null) {
            count++;
        }
        if (userInfo.getCompany() != null) {
            count++;
        }
        if (userInfo.getBusiness() != null) {
            count++;
        }
        Float ratio = (float) count / 11.0f;
        ratio = ratio * 100;
        return ratio.intValue();
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
            logger.error("Gen username error.", e);
            username = genRankUsername();
            count = 0;
        } finally {
            metricsClient.report(ProtocolType.HTTP, progress, this.getClass(), "genUsername", MaskClock.getCurtime() - start, count + 1);
        }
        return username;
    }

    public void find4Web(Long uid, Long otherUid, HttpServletRequest request, HttpServletResponse response) {

        UserInfo userInfo = null;
        try {
            userInfo = userHessianService.getClient().getUserByUid(otherUid, true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
            return;
        }
        if (userInfo == null) {
            sendResponseAuto(request, response, genMsgObj(USER_NOT_EXIST, "Uid " + otherUid + " not Exist!", Maps.newHashMap()));
            return;
        }
        try {
            ObjectNode jo = fillService.fillUserStat(uid, userInfo);

            LiveShowDto ls = GeneralLiveShowClient.getLsByUid(jo.get(RET_LS_UID).asLong());
            if (ls != null) {
                jo.put(RET_LS_STARTING_NOW, true);
            } else {
                jo.put(RET_LS_STARTING_NOW, false);
            }

        } catch (Exception e) {
            logger.error("Find user info error.", e);
            sendResponseAuto(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void checkUsername(String id, HttpServletRequest request, HttpServletResponse response) {
        UserInfo userInfo = null;
        if (!legalUsername(id)) {// 包含非法字符
            sendResponse(request, response, genMsgObj(USERNAME_NOT_LEGAL, "Username (" + id + ") Not Legal!"));
            return;
        }
        try {
            userInfo = userHessianService.getClient().getUserByUsername(id.toLowerCase());
        } catch (Exception e) {
            logger.error("Get user by username error.", e);

            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            return;
        }

        if (userInfo != null) {// 用人使用了该username
            sendResponse(request, response, genMsgObj(USERNAME_EXISTED, "Username (" + id + ") already Exist!"));
            return;
        }

        sendResponse(request, response, genMsgObj(SUCCESS));
    }

    private final static String opAddOrUpdate = "addorupdate";

    public void addOrUpdate(UserInfo userInfo, String clientType, String clientVer, String pushId, String channel, HttpServletRequest request,
            HttpServletResponse response) {
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
                if (userHessianService.getClient().getUserByUid(userInfo.getUid(), false) == null) {// 新用户注册
                    metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 4);
                } else {
                    metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 7);
                }
                return;
            }

            String rawNick = userInfo.getNick();

            if (userInfo.getHeaderUrl() != null && !userInfo.getHeaderUrl().startsWith(Bs2Service.DOWNLOAD_SMALL_AND_CDN_HOST)) {
                userInfo.setHeaderUrl(null);// 不是我方的图片，不能设置为头像，第三方登陆的头像必须转存
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
            if (userInfo.getBusiness() != null) {
                if (StringUtils.isBlank(userInfo.getBusiness())) {
                    userInfo.setBusiness("");
                } else {
                    userInfo.setBusiness(censorWordService.verifyCensorText(userInfo.getBusiness()));
                }
            }
            if (userInfo.getCompany() != null) {
                if (StringUtils.isBlank(userInfo.getCompany())) {
                    userInfo.setCompany("");
                } else {
                    userInfo.setCompany(censorWordService.verifyCensorText(userInfo.getCompany()));
                }
            }
            if (userInfo.getInterest() != null) {
                if (StringUtils.isBlank(userInfo.getInterest())) {
                    userInfo.setInterest("");
                } else {
                    userInfo.setInterest(censorWordService.verifyCensorText(userInfo.getInterest()));
                }
            }
            if (userInfo.getHomeTownCity() != null) {// 客户端传的是" "
                if (StringUtils.isBlank(userInfo.getHomeTownCity())) {
                    userInfo.setHomeTownCity("");
                }
            }
            if (userInfo.getHomeTownProvince() != null) {
                if (StringUtils.isBlank(userInfo.getHomeTownProvince())) {
                    userInfo.setHomeTownProvince("");
                }
            }
            if (userInfo.getPlaceCity() != null) {
                if (StringUtils.isBlank(userInfo.getPlaceCity())) {
                    userInfo.setPlaceCity("");
                }
            }
            if (userInfo.getPlaceProvince() != null) {
                if (StringUtils.isBlank(userInfo.getPlaceProvince())) {
                    userInfo.setPlaceProvince("");
                }
            }
            if (userInfo.getSex() != null && userInfo.getSex() == 0) {
                userInfo.setSex(null);
            }
            boolean firstTime = false;
            dbObj = userHessianService.getClient().getUserByUid(userInfo.getUid(), false);
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
                }
                userInfo.setChangeSexCount(0);
                userInfo.setVerified(false);
                userInfo.setCreateTime(new Date());
                userInfo.setUpdateTime(new Date());
                // userInfo.setFeedCount(0L);
                // userInfo.setLikeCount(0L);
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
                    userInfo.setHeaderUrl(UserInfoUtil.DEFAULT_PIC);
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
                if (dbObj.getBaned() != null && dbObj.getBaned()) {
                    String banStr = messageMongoDBMapper.fm(dbObj.getBanedType(), MessageService.dateFormatter.format(dbObj.getBanedEndTime()));
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
                // userInfo.setFeedCount(null);
                // userInfo.setLikeCount(null);
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
                        userInfo.setChangeSexCount(0);
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
                        userHessianService.getClient()
                                .updateVerifyApplicationStatus(userInfo.getUid(), VerifyApplicationStatus.WAIT.getValue(), null);
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

            ObjectNode objectNode = getLocalObjMapper().convertValue(userInfo, ObjectNode.class);
            objectNode.put(RET_U_FIRSTTIME, firstTime);

            sendResponse(request, response, genMsgObj(SUCCESS, null, objectNode));
            metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start,
                    MetricsClient.RESCODE_SUCCESS);
        } catch (Exception e) {
            logger.error("User add or update error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            metricsClient.report(MetricsClient.ProtocolType.DB, "uinfoMongoDB", opAddOrUpdate, MaskClock.getCurtime() - start, 6);
        }

        try {
            userHessianService.getClient().insertHiidoUserUpdate(userInfo.getUid());
            hiddoSearch.addOrUpdateUserInfo(userInfo, 0, null);
        } catch (Exception e) {
            logger.error("Update user info to hiddo error.", e);
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
                jo.put(FIELD_U_HEADER_URL, userInfo.getHeaderUrl());
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushImgReport(userInfo.getUid(), userInfo.getHeaderUrl(), getLocalObjMapper().writeValueAsString(jo));
            } catch (Exception e) {
                logger.error("Report user header error.", e);
            }
            try {
                if(medalMongoDBMapper.isValid(MedalMongoDBMapper.MEDAL_ID_HEADER_REVIEW)){
                    headerReviewMongoDBMapper.upsertRecord(userInfo.getUid(), userInfo.getHeaderUrl());
                }
            } catch (Exception e) {
                logger.error("headerReview upsertRecord error.uid:{}", userInfo.getUid(), e);
            }
        }
        if (sendNick2Mms) {
            try {
                // 昵称送审
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(MMS_TYPE, MmsType.USER_NICK.getValue());
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushTxtReport(userInfo, getLocalObjMapper().writeValueAsString(jo), MmsType.USER_NICK);
            } catch (Exception e) {
                logger.error("Report user nick error.", e);
            }
        }
        if (sendSignature2Mms) {
            try {
                // 签名送审
                Map<String, Object> jo = Maps.newHashMap();
                jo.put(MMS_TYPE, MmsType.USER_SIGNATURE.getValue());
                jo.put(FIELD_U_UID, userInfo.getUid());
                jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushTxtReport(userInfo, getLocalObjMapper().writeValueAsString(jo), MmsType.USER_SIGNATURE);
            } catch (Exception e) {
                logger.error("Report user signature error.", e);
            }
        }
    }

    public void report(long uid, long otherUid, String reportContent, HttpServletRequest request, HttpServletResponse response) {
        try {
            UserInfo otherUser = null;
            if (uid != otherUid && (otherUser = userHessianService.getClient().getUserByUid(otherUid, false)) != null) {
                reportMongoDBMapper.insertUser(uid, otherUid, reportContent);
                try {
                    // 图片再次送审
                    Map<String, Object> jo = Maps.newHashMap();
                    jo.put(MMS_TYPE, MmsType.USER_HEAD.getValue());
                    jo.put(FIELD_U_UID, otherUid);
                    jo.put(MMS_ACTION_TIME, System.currentTimeMillis());
                    mmsReportService.pushImgReport(otherUid, otherUser.getHeaderUrl(), getLocalObjMapper().writeValueAsString(jo));
                } catch (Exception e) {
                    logger.error("Report user header error.", e);
                }

                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }

            logger.warn("otherUid not exist, otherUid={}, uid={}", otherUid, uid);
            sendResponse(request, response, genMsgObj(FAILED));
        } catch (Exception e) {
            logger.error("Report user error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void applyVerification(long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 检查是否打开实名认证开关
            boolean verifySwitch = cntConfService.getUserVerifySwitch();

            // 检查是否已绑定实名认证
            boolean verified = false;
            if (verifySwitch) {
                PaymentAccount account = paymentAccountMapper.findById(uid);
                if (account != null) {
                    verified = true;
                }
            }

            Map<String, Object> ret = Maps.newHashMap();
            ret.put("enable", verifySwitch);
            ret.put("verified", verified);

            sendResponse(request, response, genMsgObj(SUCCESS, null, ret));
        } catch (Exception e) {
            logger.error("Apply verification error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        }
    }

    public void setInterest(long uid, List<InterestNode> interests, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            if (interests != null && !interests.isEmpty()) {
                for (InterestNode interestNode : interests) {
                    List<String> details = interestNode.getDetails();
                    if (details != null && !details.isEmpty()) {
                        for (int i = 0; i < details.size(); i++) {
                            details.set(i, censorWordService.verifyCensorText(details.get(i)));
                        }
                    }
                }
            }
            userHessianService.getClient().updateUserInterest(uid, interests);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("setInterest error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "setInterest", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void setMulti(long uid, List<MultiInfo> list, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            if (list != null && !list.isEmpty()) {
                List<String> picUrls = Lists.transform(list, new Function<MultiInfo, String>() {
                    public String apply(@Nullable MultiInfo input) {
                        return input.getPicUrl();
                    }
                });
                // 相册图片送审
                Map<String, Object> extMap = Maps.newHashMap();
                extMap.put(MMS_TYPE, MmsType.USER_MULTI_HEAD.getValue());
                extMap.put(FIELD_U_UID, uid);
                extMap.put(MMS_ACTION_TIME, System.currentTimeMillis());
                mmsReportService.pushImgReport(uid, picUrls, extMap);// 图片上报秩序组
                String videoUrl = null;
                for (MultiInfo mutiInfo : list) {
                    if (mutiInfo.getType() == 1) {// 视频
                        videoUrl = mutiInfo.getVideoUrl();
                        break;
                    }
                }
                if (StringUtils.isNotBlank(videoUrl)) {
                    // 自我介绍视频送审
                    Map<String, Object> vExtMap = Maps.newHashMap();
                    vExtMap.put(MMS_TYPE, MmsType.USER_VIDEO.getValue());
                    vExtMap.put(FIELD_U_UID, uid);
                    vExtMap.put(MMS_ACTION_TIME, System.currentTimeMillis());
                    vExtMap.put(MMS_VIDEO_URL, videoUrl);
                    mmsReportService.pushVideoReport(uid, videoUrl, vExtMap);// 视频送审
                }
            }
            userHessianService.getClient().updateUserMultiInfo(uid, list);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            logger.error("setMulti error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "setMulti", 1, MaskClock.getCurtime() - t, rescode);
        }

    }

    public void reportLocation(final long uid, Double longitude, Double latitude, Boolean firstTime, String provice, String city, final String lid,
            HttpServletRequest request, HttpServletResponse response) {
        logger.info("[reportLocation] uid:{} longitude:{} latitude:{} firstTime:{} provice:{} city:{} lid:{}", uid, longitude, latitude, firstTime,
                provice, city, lid);
        long t = MaskClock.getCurtime();
        long cur = t;
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            String ip = HttpUtil.getRemoteIP(request);
            Map<String, Object> node = Maps.newHashMap();
            node.put("nextReport", cntConfService.findRefreshDisGap());
            node.put("longitude", longitude);
            node.put("latitude", latitude);
            LsRequestHeader header = LiveShowServletUtil.genHeader(request);
            if (!DistIpUtil.judgeCoordinates(new Double[] { longitude, latitude })) {
                // 根据ip解析出经纬度
                List<Double> longlatiFromIpRet = geoThriftService.getClient().findGeoCoordinates(uid, null, ip);
                if (longlatiFromIpRet != null && longlatiFromIpRet.size() == 2) {
                    longitude = longlatiFromIpRet.get(0);
                    latitude = longlatiFromIpRet.get(1);
                    // 上报晓龙的服务
                    userHessianService.getClient().updateLongitudeAndLatitude(uid, longitude, latitude);
                }
                node.put("longitude", longitude);
                node.put("latitude", latitude);
                sendResponse(request, response, genMsgObj(SUCCESS, null, node));// 当经纬度不合法的时候直接返回
                pushUpdateMesage(uid, lid, longitude, latitude);
                return;
            }
            // 上报晓龙的服务
            userHessianService.getClient().updateLongitudeAndLatitude(uid, longitude, latitude);
            logger.info("[reportLocation-1] cost:" + (System.currentTimeMillis() - cur) + " ms");
            // 如果在用户在开播的话将地理位置信息更新到liveshow中去(定时调用的)
            LiveShowDto liveshow = GeneralLiveShowClient.getLsByUid(uid);
            if (liveshow != null) {
                cur = System.currentTimeMillis();
                liveShowService.updateLocationToLs(header, uid, liveshow.getLid(), longitude, latitude, city, ip);
                logger.info("[reportLocation-2] cost:" + (System.currentTimeMillis() - cur)
                        + " ms uid:{} lid:{} longitude:{} latitude:{} city:{} ip:{}", uid, liveshow.getLid(), longitude, latitude, city, ip);
            }
            if (firstTime) {// 要设置用户的居住地信息
                if (StringUtils.isNotBlank(provice) && StringUtils.isNotBlank(city)) {
                    cur = System.currentTimeMillis();
                    userHessianService.getClient().updatePlace(uid, provice, city);
                    logger.info("[reportLocation-3] cost:" + (System.currentTimeMillis() - cur) + " ms");
                } else {
                    // 使用成彦的位置信息服务
                    List<Double> longlatis = Lists.newArrayList();
                    longlatis.add(longitude);
                    longlatis.add(latitude);
                    cur = System.currentTimeMillis();
                    List<String> posList = geoThriftService.getClient().findCityDetail(uid, longlatis, ip);
                    logger.info("[reportLocation-4] cost:" + (System.currentTimeMillis() - cur) + " ms");

                    if (posList != null && !posList.isEmpty()) {
                        String parseProvice = "";
                        String parseCity = "";
                        if (posList.size() >= 2) {
                            parseProvice = posList.get(1);
                            if (StringUtils.isNotEmpty(parseProvice) && parseProvice.endsWith("省")) {
                                parseProvice = StringUtils.substringBeforeLast(parseProvice, "省");
                            }
                        }
                        if (posList.size() >= 3) {
                            parseCity = posList.get(2);
                            if (StringUtils.isNotEmpty(parseCity) && parseCity.endsWith("市")) {
                                parseCity = StringUtils.substringBeforeLast(parseCity, "市");
                            }
                        }
                        cur = System.currentTimeMillis();
                        userHessianService.getClient().updatePlace(uid, parseProvice, parseCity);
                        logger.info("[reportLocation-5] cost:" + (System.currentTimeMillis() - cur) + " ms");
                    }
                }
            }
            pushUpdateMesage(uid, lid, longitude, latitude);
            logger.info("[reportLocation-finally] cost:" + (System.currentTimeMillis() - t) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS, null, node));
        } catch (Exception e) {
            logger.error("Report location error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        } finally {
            metricsClient
                    .report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "reportLocation", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void pushUpdateMesage(final Long uid, final String lid, final Double longi, final Double lati) {
        if (StringUtils.isNotBlank(lid)) {
            List<String> lids = Lists.newArrayList(lid);
            final List<LiveShowDto> liveshows = GeneralLiveShowClient.findLsByLids(lids);
            if (liveshows != null && liveshows.size() >= 1)
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LiveShowDto watchingLs = liveshows.get(0);
                        Double lsLongitude = watchingLs.getLocationY();
                        Double lsLatitude = watchingLs.getLocationX();
                        if (longi != null && lati != null && lsLongitude != null && lsLatitude != null) {
                            Double distance = DistIpUtil.calDistance(longi, lati, lsLongitude, lsLatitude);
                            // distance = distance * 1000;
                            String disStr = DistIpUtil.getDisStr(distance);
                            // if (distance > 200d) {// 大于两百公里
                            // disStr = watchingLs.getLocationCityName();
                            // }
                            logger.info("push to user:{} update distance:{} in liveshow room:{}", uid, disStr, lid);
                            messageService.pushToUpdateDistance(lid, uid, disStr);
                        }
                    }
                }).start();
        }
    }

    public void getAccessList(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        long cur = t;
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            cur = System.currentTimeMillis();
            Map<Long, Long> accessRecord = checkUserInfoMongoDBMapper.getCheckedUserList(uid);
            logger.info("[getAccessList-1] cost:" + (System.currentTimeMillis() - cur) + " ms");
            List<Long> uids = Lists.newArrayList();
            for (Entry<Long, Long> entry : accessRecord.entrySet()) {
                uids.add(entry.getKey());
            }
            cur = System.currentTimeMillis();
            List<UserInfo> retUsers = userHessianService.getClient().findUserListByUids(uids, true);
            logger.info("[getAccessList-2] cost:" + (System.currentTimeMillis() - cur) + " ms");
            List<ObjectNode> retList = Lists.newArrayList();
            for (UserInfo userInfo : retUsers) {
                ObjectNode jnode = getLocalObjMapper().createObjectNode();
                jnode.put("uid", userInfo.getUid());
                jnode.put("sex", userInfo.getSex());
                jnode.put("nick", userInfo.getNick() == null ? "" : userInfo.getNick());
                jnode.put("signature", userInfo.getSignature() == null ? "" : userInfo.getSignature());
                jnode.put("headerUrl", userInfo.getHeaderUrl());
                if (userInfo.getTopMedal() != null) {
                    jnode.put("topMedal", userInfo.getTopMedal());
                }
                jnode.put("checkTime", accessRecord.get(userInfo.getUid()));
                retList.add(jnode);
            }
            logger.info("[getAccessList-finally] cost:" + (System.currentTimeMillis() - t) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS, null, retList));
        } catch (Exception e) {
            logger.error("Get access list error.", e);
            rescode = MetricsClient.RESCODE_FAIL;
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getAccessList", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    /**
     * 按关注时间倒序获取关注列表
     * 
     * @param fid
     * @param lastRid
     * @param limit
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getIdols(long uid, long fid, String lastRid, int limit, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        long cur = t;
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List[] objIdTids = getFriendClient().getIdolIds(fid, lastRid, limit);
            logger.info("[getIdols-1] cost:" + (System.currentTimeMillis() - t) + " ms");
            if (objIdTids == null) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            cur = System.currentTimeMillis();
            List<ObjectNode> objectNodes = fillService.fillIdolsInfo(uid, objIdTids[1], objIdTids[0]);
            logger.info("[getIdols-2] cost:" + (System.currentTimeMillis() - cur) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS, null, objectNodes));
        } catch (Exception e) {
            logger.error("Get idols error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getIdols", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    /**
     * 按关注时间倒序获取粉丝列表
     * 
     * @param uid
     * @param tid
     * @param lastRid
     * @param limit
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getFans(long uid, long tid, String lastRid, int limit, HttpServletRequest request, HttpServletResponse response) {

        long t = MaskClock.getCurtime();
        long cur = t;
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List[] objIdFids = getFriendClient().getFanIds(tid, lastRid, limit);
            logger.info("[getFans-1] cost:" + (System.currentTimeMillis() - t) + " ms");
            if (objIdFids == null) {
                sendResponse(request, response, genMsgObj(SUCCESS));
                return;
            }
            cur = System.currentTimeMillis();
            List<ObjectNode> objectNodes = fillService.fillIdolsInfo(uid, objIdFids[1], objIdFids[0]);// 有一次rpc调用
            logger.info("[getFans-2] cost:" + (System.currentTimeMillis() - cur) + " ms");
            sendResponse(request, response, genMsgObj(SUCCESS, null, objectNodes));
        } catch (Exception e) {
            logger.error("Get fans error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getFans", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void getMulti(long uid, long otherUid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            List<MultiInfo> retList = userHessianService.getClient().getUserMultiInfo(otherUid);
            sendResponse(request, response, genMsgObj(SUCCESS, null, BaseServletUtil.getLocalObjMapper().valueToTree(retList)));
        } catch (Exception e) {
            logger.error("getMulti error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
            return;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getMulti", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void getInterestAndMeetInfo(long uid, long otherUid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            Map<String, Object> retMap = Maps.newHashMap();
            List<InterestNode> retInterList = userHessianService.getClient().getUserInterest(otherUid);
            if (retInterList != null && !retInterList.isEmpty()) {
                retMap.put(FIELD_U_INTERESTS, BaseServletUtil.getLocalObjMapper().valueToTree(retInterList));
            }
            if (uid != otherUid) {// 查看的是别人的主页
                List<String> commonLabelList = userHessianService.getClient().getCommonInterestLabel(uid, otherUid);
                retMap.put(FIELD_U_COMMONLABEL, commonLabelList);// 共同的标签
                MeetInfo meetInfo = userHessianService.getClient().getMeetInfo(uid, otherUid);
                if (meetInfo != null) {
                    List<Double> longlatis = Lists.newArrayList();
                    longlatis.add(meetInfo.getLongtitude());
                    longlatis.add(meetInfo.getLatitude());
                    // geoThriftService.getClient().findCityName(appId, otherUid, location, ip);
                }
                retMap.put(FIELD_U_MEETINFO, meetInfo);// 相遇的信息
            }
            sendResponse(request, response, genMsgObj(SUCCESS, null, retMap));
        } catch (Exception e) {
            logger.error("getInterestAndMeetInfo error.", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
            return;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "getInterestAndMeetInfo", 1, MaskClock.getCurtime() - t,
                    rescode);
        }
    }

    /**
     * 第三方电话号码绑定判定
     * 
     * @param uid
     * @param request
     * @param response
     */
    public void isBind(long uid, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码
        try {
            ContactCode contactCode = contactHessianService.getClient().selectUidFromBind(uid);
            if (ContactCode.HAS_BOUND_BY_MOBILE == contactCode) {
                sendResponse(request, response, genMsgObj(HAS_BOUND_BY_MOBILE));
            } else {
                sendResponse(request, response, genMsgObj(SUCCESS));
            }
            return;
        } catch (Exception e) {
            logger.error("there are some error the exception is ", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "isBind", 1, MaskClock.getCurtime() - t, rescode);
        }

    }

    /**
     * 第三方绑定电话号码
     * 
     * @param uid
     * @param mobile
     * @param openSource
     * @param request
     * @param response
     */
    public void bindUid(long uid, String mobile, Integer openSource, HttpServletRequest request, HttpServletResponse response) {
        long t = MaskClock.getCurtime();
        int rescode = MetricsClient.RESCODE_SUCCESS;// 上报鹰眼结果码

        try {
            // + 86
            mobile = add86FrontMobile(mobile);
            if (mobile == null) {
                logger.info("the code is invalid");
                sendResponse(request, response, genMsgObj(FAILED, "mobile is error"));
                return;
            }
            // 判定该电话号码是否被绑定或是否绑定超过三个
            RetMsgObj retMsgObj = judgeMentMobileAndUid(mobile, openSource);
            if (retMsgObj.getCode() != SUCCESS) {
                rescode = MetricsClient.RESCODE_FAIL;
                sendResponse(request, response, retMsgObj);
                return;
            }
            // 绑定手机
            ContactCode contactCode = contactHessianService.getClient().bindMobileAndUid(uid, mobile, openSource);
            sendResponse(request, response, genMsgObj(SUCCESS));
            return;
        } catch (Exception e) {
            logger.error("there are some error the exception is ", e);
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            rescode = MetricsClient.RESCODE_FAIL;
            return;
        } finally {
            metricsClient.report(MetricsClient.ProtocolType.HTTP, progress, this.getClass(), "bindUid", 1, MaskClock.getCurtime() - t, rescode);
        }
    }

    public void unBindMobile(long uid, HttpServletRequest request, HttpServletResponse response) {
        try {
            contactHessianService.getClient().unBindMobile(uid);
            sendResponse(request, response, genMsgObj(SUCCESS));
        } catch (Exception e) {
            sendResponse(request, response, genMsgObj(FAILED, e.getMessage()));
            return;
        }
    }

    // +86
    private String add86FrontMobile(String mobile) {
        if (mobile.length() == 11) {
            if (mobile.startsWith("1")) {
                mobile = "86" + mobile;
                return mobile;
            } else {
                return null;
            }
        } else if (mobile.length() == 13) {
            if (!mobile.startsWith("86")) {
                return null;
            }
            return mobile;
        } else {
            return null;
        }
    }

    /**
     * 判定该电话号码是否被绑定或是否绑定超过三个
     * 
     * @param mobile
     * @param openSourceInt
     * @return
     */
    public RetMsgObj judgeMentMobileAndUid(String mobile, Integer openSourceInt) {
        Integer bindCountInteger = Integer.valueOf(cntConfService.findBindCount());
        ContactCode contactCode = contactHessianService.getClient().judgeBindMobile(mobile, openSourceInt);
        if (ContactCode.HAS_BOUND_BY_UID == contactCode) {
            logger.warn("the mobile has bound by this user");
            return genMsgObj(BaseServletUtil.HAS_BOUND_BY_UID, "the mobile has bound by this user");
        }

        if (ContactCode.BOUND_OVER_BINDCOUNT == contactCode) {// 第三方登录的前提下判断是否已经被绑定三次及以上
            logger.warn("the user has bound is over {}", bindCountInteger);
            return genMsgObj(BaseServletUtil.BOUND_OVER_BINDCOUNT, "the user has bound is over " + bindCountInteger);
        }
        return genMsgObj(SUCCESS);
    }

}
